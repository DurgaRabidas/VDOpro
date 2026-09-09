package com.vdopro.app.ai

import com.vdopro.app.domain.model.FilterType
import com.vdopro.app.domain.model.VideoClip

data class ColorAdjustmentPreset(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float
)

class AiColorGradingEngine {

    fun getPresetForFilter(filterType: FilterType): ColorAdjustmentPreset {
        return when (filterType) {
            FilterType.NONE -> ColorAdjustmentPreset(0f, 1.0f, 1.0f)
            FilterType.AI_SMART_ENHANCE -> ColorAdjustmentPreset(0.08f, 1.25f, 1.20f)
            FilterType.CINEMATIC -> ColorAdjustmentPreset(-0.05f, 1.35f, 0.85f)
            FilterType.VINTAGE -> ColorAdjustmentPreset(0.10f, 0.90f, 0.75f)
            FilterType.MONOCHROME -> ColorAdjustmentPreset(0.00f, 1.40f, 0.00f)
            FilterType.VIBRANT -> ColorAdjustmentPreset(0.05f, 1.20f, 1.50f)
            FilterType.WARM -> ColorAdjustmentPreset(0.08f, 1.10f, 1.15f)
            FilterType.COOL -> ColorAdjustmentPreset(-0.02f, 1.15f, 0.95f)
            FilterType.PORTRAIT_GLOW -> ColorAdjustmentPreset(0.12f, 1.05f, 1.10f)
        }
    }

    fun applyAiColorGrading(clip: VideoClip, filterType: FilterType): VideoClip {
        val preset = getPresetForFilter(filterType)
        return clip.copy(
            activeFilter = filterType,
            brightness = preset.brightness,
            contrast = preset.contrast,
            saturation = preset.saturation
        )
    }

    fun autoEnhanceClip(clip: VideoClip, averageLuminance: Float = 0.45f): VideoClip {
        val targetLuminance = 0.5f
        val brightnessDelta = (targetLuminance - averageLuminance).coerceIn(-0.3f, 0.3f)
        val optimalContrast = if (averageLuminance < 0.3f) 1.3f else 1.15f

        return clip.copy(
            activeFilter = FilterType.AI_SMART_ENHANCE,
            brightness = brightnessDelta,
            contrast = optimalContrast,
            saturation = 1.20f
        )
    }
}
