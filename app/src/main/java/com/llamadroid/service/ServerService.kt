package com.llamadroid.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.llamadroid.domain.server.OpenAiApi
import com.llamadroid.domain.server.ServerConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ServerService : Service() {

    @Inject lateinit var api: OpenAiApi

    override fun onCreate() { super.onCreate(); NotificationHelper.createChannel(this, "API Server") }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.build(this, "LlamaDroid Server", "API server running", ongoing = true).build()
        startForeground(2, notification)

        val config = ServerConfig(port = intent?.getIntExtra(EXTRA_PORT, 8080) ?: 8080)
        api.start(config)
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() { api.stop(); super.onDestroy() }

    companion object {
        const val EXTRA_PORT = "port"
        fun intent(context: android.content.Context, port: Int = 8080): Intent =
            Intent(context, ServerService::class.java).putExtra(EXTRA_PORT, port)
    }
}
