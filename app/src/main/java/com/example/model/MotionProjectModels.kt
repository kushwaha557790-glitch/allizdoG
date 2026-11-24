package com.example.model

import android.net.Uri
import java.util.UUID

enum class MediaType {
    VIDEO,
    IMAGE,
}

data class MediaClip(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val displayName: String,
    val mediaType: MediaType,
    val durationMs: Long = 0L,
    val mimeType: String = "",
)

enum class FilterPreset(
    val label: String,
    val contrast: Float,
    val saturation: Float,
    val brightness: Float,
    val blurRadiusDp: Float,
) {
    NONE("Original", 1f, 1f, 1f, 0f),
    COLOR_BOOST("Color Boost", 1.2f, 1.3f, 1f, 0f),
    SOFT_BLUR("Soft Blur", 1f, 1f, 1f, 5f),
    BRIGHT("Bright Pop", 1.05f, 1.08f, 1.18f, 0f),
    CINEMA("Warm Cinema", 1.18f, 0.88f, 0.96f, 0f),
    NOIR("Noir Mono", 1.32f, 0f, 1.04f, 0f),
}

data class FilterSettings(
    val preset: FilterPreset = FilterPreset.NONE,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val brightness: Float = 1f,
    val blurRadiusDp: Float = 0f,
) {
    val isModified: Boolean
        get() = contrast != 1f || saturation != 1f || brightness != 1f || blurRadiusDp > 0.05f
}

enum class StudioToolTab {
    TEXT,
    FILTERS,
    COLOR,
    BLUR,
    BRIGHTNESS,
}

enum class TextStyleColor(
    val label: String,
    val argb: Long,
    val hasBackdropPill: Boolean = false,
) {
    WHITE("White", 0xFFFFFFFF),
    CORAL("Coral", 0xFFFF704E),
    AMBER("Amber", 0xFFFFC857),
    CYAN("Cyan", 0xFF4CE0D2),
    DARK_PILL("Dark Pill", 0xFFFFFFFF, hasBackdropPill = true),
}

data class TextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fontSizeSp: Float = 30f,
    val color: TextStyleColor = TextStyleColor.WHITE,
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    val isVisible: Boolean = true,
)

data class MotionStudioUiState(
    val clips: List<MediaClip> = emptyList(),
    val activeClipId: String? = null,
    val textLayers: List<TextLayer> = emptyList(),
    val selectedTextLayerId: String? = null,
    val draftText: String = "",
    val draftFontSizeSp: Float = 30f,
    val draftTextColor: TextStyleColor = TextStyleColor.WHITE,
    val filters: FilterSettings = FilterSettings(),
    val activeToolTab: StudioToolTab = StudioToolTab.TEXT,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val isLooping: Boolean = true,
    val isExporting: Boolean = false,
    val statusMessage: String? = null,
) {
    val activeClip: MediaClip?
        get() = clips.firstOrNull { it.id == activeClipId } ?: clips.firstOrNull()

    val selectedTextLayer: TextLayer?
        get() = textLayers.firstOrNull { it.id == selectedTextLayerId }
}
