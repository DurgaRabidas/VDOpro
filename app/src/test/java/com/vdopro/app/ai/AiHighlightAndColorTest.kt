package com.vdopro.app.ai

import com.vdopro.app.data.media.AudioSampleChunk
import com.vdopro.app.domain.model.FilterType
import com.vdopro.app.domain.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiHighlightAndColorTest {

    @Test
    fun testExtractBestHighlights() {
        val extractor = AiHighlightExtractor()

        val audioChunks = (0L until 20000L step 1000L).map { time ->
            val amp = if (time in 10000L..14000L) 0.9f else 0.2f
            AudioSampleChunk(timestampMs = time, durationMs = 1000L, maxAmplitude = amp, isSilence = false)
        }

        val cuts = listOf(
            SceneCutPoint(timestampMs = 11000L, differenceScore = 0.8f)
        )

        val highlights = extractor.extractBestHighlights(
            audioChunks = audioChunks,
            visualCutPoints = cuts,
            targetDurationMs = 5000L,
            minSegmentDurationMs = 3000L
        )

        assertTrue(highlights.isNotEmpty())
        assertEquals(11000L, highlights[0].startMs)
    }

    @Test
    fun testAiColorGradingEngine() {
        val engine = AiColorGradingEngine()
        val clip = VideoClip(id = "c1", filePath = "path", name = "Landscape", sourceDurationMs = 5000L)

        val enhanced = engine.applyAiColorGrading(clip, FilterType.CINEMATIC)

        assertEquals(FilterType.CINEMATIC, enhanced.activeFilter)
        assertEquals(-0.05f, enhanced.brightness, 0.001f)
        assertEquals(1.35f, enhanced.contrast, 0.001f)
        assertEquals(0.85f, enhanced.saturation, 0.001f)

        val autoEnhanced = engine.autoEnhanceClip(clip, averageLuminance = 0.25f)
        assertEquals(FilterType.AI_SMART_ENHANCE, autoEnhanced.activeFilter)
        assertTrue(autoEnhanced.brightness > 0f)
    }
}
