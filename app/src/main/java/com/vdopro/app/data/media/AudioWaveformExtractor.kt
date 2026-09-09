package com.vdopro.app.data.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class AudioSampleChunk(
    val timestampMs: Long,
    val durationMs: Long,
    val maxAmplitude: Float,
    val isSilence: Boolean
)

class AudioWaveformExtractor(private val context: Context) {

    suspend fun extractAudioAmplitudes(
        filePath: String,
        chunkDurationMs: Long = 100L,
        silenceThreshold: Float = 0.05f
    ): List<AudioSampleChunk> = withContext(Dispatchers.IO) {
        val sampleChunks = mutableListOf<AudioSampleChunk>()
        val extractor = MediaExtractor()

        try {
            if (filePath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(android.net.Uri.parse(filePath), "r")?.use { pfd ->
                    extractor.setDataSource(pfd.fileDescriptor)
                }
            } else {
                extractor.setDataSource(filePath)
            }

            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                return@withContext emptyList()
            }

            extractor.selectTrack(audioTrackIndex)
            val bufferSize = 8192
            val byteBuffer = java.nio.ByteBuffer.allocate(bufferSize)

            var currentChunkTimeMs = 0L
            var currentChunkMaxAmp = 0
            val chunkDurationUs = chunkDurationMs * 1000L
            var nextChunkEndTimeUs = chunkDurationUs

            while (true) {
                val sampleSize = extractor.readSampleData(byteBuffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                val sampleTimeMs = sampleTimeUs / 1000L

                byteBuffer.rewind()
                var maxSample = 0
                while (byteBuffer.remaining() >= 2) {
                    val sample = byteBuffer.short.toInt()
                    val absSample = abs(sample)
                    if (absSample > maxSample) {
                        maxSample = absSample
                    }
                }

                if (maxSample > currentChunkMaxAmp) {
                    currentChunkMaxAmp = maxSample
                }

                if (sampleTimeUs >= nextChunkEndTimeUs) {
                    val normalizedAmp = (currentChunkMaxAmp / 32767.0f).coerceIn(0.0f, 1.0f)
                    sampleChunks.add(
                        AudioSampleChunk(
                            timestampMs = currentChunkTimeMs,
                            durationMs = chunkDurationMs,
                            maxAmplitude = normalizedAmp,
                            isSilence = normalizedAmp < silenceThreshold
                        )
                    )

                    currentChunkTimeMs = sampleTimeMs
                    currentChunkMaxAmp = 0
                    nextChunkEndTimeUs += chunkDurationUs
                }

                extractor.advance()
            }

            if (currentChunkMaxAmp > 0 || sampleChunks.isEmpty()) {
                val normalizedAmp = (currentChunkMaxAmp / 32767.0f).coerceIn(0.0f, 1.0f)
                sampleChunks.add(
                    AudioSampleChunk(
                        timestampMs = currentChunkTimeMs,
                        durationMs = chunkDurationMs,
                        maxAmplitude = normalizedAmp,
                        isSilence = normalizedAmp < silenceThreshold
                    )
                )
            }

        } catch (e: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }

        sampleChunks
    }
}
