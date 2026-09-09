package com.vdopro.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onStartExport: (resolution: String, fps: Int) -> Unit,
    isExporting: Boolean = false,
    exportProgress: Float = 0f
) {
    var selectedResolution by remember { mutableStateOf("1080p (FHD)") }
    var selectedFps by remember { mutableStateOf(30) }

    val resolutions = listOf("720p (HD)", "1080p (FHD)", "4K (UHD)")
    val frameRates = listOf(24, 30, 60)

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text("Export Video", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (isExporting) {
                    Text("Rendering local video file...", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { exportProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF03DAC6)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(exportProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                } else {
                    Text("Resolution", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        resolutions.forEach { res ->
                            val isSelected = res == selectedResolution
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) Color(0xFF03DAC6) else Color(0xFF333333),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedResolution = res }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = res.split(" ")[0],
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Frame Rate", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        frameRates.forEach { fps ->
                            val isSelected = fps == selectedFps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) Color(0xFF03DAC6) else Color(0xFF333333),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fps} FPS",
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isExporting) {
                Button(
                    onClick = { onStartExport(selectedResolution, selectedFps) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
                ) {
                    Text("Start Export", color = Color.Black)
                }
            }
        },
        dismissButton = {
            if (!isExporting) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        },
        containerColor = Color(0xFF2C2C2C)
    )
}
