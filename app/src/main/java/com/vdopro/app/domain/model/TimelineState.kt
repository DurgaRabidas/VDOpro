package com.vdopro.app.domain.model

data class TimelineState(
    val project: VideoProject,
    val currentPositionMs: Long = 0L,
    val selectedClipId: String? = null,
    val selectedAudioTrackId: String? = null,
    val selectedTextOverlayId: String? = null,
    val isPlaying: Boolean = false,
    val zoomLevel: Float = 1.0f
) {
    val selectedClip: VideoClip?
        get() = project.videoClips.find { it.id == selectedClipId }

    fun updateClip(updatedClip: VideoClip): TimelineState {
        val newClips = project.videoClips.map {
            if (it.id == updatedClip.id) updatedClip else it
        }
        return copy(project = project.copy(videoClips = newClips, updatedAtMs = System.currentTimeMillis()))
    }

    fun splitClipAt(clipId: String, splitOffsetInClipMs: Long): TimelineState {
        val clipIndex = project.videoClips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return this

        val clip = project.videoClips[clipIndex]
        val sourceSplitPoint = clip.startInSourceMs + (splitOffsetInClipMs * clip.speed).toLong()

        if (sourceSplitPoint <= clip.startInSourceMs || sourceSplitPoint >= clip.endInSourceMs) {
            return this
        }

        val clip1 = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            endInSourceMs = sourceSplitPoint
        )
        val clip2 = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            startInSourceMs = sourceSplitPoint
        )

        val newClips = project.videoClips.toMutableList().apply {
            removeAt(clipIndex)
            add(clipIndex, clip2)
            add(clipIndex, clip1)
        }

        return copy(
            project = project.copy(videoClips = newClips, updatedAtMs = System.currentTimeMillis()),
            selectedClipId = clip1.id
        )
    }

    fun reorderClips(fromIndex: Int, toIndex: Int): TimelineState {
        if (fromIndex !in project.videoClips.indices || toIndex !in project.videoClips.indices) return this
        val newClips = project.videoClips.toMutableList()
        val movedClip = newClips.removeAt(fromIndex)
        newClips.add(toIndex, movedClip)
        return copy(project = project.copy(videoClips = newClips, updatedAtMs = System.currentTimeMillis()))
    }

    fun removeClip(clipId: String): TimelineState {
        val newClips = project.videoClips.filterNot { it.id == clipId }
        val newSelectedId = if (selectedClipId == clipId) null else selectedClipId
        return copy(
            project = project.copy(videoClips = newClips, updatedAtMs = System.currentTimeMillis()),
            selectedClipId = newSelectedId
        )
    }
}
