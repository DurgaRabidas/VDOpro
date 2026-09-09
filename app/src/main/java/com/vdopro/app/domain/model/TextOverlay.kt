package com.vdopro.app.domain.model

import java.util.UUID

data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startTimeMs: Long,
    val durationMs: Long,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f,
    val fontSizeSp: Float = 24f,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String? = "#80000000"
)
