package com.vdopro.app.domain.model

import java.util.UUID

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val name: String,
    val sourceDurationMs: Long,
    val startInSourceMs: Long = 0L,
    val endInSourceMs: Long = sourceDurationMs,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val activeFilter: FilterType = FilterType.NONE,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val rotationDegrees: Int = 0
) {
    val trimmedDurationMs: Long
        get() = ((endInSourceMs - startInSourceMs) / speed).toLong().coerceAtLeast(0L)
}
