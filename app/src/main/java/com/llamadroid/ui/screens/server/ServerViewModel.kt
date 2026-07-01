package com.llamadroid.ui.screens.server

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.llamadroid.domain.server.OpenAiApi
import com.llamadroid.domain.server.ServerConfig
import com.llamadroid.domain.server.ServerMetrics
import com.llamadroid.service.ServerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerUiState(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val host: String = "127.0.0.1",
    val localIp: String = "127.0.0.1",
    val apiKey: String = "",
    val publicAccess: Boolean = false,
    val metrics: ServerMetrics = ServerMetrics()
)

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val api: OpenAiApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(localIp = OpenAiApi.localIpAddress())
        viewModelScope.launch {
            api.metrics.collect { metrics ->
                val running = api.isRunning()
                val uptime = if (running && metrics.startTime > 0)
                    (System.currentTimeMillis() - metrics.startTime) / 1000 else 0
                _state.value = _state.value.copy(
                    isRunning = running, metrics = metrics.copy(uptimeSeconds = uptime)
                )
            }
        }
    }

    fun toggle() {
        if (_state.value.isRunning) {
            stop()
        } else {
            start()
        }
    }

    fun setPort(port: Int) { _state.value = _state.value.copy(port = port) }
    fun setApiKey(key: String) { _state.value = _state.value.copy(apiKey = key) }
    fun setPublicAccess(enabled: Boolean) {
        _state.value = _state.value.copy(
            publicAccess = enabled,
            host = if (enabled) "0.0.0.0" else "127.0.0.1"
        )
    }

    private fun start() {
        val intent = ServerService.intent(context, _state.value.port)
        context.startForegroundService(intent)
    }

    private fun stop() {
        val intent = android.content.Intent(context, ServerService::class.java)
        context.stopService(intent)
        api.stop()
    }
}
