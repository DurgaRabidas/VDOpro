package com.vdopro.app.ai

import com.vdopro.app.data.media.AudioSampleChunk
import com.vdopro.app.domain.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Test

class AiSilenceRemoverTest {

    @Test
    fun testDetectNonSilentIntervals() {
        val remover = AiSilenceRemover()

        val chunks = mutableListOf<AudioSampleChunk>()
        for (time in 0L until 1000L step 100L) {
            chunks.add(AudioSampleChunk(timestampMs = time, durationMs = 100L, maxAmplitude = 0.5f, isSilence = false))
        }
        for (time in 1000L until 3000L step 100L) {
            chunks.add(AudioSampleChunk(timestampMs = time, durationMs = 100L, maxAmplitude = 0.01f, isSilence = true))
        }
        for (time in 3000L until 5000L step 100L) {
            chunks.add(AudioSampleChunk(timestampMs = time, durationMs = 100L, maxAmplitude = 0.6f, isSilence = false))
        }

        val intervals = remover.detectNonSilentIntervals(chunks, paddingMs = 50L)

        assertEquals(2, intervals.size)
        assertEquals(0L, intervals[0].startMs)
        assertEquals(1050L, intervals[0].endMs)

        assertEquals(2950L, intervals[1].startMs)
        assertEquals(5000L, intervals[1].endMs)
    }

    @Test
    fun testApplySilenceRemovalToClip() {
        val remover = AiSilenceRemover()
        val originalClip = VideoClip(id = "c1", filePath = "path", name = "Interview", sourceDurationMs = 10000L)

        val intervals = listOf(
            AudioTimeInterval(startMs = 500L, endMs = 3000L),
            AudioTimeInterval(startMs = 6000L, endMs = 9000L)
        )

        val resultClips = remover.applySilenceRemovalToClip(originalClip, intervals)

        assertEquals(2, resultClips.size)
        assertEquals(500L, resultClips[0].startInSourceMs)
        assertEquals(3000L, resultClips[0].endInSourceMs)

        assertEquals(6000L, resultClips[1].startInSourceMs)
        assertEquals(9000L, resultClips[1].endInSourceMs)
    }
}
