package com.lanlan3292.resolutionchanger

data class ResolutionOption(
    val id: String,
    val titleRes: Int,
    val width: Int,
    val height: Int,
    val density: Int,
    val isReset: Boolean = false
)
