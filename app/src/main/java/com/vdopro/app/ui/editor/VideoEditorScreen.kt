package com.vdopro.app.ui.editor

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
    val context = LocalContext.current
    val state by viewModel.timelineState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val currentProject = state?.project

    // Media Picker for adding real local video clips
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.addVideoClipFromUri(context, it)
        }
    }

    // ExoPlayer Setup for Live Local Video Preview
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isExoPlaying by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isExoPlaying = isPlaying
                }
            })
        }
        exoPlayer = player
        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    // Update ExoPlayer MediaItem when selected clip changes
    val selectedClip = state?.selectedClip
    LaunchedEffect(selectedClip?.filePath) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (selectedClip != null && selectedClip.filePath.isNotBlank()) {
            val uri = if (selectedClip.filePath.startsWith("content://") || selectedClip.filePath.startsWith("file://")) {
                Uri.parse(selectedClip.filePath)
            } else {
                Uri.fromFile(java.io.File(selectedClip.filePath))
            }
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
        } else {
            player.clearMediaItems()
        }
    }

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
                    IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Import Local Video", tint = Color.White)
                    }
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
            // Live Preview Player Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedClip != null && exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.height(64.dp).width(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedClip != null) selectedClip.name else "Tap '+' to import a video or select a clip",
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
            }

            // Playhead Seek Bar
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
                    onValueChange = {
                        viewModel.updateCurrentPosition(it.toLong())
                        exoPlayer?.seekTo(it.toLong())
                    },
                    valueRange = 0f..totalDuration.coerceAtLeast(1L).toFloat()
                )
            }

            // Timeline Clips Row
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
                        Text(
                            text = "No clips in timeline. Tap top '+' icon to import local videos!",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
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

            // Quick Toolbar Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF252525))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Import Local Video", tint = Color.White)
                }
                IconButton(onClick = {
                    val player = exoPlayer
                    if (player != null) {
                        if (player.isPlaying) player.pause() else player.play()
                    }
                }) {
                    Icon(
                        imageVector = if (isExoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
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
