package com.vdopro.app.domain.model

import java.util.UUID

data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val videoClips: List<VideoClip> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val frameRate: Int = 30
) {
    val totalDurationMs: Long
        get() = videoClips.sumOf { it.trimmedDurationMs }

    fun getClipAtTimestamp(timelineMs: Long): Pair<VideoClip, Long>? {
        var accumulated = 0L
        for (clip in videoClips) {
            val clipDuration = clip.trimmedDurationMs
            if (timelineMs in accumulated until (accumulated + clipDuration)) {
                val offsetInClipTimeline = timelineMs - accumulated
                val offsetInSource = clip.startInSourceMs + (offsetInClipTimeline * clip.speed).toLong()
                return Pair(clip, offsetInSource)
            }
            accumulated += clipDuration
        }
        return videoClips.lastOrNull()?.let { Pair(it, it.endInSourceMs) }
    }
}
