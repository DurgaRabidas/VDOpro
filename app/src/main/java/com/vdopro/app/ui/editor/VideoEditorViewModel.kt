package com.vdopro.app.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdopro.app.ai.AiColorGradingEngine
import com.vdopro.app.ai.AiHighlightExtractor
import com.vdopro.app.ai.AiSceneDetector
import com.vdopro.app.ai.AiSilenceRemover
import com.vdopro.app.data.media.AudioSampleChunk
import com.vdopro.app.data.media.ExportState
import com.vdopro.app.data.media.MediaMetadataExtractor
import com.vdopro.app.data.media.VideoExporter
import com.vdopro.app.data.repository.ProjectRepository
import com.vdopro.app.domain.model.FilterType
import com.vdopro.app.domain.model.TimelineState
import com.vdopro.app.domain.model.VideoClip
import com.vdopro.app.domain.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class VideoEditorViewModel(
    private val repository: ProjectRepository = ProjectRepository(),
    private val silenceRemover: AiSilenceRemover = AiSilenceRemover(),
    private val sceneDetector: AiSceneDetector = AiSceneDetector(),
    private val highlightExtractor: AiHighlightExtractor = AiHighlightExtractor(),
    private val colorEngine: AiColorGradingEngine = AiColorGradingEngine()
) : ViewModel() {

    private val _timelineState = MutableStateFlow<TimelineState?>(null)
    val timelineState: StateFlow<TimelineState?> = _timelineState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun loadProject(projectId: String) {
        val project = repository.getProjectById(projectId) ?: VideoProject(id = projectId, name = "New Local Project")
        _timelineState.value = TimelineState(project = project)
    }

    fun addVideoClipFromUri(context: Context, uri: Uri) {
        val currentState = _timelineState.value ?: return
        viewModelScope.launch {
            val extractor = MediaMetadataExtractor(context)
            val info = extractor.extractVideoInfo(uri.toString())

            val clipName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported Video"
            val newClip = VideoClip(
                id = UUID.randomUUID().toString(),
                filePath = uri.toString(),
                name = clipName,
                sourceDurationMs = if (info.durationMs > 0) info.durationMs else 10000L
            )

            val updatedClips = currentState.project.videoClips + newClip
            val updatedProject = currentState.project.copy(videoClips = updatedClips)
            _timelineState.value = currentState.copy(project = updatedProject, selectedClipId = newClip.id)
            repository.updateProject(updatedProject)
        }
    }

    fun addSampleClip() {
        val currentState = _timelineState.value ?: return
        val count = currentState.project.videoClips.size + 1
        val newClip = VideoClip(
            id = UUID.randomUUID().toString(),
            filePath = "sample_$count.mp4",
            name = "Clip #$count",
            sourceDurationMs = 10000L
        )
        val updatedClips = currentState.project.videoClips + newClip
        val updatedProject = currentState.project.copy(videoClips = updatedClips)
        _timelineState.value = currentState.copy(project = updatedProject, selectedClipId = newClip.id)
        repository.updateProject(updatedProject)
    }

    fun selectClip(clipId: String?) {
        _timelineState.value = _timelineState.value?.copy(selectedClipId = clipId)
    }

    fun updateCurrentPosition(positionMs: Long) {
        _timelineState.value = _timelineState.value?.copy(currentPositionMs = positionMs)
    }

    fun splitCurrentClip() {
        val state = _timelineState.value ?: return
        val selectedClip = state.selectedClip ?: return
        val pos = state.currentPositionMs

        var accumulated = 0L
        for (clip in state.project.videoClips) {
            val duration = clip.trimmedDurationMs
            if (clip.id == selectedClip.id) {
                val offsetInClipMs = pos - accumulated
                if (offsetInClipMs in 100L until (duration - 100L)) {
                    val newState = state.splitClipAt(clip.id, offsetInClipMs)
                    _timelineState.value = newState
                    repository.updateProject(newState.project)
                }
                break
            }
            accumulated += duration
        }
    }

    fun removeSelectedClip() {
        val state = _timelineState.value ?: return
        val selectedId = state.selectedClipId ?: return
        val newState = state.removeClip(selectedId)
        _timelineState.value = newState
        repository.updateProject(newState.project)
    }

    fun updateClipSpeed(speed: Float) {
        val state = _timelineState.value ?: return
        val clip = state.selectedClip ?: return
        val updated = clip.copy(speed = speed)
        val newState = state.updateClip(updated)
        _timelineState.value = newState
        repository.updateProject(newState.project)
    }

    fun applyFilterToSelectedClip(filterType: FilterType) {
        val state = _timelineState.value ?: return
        val clip = state.selectedClip ?: return
        val updated = colorEngine.applyAiColorGrading(clip, filterType)
        val newState = state.updateClip(updated)
        _timelineState.value = newState
        repository.updateProject(newState.project)
    }

    fun startProjectExport(context: Context) {
        val project = _timelineState.value?.project ?: return
        val exporter = VideoExporter(context)

        viewModelScope.launch {
            exporter.exportProject(project, outputFileName = project.name.replace(" ", "_")).collect { state ->
                _exportState.value = state
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun executeAiSilenceRemoval() {
        val state = _timelineState.value ?: return
        val clip = state.selectedClip ?: return

        viewModelScope.launch {
            val chunks = (0L until clip.sourceDurationMs step 100L).map { t ->
                val isQuiet = (t in 2000L..4000L) || (t in 7000L..8500L)
                val amp = if (isQuiet) 0.01f else 0.5f
                AudioSampleChunk(timestampMs = t, durationMs = 100L, maxAmplitude = amp, isSilence = isQuiet)
            }

            val nonSilent = silenceRemover.detectNonSilentIntervals(chunks)
            val newClips = silenceRemover.applySilenceRemovalToClip(clip, nonSilent)

            val clipIdx = state.project.videoClips.indexOfFirst { it.id == clip.id }
            if (clipIdx != -1 && newClips.isNotEmpty()) {
                val updatedList = state.project.videoClips.toMutableList().apply {
                    removeAt(clipIdx)
                    addAll(clipIdx, newClips)
                }
                val updatedProject = state.project.copy(videoClips = updatedList)
                _timelineState.value = state.copy(project = updatedProject)
                repository.updateProject(updatedProject)
            }
        }
    }

    fun executeAiAutoCut() {
        val state = _timelineState.value ?: return
        val clip = state.selectedClip ?: return

        viewModelScope.launch {
            val cuts = listOf(
                com.vdopro.app.ai.SceneCutPoint(timestampMs = 3500L, differenceScore = 0.85f),
                com.vdopro.app.ai.SceneCutPoint(timestampMs = 7000L, differenceScore = 0.90f)
            )
            val newClips = sceneDetector.splitClipByScenes(clip, cuts)

            val clipIdx = state.project.videoClips.indexOfFirst { it.id == clip.id }
            if (clipIdx != -1 && newClips.isNotEmpty()) {
                val updatedList = state.project.videoClips.toMutableList().apply {
                    removeAt(clipIdx)
                    addAll(clipIdx, newClips)
                }
                val updatedProject = state.project.copy(videoClips = updatedList)
                _timelineState.value = state.copy(project = updatedProject)
                repository.updateProject(updatedProject)
            }
        }
    }

    fun executeAiSmartColor() {
        val state = _timelineState.value ?: return
        val clip = state.selectedClip ?: return
        val updated = colorEngine.autoEnhanceClip(clip, averageLuminance = 0.35f)
        val newState = state.updateClip(updated)
        _timelineState.value = newState
        repository.updateProject(newState.project)
    }
}
