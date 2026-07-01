package com.llamadroid.domain.server

import com.llamadroid.domain.inference.InferenceEngine
import com.llamadroid.domain.inference.SamplingParams
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.routing.route
import io.ktor.http.ContentType
import io.ktor.server.response.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class ServerMetrics(
    val requestsServed: Int = 0,
    val tokensGenerated: Int = 0,
    val activeConnections: Int = 0,
    val uptimeSeconds: Long = 0,
    val startTime: Long = 0
)

@Singleton
class OpenAiApi @Inject constructor(
    private val inferenceEngine: InferenceEngine
) {
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }
    private var server: ApplicationEngine? = null

    private val _metrics = MutableStateFlow(ServerMetrics())
    val metrics: StateFlow<ServerMetrics> = _metrics.asStateFlow()

    private val requestCount = AtomicInteger(0)

    fun isRunning(): Boolean = server != null

    fun start(config: ServerConfig) {
        if (server != null) return
        _metrics.value = _metrics.value.copy(startTime = System.currentTimeMillis())

        server = embeddedServer(Netty, port = config.port, host = config.host) {
            configureRoutes(config)
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun Application.configureRoutes(config: ServerConfig) {
        routing {
            get("/health") {
                call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
            }

            get("/v1/models") {
                // ponytail: return placeholder model list, enrich when model manager exists
                val body = """{"object":"list","data":[{"id":"llamadroid","object":"model","owned_by":"user"}]}"""
                call.respondText(body, ContentType.Application.Json)
            }

            post("/v1/chat/completions") {
                val raw = call.receiveText()
                val req = json.decodeFromString<ChatCompletionRequest>(raw)
                requestCount.incrementAndGet()
                _metrics.value = _metrics.value.copy(requestsServed = requestCount.get())

                if (req.stream == true) {
                    // ponytail: streaming not yet implemented in native bridge, return single response
                    call.respondText("""{"error":"streaming not available"}""", ContentType.Application.Json, HttpStatusCode.NotImplemented)
                } else {
                    val prompt = req.messages.joinToString("\n") { "${it.role}: ${it.content}" }
                    val params = SamplingParams(
                        temperature = req.temperature ?: 0.7f,
                        topP = req.topP ?: 0.9f,
                        maxTokens = req.maxTokens ?: 2048
                    )
                    try {
                        // inference is stubbed until native lib is linked
                        val result = """{"id":"chatcmpl-${requestCount.get()}","object":"chat.completion","choices":[{"index":0,"message":{"role":"assistant","content":"Server active. Load a model to enable inference."},"finish_reason":"stop"}],"usage":{"prompt_tokens":0,"completion_tokens":0,"total_tokens":0}}"""
                        call.respondText(result, ContentType.Application.Json)
                    } catch (e: Exception) {
                        call.respondText("""{"error":"${e.message}"}""", ContentType.Application.Json, HttpStatusCode.InternalServerError)
                    }
                }
            }

            post("/v1/completions") {
                val raw = call.receiveText()
                val req = json.decodeFromString<CompletionRequest>(raw)
                val result = """{"id":"cmpl-${requestCount.incrementAndGet()}","object":"text_completion","choices":[{"index":0,"text":"Server active. Load a model to enable inference.","finish_reason":"stop"}]}"""
                call.respondText(result, ContentType.Application.Json)
            }

            post("/v1/embeddings") {
                val raw = call.receiveText()
                val req = json.decodeFromString<EmbeddingRequest>(raw)
                val dims = 384
                val vec = "[" + (1..dims).joinToString(",") { "0.0" } + "]"
                val result = """{"object":"list","data":[{"object":"embedding","index":0,"embedding":$vec}]}"""
                call.respondText(result, ContentType.Application.Json)
            }
        }
    }

    companion object {
        fun localIpAddress(): String = try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address) return addr.hostAddress ?: "127.0.0.1"
                }
            }
            "127.0.0.1"
        } catch (_: Exception) { "127.0.0.1" }
    }
}

@Serializable
data class ChatCompletionRequest(
    val model: String = "default",
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val stream: Boolean? = null
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class CompletionRequest(
    val model: String = "default",
    val prompt: String = "",
    val maxTokens: Int? = null
)

@Serializable
data class EmbeddingRequest(
    val model: String = "default",
    val input: String = "",
    val inputList: List<String>? = null
)
