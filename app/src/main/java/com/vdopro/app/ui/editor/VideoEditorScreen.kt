package com.vdopro.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vdopro.app.domain.model.FilterType
import com.vdopro.app.domain.model.VideoClip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    projectId: String,
    viewModel: VideoEditorViewModel,
    onBack: () -> Unit,
    onOpenAiTools: () -> Unit,
    onExport: () -> Unit
) {
    val state by viewModel.timelineState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val currentProject = state?.project

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject?.name ?: "Video Editor", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAiTools) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Tools", tint = Color(0xFFBB86FC))
                    }
                    IconButton(onClick = onExport) {
                        Icon(imageVector = Icons.Default.IosShare, contentDescription = "Export", tint = Color(0xFF03DAC6))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                val selectedClip = state?.selectedClip
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.height(64.dp).width(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedClip?.name ?: "Tap a clip to view preview",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    if (selectedClip != null && selectedClip.activeFilter != FilterType.NONE) {
                        Text(
                            text = "Filter: ${selectedClip.activeFilter.displayName}",
                            color = Color(0xFFBB86FC),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            val totalDuration = currentProject?.totalDurationMs ?: 1L
            val currentPos = state?.currentPositionMs ?: 0L
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(currentPos), color = Color.Gray, fontSize = 12.sp)
                    Text(text = formatTime(totalDuration), color = Color.Gray, fontSize = 12.sp)
                }
                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { viewModel.updateCurrentPosition(it.toLong()) },
                    valueRange = 0f..totalDuration.coerceAtLeast(1L).toFloat()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF1A1A1A))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                val clips = currentProject?.videoClips ?: emptyList()
                if (clips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No clips in timeline. Tap '+' below to add!", color = Color.Gray)
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(clips, key = { it.id }) { clip ->
                            val isSelected = clip.id == state?.selectedClipId
                            ClipTimelineItem(
                                clip = clip,
                                isSelected = isSelected,
                                onClick = { viewModel.selectClip(clip.id) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252525))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { viewModel.addSampleClip() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Clip", tint = Color.White)
                }
                IconButton(onClick = { viewModel.splitCurrentClip() }) {
                    Icon(imageVector = Icons.Default.ContentCut, contentDescription = "Split Clip", tint = Color.White)
                }
                IconButton(onClick = { viewModel.removeSelectedClip() }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
                }
                IconButton(onClick = { viewModel.updateClipSpeed(1.5f) }) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = "Speed", tint = Color.White)
                }
                IconButton(onClick = { viewModel.applyFilterToSelectedClip(FilterType.CINEMATIC) }) {
                    Icon(imageVector = Icons.Default.Style, contentDescription = "Filter", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ClipTimelineItem(
    clip: VideoClip,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val durationSec = clip.trimmedDurationMs / 1000L
    val itemWidth = (durationSec * 15).coerceAtLeast(80).toInt().dp
    Box(
        modifier = Modifier
            .width(itemWidth)
            .fillMaxHeight()
            .background(
                color = if (isSelected) Color(0xFF3700B3) else Color(0xFF424242),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color(0xFFBB86FC) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = clip.name,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = "${durationSec}s",
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
