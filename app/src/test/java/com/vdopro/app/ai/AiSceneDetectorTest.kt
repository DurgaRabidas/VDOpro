package com.vdopro.app.ai

import com.vdopro.app.domain.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Test

class AiSceneDetectorTest {

    @Test
    fun testDetectSceneCutsWithRawFrameData() {
        val detector = AiSceneDetector()

        val redPixels = IntArray(100 * 100) { 0xFFFF0000.toInt() }
        val bluePixels = IntArray(100 * 100) { 0xFF0000FF.toInt() }

        val frame1 = RawFrameData(100, 100, redPixels)
        val frame2 = RawFrameData(100, 100, redPixels)
        val frame3 = RawFrameData(100, 100, bluePixels)

        val frames = listOf(
            Pair(0L, frame1),
            Pair(1000L, frame2),
            Pair(2000L, frame3)
        )

        val cuts = detector.detectSceneCuts(frames, threshold = 0.2f, minSceneLengthMs = 500L)

        assertEquals(1, cuts.size)
        assertEquals(2000L, cuts[0].timestampMs)
    }

    @Test
    fun testSplitClipByScenes() {
        val detector = AiSceneDetector()
        val originalClip = VideoClip(id = "c1", filePath = "path", name = "Vlog", sourceDurationMs = 10000L)

        val cuts = listOf(
            SceneCutPoint(timestampMs = 3000L, differenceScore = 0.8f),
            SceneCutPoint(timestampMs = 7000L, differenceScore = 0.9f)
        )

        val clips = detector.splitClipByScenes(originalClip, cuts)

        assertEquals(3, clips.size)
        assertEquals(0L, clips[0].startInSourceMs)
        assertEquals(3000L, clips[0].endInSourceMs)

        assertEquals(3000L, clips[1].startInSourceMs)
        assertEquals(7000L, clips[1].endInSourceMs)

        assertEquals(7000L, clips[2].startInSourceMs)
        assertEquals(10000L, clips[2].endInSourceMs)
    }
}
