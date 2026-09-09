package com.vdopro.app.ai

import com.vdopro.app.data.media.AudioSampleChunk
import com.vdopro.app.domain.model.VideoClip
import java.util.UUID

data class AudioTimeInterval(
    val startMs: Long,
    val endMs: Long
)

class AiSilenceRemover {

    fun detectNonSilentIntervals(
        chunks: List<AudioSampleChunk>,
        silenceThreshold: Float = 0.05f,
        minNonSilenceDurationMs: Long = 300L,
        paddingMs: Long = 100L
    ): List<AudioTimeInterval> {
        if (chunks.isEmpty()) return emptyList()

        val activeChunks = chunks.map { chunk ->
            chunk.copy(isSilence = chunk.maxAmplitude < silenceThreshold)
        }

        val rawIntervals = mutableListOf<AudioTimeInterval>()
        var currentStart: Long? = null
        var lastEnd = 0L

        for (chunk in activeChunks) {
            val chunkStart = chunk.timestampMs
            val chunkEnd = chunkStart + chunk.durationMs
            lastEnd = maxOf(lastEnd, chunkEnd)

            if (!chunk.isSilence) {
                if (currentStart == null) {
                    currentStart = chunkStart
                }
            } else {
                if (currentStart != null) {
                    rawIntervals.add(AudioTimeInterval(currentStart, chunkStart))
                    currentStart = null
                }
            }
        }

        if (currentStart != null) {
            rawIntervals.add(AudioTimeInterval(currentStart, lastEnd))
        }

        val paddedIntervals = rawIntervals.map { interval ->
            val paddedStart = (interval.startMs - paddingMs).coerceAtLeast(0L)
            val paddedEnd = (interval.endMs + paddingMs).coerceAtMost(lastEnd)
            AudioTimeInterval(paddedStart, paddedEnd)
        }

        if (paddedIntervals.isEmpty()) return emptyList()

        val mergedIntervals = mutableListOf<AudioTimeInterval>()
        var current = paddedIntervals[0]

        for (i in 1 until paddedIntervals.size) {
            val next = paddedIntervals[i]
            if (next.startMs <= current.endMs) {
                current = AudioTimeInterval(current.startMs, maxOf(current.endMs, next.endMs))
            } else {
                if (current.endMs - current.startMs >= minNonSilenceDurationMs) {
                    mergedIntervals.add(current)
                }
                current = next
            }
        }

        if (current.endMs - current.startMs >= minNonSilenceDurationMs) {
            mergedIntervals.add(current)
        }

        return mergedIntervals
    }

    fun applySilenceRemovalToClip(
        clip: VideoClip,
        nonSilentIntervals: List<AudioTimeInterval>
    ): List<VideoClip> {
        if (nonSilentIntervals.isEmpty()) return listOf(clip)

        return nonSilentIntervals.mapNotNull { interval ->
            val startInSource = (clip.startInSourceMs + interval.startMs).coerceIn(clip.startInSourceMs, clip.endInSourceMs)
            val endInSource = (clip.startInSourceMs + interval.endMs).coerceIn(clip.startInSourceMs, clip.endInSourceMs)

            if (endInSource - startInSource > 100L) {
                clip.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${clip.name} (AI Cut)",
                    startInSourceMs = startInSource,
                    endInSourceMs = endInSource
                )
            } else {
                null
            }
        }
    }
}
