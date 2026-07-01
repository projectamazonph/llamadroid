package com.llamadroid.domain.models

import com.llamadroid.data.models.DownloadedModel
import com.llamadroid.data.repository.ModelDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val modelId: String,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val percent: Float get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}

@Singleton
class DownloadManager @Inject constructor(
    private val modelDao: ModelDao
) {
    private var activeDownload: DownloadProgress? = null

    fun getProgress() = activeDownload

    suspend fun download(
        hfModelId: String,
        ggufFile: String,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val dirName = hfModelId.replace('/', '_')
        val modelsDir = File(DOWNLOAD_DIR, dirName)
        modelsDir.mkdirs()
        val target = File(modelsDir, ggufFile)

        try {
            val url = URL("https://huggingface.co/$hfModelId/resolve/main/$ggufFile")

            // Check for partial download to resume
            val startBytes = if (target.exists()) target.length() else 0L

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 30000
            conn.readTimeout = 120000
            if (startBytes > 0) conn.setRequestProperty("Range", "bytes=$startBytes-")

            val totalBytes = conn.contentLengthLong
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(target, startBytes > 0)

            val buffer = ByteArray(8192)
            var downloaded = startBytes
            var lastNotify = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastNotify > 81920) {
                            lastNotify = downloaded
                            activeDownload = DownloadProgress(
                                modelId = hfModelId,
                                bytesDownloaded = downloaded,
                                totalBytes = if (totalBytes > 0) totalBytes else downloaded,
                                isRunning = true
                            )
                            activeDownload?.let { onProgress(it) }
                        }
                    }
                }
            }

            val quant = extractQuant(ggufFile)
            val params = extractParams(ggufFile)
            modelDao.upsert(DownloadedModel(
                id = hfModelId,
                localPath = target.absolutePath,
                filename = ggufFile,
                quantization = quant,
                paramSize = params,
                fileSize = target.length()
            ))

            activeDownload = DownloadProgress(
                modelId = hfModelId,
                bytesDownloaded = target.length(),
                totalBytes = target.length(),
                isComplete = true
            )
            activeDownload?.let { onProgress(it) }
            true
        } catch (e: Exception) {
            activeDownload = DownloadProgress(modelId = hfModelId, error = e.message)
            activeDownload?.let { onProgress(it) }
            false
        }
    }

    companion object {
        val DOWNLOAD_DIR: String
            get() = "/storage/emulated/0/Android/data/com.llamadroid/models"

        private fun extractQuant(filename: String): String =
            Regex("Q\\d+_K_\\w|Q\\d+_\\w|Q\\d+|q\\d+_\\w|IQ\\d+_\\w")
                .find(filename)?.value?.uppercase() ?: "unknown"

        private fun extractParams(filename: String): String =
            Regex("\\d+\\.?\\d*\\s*[Bb]").find(filename)?.value?.uppercase() ?: ""
    }
}
