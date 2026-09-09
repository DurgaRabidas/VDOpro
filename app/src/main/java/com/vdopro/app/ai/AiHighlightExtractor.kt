package com.vdopro.app.ai

import com.vdopro.app.data.media.AudioSampleChunk
import com.vdopro.app.domain.model.VideoClip
import java.util.UUID

data class HighlightSegment(
    val startMs: Long,
    val endMs: Long,
    val score: Float
)

class AiHighlightExtractor {

    fun extractBestHighlights(
        audioChunks: List<AudioSampleChunk>,
        visualCutPoints: List<SceneCutPoint>,
        targetDurationMs: Long = 15000L,
        minSegmentDurationMs: Long = 2000L
    ): List<HighlightSegment> {
        if (audioChunks.isEmpty()) return emptyList()

        val totalDurationMs = audioChunks.lastOrNull()?.let { it.timestampMs + it.durationMs } ?: 0L
        if (totalDurationMs <= 0) return emptyList()

        val windowSizeMs = 1000L
        val scores = mutableListOf<Pair<Long, Float>>()

        var currentWindowStart = 0L
        while (currentWindowStart < totalDurationMs) {
            val windowEnd = currentWindowStart + windowSizeMs

            val windowAudio = audioChunks.filter { it.timestampMs in currentWindowStart until windowEnd }
            val avgAudioEnergy = if (windowAudio.isNotEmpty()) windowAudio.map { it.maxAmplitude }.average().toFloat() else 0f

            val visualCuts = visualCutPoints.filter { it.timestampMs in currentWindowStart until windowEnd }
            val visualScore = visualCuts.sumOf { it.differenceScore.toDouble() }.toFloat()

            val combinedScore = (avgAudioEnergy * 0.6f) + (visualScore * 0.4f)
            scores.add(Pair(currentWindowStart, combinedScore))

            currentWindowStart += windowSizeMs
        }

        val topScoringWindows = scores.sortedByDescending { it.second }
        val selectedHighlights = mutableListOf<HighlightSegment>()
        var accumulatedMs = 0L

        for (window in topScoringWindows) {
            if (accumulatedMs >= targetDurationMs) break

            val segStart = window.first
            val segEnd = (segStart + minSegmentDurationMs).coerceAtMost(totalDurationMs)

            val overlaps = selectedHighlights.any { existing ->
                (segStart < existing.endMs && segEnd > existing.startMs)
            }

            if (!overlaps) {
                selectedHighlights.add(HighlightSegment(startMs = segStart, endMs = segEnd, score = window.second))
                accumulatedMs += (segEnd - segStart)
            }
        }

        return selectedHighlights.sortedBy { it.startMs }
    }

    fun createHighlightReel(
        originalClip: VideoClip,
        highlightSegments: List<HighlightSegment>
    ): List<VideoClip> {
        return highlightSegments.mapIndexed { index, seg ->
            originalClip.copy(
                id = UUID.randomUUID().toString(),
                name = "${originalClip.name} (Highlight #${index + 1})",
                startInSourceMs = seg.startMs,
                endInSourceMs = seg.endMs
            )
        }
    }
}
