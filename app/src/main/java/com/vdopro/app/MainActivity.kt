package com.vdopro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vdopro.app.data.media.ExportState
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
    val context = LocalContext.current
    var activeProjectId by remember { mutableStateOf<String?>(null) }
    var showAiBottomSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val projectListViewModel: ProjectListViewModel = viewModel()
    val editorViewModel: VideoEditorViewModel = viewModel()
    val exportState by editorViewModel.exportState.collectAsState()

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
            val isExporting = exportState is ExportState.Progress
            val progress = when (val s = exportState) {
                is ExportState.Progress -> s.percent
                is ExportState.Success -> 1.0f
                else -> 0f
            }

            ExportDialog(
                onDismiss = {
                    showExportDialog = false
                    editorViewModel.resetExportState()
                },
                onStartExport = { _, _ ->
                    editorViewModel.startProjectExport(context)
                },
                isExporting = isExporting,
                exportProgress = progress
            )
        }
    }
}
