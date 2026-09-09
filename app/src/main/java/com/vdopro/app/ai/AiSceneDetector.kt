package com.vdopro.app.ai

import android.graphics.Bitmap
import com.vdopro.app.domain.model.VideoClip
import java.util.UUID
import kotlin.math.abs

data class SceneCutPoint(
    val timestampMs: Long,
    val differenceScore: Float
)

interface FrameData {
    val width: Int
    val height: Int
    fun getPixelRGB(x: Int, y: Int): IntArray
}

class BitmapFrameData(private val bitmap: Bitmap) : FrameData {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun getPixelRGB(x: Int, y: Int): IntArray {
        val pixel = bitmap.getPixel(x, y)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return intArrayOf(r, g, b)
    }
}

class RawFrameData(
    override val width: Int,
    override val height: Int,
    private val pixelsRgb: IntArray
) : FrameData {
    override fun getPixelRGB(x: Int, y: Int): IntArray {
        val idx = y * width + x
        val pixel = pixelsRgb[idx]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return intArrayOf(r, g, b)
    }
}

class AiSceneDetector {

    fun calculateFrameDifference(frame1: FrameData, frame2: FrameData): Float {
        val width = minOf(frame1.width, frame2.width)
        val height = minOf(frame1.height, frame2.height)

        if (width <= 0 || height <= 0) return 0f

        var diffSum = 0L
        var samplesCount = 0

        for (y in 0 until height step maxOf(1, height / 20)) {
            for (x in 0 until width step maxOf(1, width / 20)) {
                val rgb1 = frame1.getPixelRGB(x, y)
                val rgb2 = frame2.getPixelRGB(x, y)

                val diff = abs(rgb1[0] - rgb2[0]) + abs(rgb1[1] - rgb2[1]) + abs(rgb1[2] - rgb2[2])
                diffSum += diff
                samplesCount++
            }
        }

        if (samplesCount == 0) return 0f
        val avgDiffPerPixel = diffSum.toFloat() / (samplesCount * 3 * 255f)
        return avgDiffPerPixel.coerceIn(0f, 1f)
    }

    fun detectSceneCuts(
        frames: List<Pair<Long, FrameData>>,
        threshold: Float = 0.25f,
        minSceneLengthMs: Long = 1000L
    ): List<SceneCutPoint> {
        if (frames.size < 2) return emptyList()

        val cutPoints = mutableListOf<SceneCutPoint>()
        var lastCutTimeMs = 0L

        for (i in 0 until frames.size - 1) {
            val (_, frame1) = frames[i]
            val (time2, frame2) = frames[i + 1]

            val diffScore = calculateFrameDifference(frame1, frame2)

            if (diffScore >= threshold && (time2 - lastCutTimeMs) >= minSceneLengthMs) {
                cutPoints.add(SceneCutPoint(timestampMs = time2, differenceScore = diffScore))
                lastCutTimeMs = time2
            }
        }

        return cutPoints
    }

    fun splitClipByScenes(
        clip: VideoClip,
        cutPoints: List<SceneCutPoint>
    ): List<VideoClip> {
        if (cutPoints.isEmpty()) return listOf(clip)

        val resultClips = mutableListOf<VideoClip>()
        var currentStart = clip.startInSourceMs

        val sortedCuts = cutPoints.map { it.timestampMs }.filter { it in (clip.startInSourceMs + 500L) until (clip.endInSourceMs - 500L) }.sorted()

        for (cutMs in sortedCuts) {
            val subClip = clip.copy(
                id = UUID.randomUUID().toString(),
                name = "${clip.name} (Scene ${resultClips.size + 1})",
                startInSourceMs = currentStart,
                endInSourceMs = cutMs
            )
            resultClips.add(subClip)
            currentStart = cutMs
        }

        if (currentStart < clip.endInSourceMs) {
            val finalClip = clip.copy(
                id = UUID.randomUUID().toString(),
                name = "${clip.name} (Scene ${resultClips.size + 1})",
                startInSourceMs = currentStart,
                endInSourceMs = clip.endInSourceMs
            )
            resultClips.add(finalClip)
        }

        return resultClips
    }
}
