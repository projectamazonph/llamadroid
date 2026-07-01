package com.llamadroid.domain.server

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val port: Int = 8080,
    val host: String = "127.0.0.1",
    val apiKey: String = ""
) {
    val isPublic: Boolean get() = host == "0.0.0.0"
}
