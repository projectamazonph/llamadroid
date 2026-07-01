package com.llamadroid.domain.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedImage(
    val filePath: String,
    val thumbnailPath: String,
    val width: Int,
    val height: Int,
    val fileSize: Long
)

@Singleton
class ImageAttachment @Inject constructor() {

    companion object {
        private const val MAX_DIMENSION = 1024   // Max pixels on longest side
        private const val JPEG_QUALITY = 85
        private const val THUMB_MAX = 256
    }

    fun process(context: Context, uri: Uri): ProcessedImage? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap ?: return null

            val processed = resize(bitmap, MAX_DIMENSION)
            val thumb = resize(bitmap, THUMB_MAX)

            val dir = File(context.cacheDir, "images")
            dir.mkdirs()

            val fileId = UUID.randomUUID().toString()
            val fullFile = File(dir, "${fileId}.jpg")
            val thumbFile = File(dir, "${fileId}_thumb.jpg")

            FileOutputStream(fullFile).use { processed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            FileOutputStream(thumbFile).use { thumb.compress(Bitmap.CompressFormat.JPEG, 70, it) }

            ProcessedImage(
                filePath = fullFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath,
                width = processed.width,
                height = processed.height,
                fileSize = fullFile.length()
            )
        } catch (e: Exception) { null }
    }

    fun imageToBase64(path: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(path)
            val stream = java.io.ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    private fun resize(bitmap: Bitmap, maxDim: Int): Bitmap {
        val (w, h) = if (bitmap.width > bitmap.height) {
            Pair(maxDim, (bitmap.height * maxDim) / bitmap.width)
        } else {
            Pair((bitmap.width * maxDim) / bitmap.height, maxDim)
        }
        return Bitmap.createScaledBitmap(bitmap, w.coerceAtLeast(1), h.coerceAtLeast(1), true)
    }
}
