package com.vdopro.app.data.repository

import com.vdopro.app.domain.model.VideoClip
import com.vdopro.app.domain.model.VideoProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class ProjectRepository {

    private val _projects = MutableStateFlow<List<VideoProject>>(
        listOf(
            VideoProject(
                id = "sample_1",
                name = "Vlog Highlight Sample",
                videoClips = listOf(
                    VideoClip(
                        id = "c1",
                        filePath = "sample1.mp4",
                        name = "Beach Intro",
                        sourceDurationMs = 8000L
                    ),
                    VideoClip(
                        id = "c2",
                        filePath = "sample2.mp4",
                        name = "Sunset Walk",
                        sourceDurationMs = 12000L
                    )
                )
            ),
            VideoProject(
                id = "sample_2",
                name = "AI Auto-Cut Drone Reel",
                videoClips = listOf(
                    VideoClip(
                        id = "c3",
                        filePath = "sample3.mp4",
                        name = "Mountain Flight",
                        sourceDurationMs = 15000L
                    )
                )
            )
        )
    )

    fun getAllProjects(): Flow<List<VideoProject>> = _projects.asStateFlow()

    fun createProject(name: String): VideoProject {
        val newProject = VideoProject(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Untitled Project" }
        )
        _projects.value = listOf(newProject) + _projects.value
        return newProject
    }

    fun getProjectById(id: String): VideoProject? {
        return _projects.value.find { it.id == id }
    }

    fun updateProject(project: VideoProject) {
        _projects.value = _projects.value.map {
            if (it.id == project.id) project else it
        }
    }

    fun deleteProject(id: String) {
        _projects.value = _projects.value.filterNot { it.id == id }
    }
}
