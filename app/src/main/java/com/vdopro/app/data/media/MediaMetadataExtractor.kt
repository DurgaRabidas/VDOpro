package com.vdopro.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VideoFileInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val frameRate: Float
)

class MediaMetadataExtractor(private val context: Context) {

    suspend fun extractVideoInfo(filePath: String): VideoFileInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePath))
            } else {
                retriever.setDataSource(filePath)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val frameRateStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            } else null

            val duration = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 1920
            val height = heightStr?.toIntOrNull() ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0
            val frameRate = frameRateStr?.toFloatOrNull() ?: 30f

            VideoFileInfo(
                durationMs = duration,
                width = width,
                height = height,
                rotation = rotation,
                frameRate = frameRate
            )
        } catch (e: Exception) {
            VideoFileInfo(
                durationMs = 0L,
                width = 1920,
                height = 1080,
                rotation = 0,
                frameRate = 30f
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun extractFrameAt(filePath: String, timeUs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePath))
            } else {
                retriever.setDataSource(filePath)
            }
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun extractThumbnails(
        filePath: String,
        durationMs: Long,
        count: Int = 10
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (durationMs <= 0 || count <= 0) return@withContext emptyList()
        val retriever = MediaMetadataRetriever()
        val thumbnails = mutableListOf<Bitmap>()
        try {
            if (filePath.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(filePath))
            } else {
                retriever.setDataSource(filePath)
            }

            val intervalUs = (durationMs * 1000L) / count
            for (i in 0 until count) {
                val timeUs = i * intervalUs
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    thumbnails.add(frame)
                }
            }
        } catch (e: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
        thumbnails
    }
}
