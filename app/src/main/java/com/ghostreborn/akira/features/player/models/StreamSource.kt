package com.ghostreborn.akira.features.player.models

data class StreamSource(
    val sourceName: String,
    val url: String,
    val priority: Double = 0.0,
    val isHls: Boolean = false,
    val type: String? = null,
    val fileExtension: String? = null,
    val isDirect: Boolean = true
)
