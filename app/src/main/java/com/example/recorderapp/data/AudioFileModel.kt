package com.example.recorderapp.data

import android.net.Uri

data class AudioFileModel(
    val id: Long,
    val name: String,
    val uri: Uri,
    val duration: Long = 0,
    val size: Long = 0
)
