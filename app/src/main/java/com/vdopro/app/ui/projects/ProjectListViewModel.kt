package com.vdopro.app.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vdopro.app.data.repository.ProjectRepository
import com.vdopro.app.domain.model.VideoProject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProjectListViewModel(
    private val repository: ProjectRepository = ProjectRepository()
) : ViewModel() {

    val projects: StateFlow<List<VideoProject>> = repository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewProject(name: String): VideoProject {
        return repository.createProject(name)
    }

    fun deleteProject(id: String) {
        repository.deleteProject(id)
    }
}
