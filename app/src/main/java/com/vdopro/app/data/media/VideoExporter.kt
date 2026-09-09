package com.vdopro.app.data.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.vdopro.app.domain.model.VideoProject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

sealed class ExportState {
    data object Idle : ExportState()
    data class Progress(val percent: Float) : ExportState()
    data class Success(val outputPath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

class VideoExporter(private val context: Context) {

    fun exportProject(project: VideoProject, outputFileName: String): Flow<ExportState> = callbackFlow {
        val outputDir = File(context.getExternalFilesDir(null), "exports")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, "$outputFileName.mp4")
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val editedMediaItems = project.videoClips.map { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.filePath)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startInSourceMs)
                        .setEndPositionMs(clip.endInSourceMs)
                        .build()
                )
                .build()

            EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(clip.volume == 0f)
                .build()
        }

        if (editedMediaItems.isEmpty()) {
            trySend(ExportState.Error("No video clips in project timeline to export."))
            close()
            return@callbackFlow
        }

        val sequence = EditedMediaItemSequence(editedMediaItems)
        val composition = Composition.Builder(listOf(sequence)).build()

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                trySend(ExportState.Success(outputFile.absolutePath))
                close()
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                trySend(ExportState.Error(exportException.localizedMessage ?: "Export failed unexpectedly."))
                close()
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        try {
            transformer.start(composition, outputFile.absolutePath)
            trySend(ExportState.Progress(0.05f))
        } catch (e: Exception) {
            trySend(ExportState.Error("Failed to initialize video transformer: ${e.message}"))
            close()
        }

        awaitClose {
            transformer.cancel()
        }
    }
}
