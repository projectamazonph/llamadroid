package com.llamadroid.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.llamadroid.domain.models.DownloadManager
import com.llamadroid.domain.models.DownloadProgress
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var downloadManager: DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() { super.onCreate(); NotificationHelper.createChannel(this, "Downloads") }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
        val ggufFile = intent.getStringExtra(EXTRA_GGUF_FILE) ?: return START_NOT_STICKY

        val notification = NotificationHelper.build(this, "Downloading model", modelId.substringAfterLast('/'), ongoing = true).build()
        startForeground(3, notification)

        scope.launch {
            downloadManager.download(hfModelId = modelId, ggufFile = ggufFile, onProgress = { progress ->
                val nm = NotificationManagerCompat.from(this@DownloadService)
                val title = if (progress.isComplete) "Download complete" else if (progress.error != null) "Download failed" else "Downloading"
                val text = if (progress.isComplete) modelId.substringAfterLast('/') else if (progress.error != null) progress.error else "${(progress.percent * 100).toInt()}%"
                nm.notify(3, NotificationHelper.build(this@DownloadService, title, text, ongoing = !progress.isComplete && progress.error == null)
                    .setProgress(100, (progress.percent * 100).toInt(), false).build())
            })
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        const val EXTRA_MODEL_ID = "model_id"
        const val EXTRA_GGUF_FILE = "gguf_file"
        fun intent(context: Context, modelId: String, ggufFile: String): Intent =
            Intent(context, DownloadService::class.java).apply { putExtra(EXTRA_MODEL_ID, modelId); putExtra(EXTRA_GGUF_FILE, ggufFile) }
    }
}
