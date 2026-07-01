package com.llamadroid.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_models")
data class DownloadedModel(
    @PrimaryKey val id: String,
    val localPath: String,
    val filename: String,
    val quantization: String = "",
    val paramSize: String = "",
    val fileSize: Long = 0,
    val downloadedAt: Long = System.currentTimeMillis()
)
