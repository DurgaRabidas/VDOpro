package com.vdopro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vdopro.app.ui.dialogs.AiToolsBottomSheet
import com.vdopro.app.ui.dialogs.ExportDialog
import com.vdopro.app.ui.editor.VideoEditorScreen
import com.vdopro.app.ui.editor.VideoEditorViewModel
import com.vdopro.app.ui.projects.ProjectListScreen
import com.vdopro.app.ui.projects.ProjectListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VDOproApp()
                }
            }
        }
    }
}

@Composable
fun VDOproApp() {
    var activeProjectId by remember { mutableStateOf<String?>(null) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val projectListViewModel: ProjectListViewModel = viewModel()
    val editorViewModel: VideoEditorViewModel = viewModel()

    val currentProjectId = activeProjectId
    if (currentProjectId == null) {
        ProjectListScreen(
            viewModel = projectListViewModel,
            onOpenProject = { projectId ->
                activeProjectId = projectId
            }
        )
    } else {
        VideoEditorScreen(
            projectId = currentProjectId,
            viewModel = editorViewModel,
            onBack = { activeProjectId = null },
            onOpenAiTools = { showAiBottomSheet = true },
            onExport = { showExportDialog = true }
        )

        if (showAiBottomSheet) {
            AiToolsBottomSheet(
                onDismiss = { showAiBottomSheet = false },
                onAiSilenceRemove = { editorViewModel.executeAiSilenceRemoval() },
                onAiAutoCut = { editorViewModel.executeAiAutoCut() },
                onAiSmartColor = { editorViewModel.executeAiSmartColor() }
            )
        }

        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onStartExport = { _, _ ->
                    showExportDialog = false
                }
            )
        }
    }
}
