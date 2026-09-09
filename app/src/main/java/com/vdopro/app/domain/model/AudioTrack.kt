package com.vdopro.app.domain.model

import java.util.UUID

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val title: String,
    val startTimeOnTimelineMs: Long = 0L,
    val sourceDurationMs: Long,
    val startInSourceMs: Long = 0L,
    val endInSourceMs: Long = sourceDurationMs,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
) {
    val durationMs: Long
        get() = (endInSourceMs - startInSourceMs).coerceAtLeast(0L)
}
