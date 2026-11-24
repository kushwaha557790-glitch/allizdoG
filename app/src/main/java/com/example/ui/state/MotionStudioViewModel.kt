package com.example.ui.state

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.FilterPreset
import com.example.model.FilterSettings
import com.example.model.MediaClip
import com.example.model.MotionStudioUiState
import com.example.model.StudioToolTab
import com.example.model.TextLayer
import com.example.model.TextStyleColor
import com.example.util.MediaUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MotionStudioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MotionStudioUiState())
    val uiState: StateFlow<MotionStudioUiState> = _uiState.asStateFlow()

    fun importMedia(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext
        viewModelScope.launch {
            val newClips = uris.map { uri ->
                MediaUtils.resolveMediaMetadata(appContext, uri)
            }
            _uiState.update { current ->
                val combined = current.clips + newClips
                val nextActiveId = current.activeClipId ?: newClips.firstOrNull()?.id
                current.copy(
                    clips = combined,
                    activeClipId = nextActiveId,
                    statusMessage = if (newClips.size == 1) {
                        "Imported ${newClips.first().displayName}"
                    } else {
                        "Imported ${newClips.size} media clips"
                    },
                )
            }
        }
    }

    fun selectClip(clipId: String) {
        _uiState.update { current ->
            if (current.clips.any { it.id == clipId }) {
                current.copy(activeClipId = clipId, isPlaying = false)
            } else {
                current
            }
        }
    }

    fun removeClip(clipId: String) {
        _uiState.update { current ->
            val updated = current.clips.filterNot { it.id == clipId }
            val nextActive = if (current.activeClipId == clipId) {
                updated.firstOrNull()?.id
            } else {
                current.activeClipId
            }
            current.copy(
                clips = updated,
                activeClipId = nextActive,
                isPlaying = if (updated.isEmpty()) false else current.isPlaying,
            )
        }
    }

    fun moveClip(clipId: String, delta: Int) {
        _uiState.update { current ->
            val list = current.clips.toMutableList()
            val index = list.indexOfFirst { it.id == clipId }
            val target = index + delta
            if (index == -1 || target !in list.indices) return@update current
            val item = list.removeAt(index)
            list.add(target, item)
            current.copy(clips = list)
        }
    }

    fun clearProject() {
        _uiState.update {
            MotionStudioUiState(statusMessage = "Workspace cleared")
        }
    }

    fun selectToolTab(tab: StudioToolTab) {
        _uiState.update { it.copy(activeToolTab = tab) }
    }

    fun applyPreset(preset: FilterPreset) {
        _uiState.update { current ->
            current.copy(
                filters = FilterSettings(
                    preset = preset,
                    contrast = preset.contrast,
                    saturation = preset.saturation,
                    brightness = preset.brightness,
                    blurRadiusDp = preset.blurRadiusDp,
                ),
            )
        }
    }

    fun updateContrast(contrast: Float) {
        _uiState.update { current ->
            current.copy(
                filters = current.filters.copy(
                    preset = FilterPreset.COLOR_BOOST,
                    contrast = contrast.coerceIn(0.4f, 2.0f),
                )
            )
        }
    }

    fun updateSaturation(saturation: Float) {
        _uiState.update { current ->
            current.copy(
                filters = current.filters.copy(
                    preset = FilterPreset.COLOR_BOOST,
                    saturation = saturation.coerceIn(0f, 2.2f),
                )
            )
        }
    }

    fun updateBlur(blurRadiusDp: Float) {
        _uiState.update { current ->
            current.copy(
                filters = current.filters.copy(
                    preset = if (blurRadiusDp > 0.1f) FilterPreset.SOFT_BLUR else current.filters.preset,
                    blurRadiusDp = blurRadiusDp.coerceIn(0f, 16f),
                )
            )
        }
    }

    fun updateBrightness(brightness: Float) {
        _uiState.update { current ->
            current.copy(
                filters = current.filters.copy(
                    preset = FilterPreset.BRIGHT,
                    brightness = brightness.coerceIn(0.4f, 1.8f),
                )
            )
        }
    }

    fun resetFilters() {
        _uiState.update { current ->
            current.copy(
                filters = FilterSettings(),
                statusMessage = "Effects reset to original",
            )
        }
    }

    fun updateDraftText(text: String) {
        _uiState.update { current ->
            val selectedId = current.selectedTextLayerId
            val updatedLayers = if (selectedId != null) {
                current.textLayers.map { layer ->
                    if (layer.id == selectedId) layer.copy(text = text) else layer
                }
            } else {
                current.textLayers
            }
            current.copy(
                draftText = text,
                textLayers = updatedLayers,
            )
        }
    }

    fun updateDraftFontSize(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(16f, 70f)
        _uiState.update { current ->
            val selectedId = current.selectedTextLayerId
            val updatedLayers = if (selectedId != null) {
                current.textLayers.map { layer ->
                    if (layer.id == selectedId) layer.copy(fontSizeSp = clamped) else layer
                }
            } else {
                current.textLayers
            }
            current.copy(
                draftFontSizeSp = clamped,
                textLayers = updatedLayers,
            )
        }
    }

    fun updateDraftTextColor(color: TextStyleColor) {
        _uiState.update { current ->
            val selectedId = current.selectedTextLayerId
            val updatedLayers = if (selectedId != null) {
                current.textLayers.map { layer ->
                    if (layer.id == selectedId) layer.copy(color = color) else layer
                }
            } else {
                current.textLayers
            }
            current.copy(
                draftTextColor = color,
                textLayers = updatedLayers,
            )
        }
    }

    fun addOrCommitTextLayer() {
        _uiState.update { current ->
            val content = current.draftText.trim().ifEmpty { "Your text" }
            val selected = current.selectedTextLayer
            if (selected != null) {
                val updated = current.textLayers.map { layer ->
                    if (layer.id == selected.id) {
                        layer.copy(
                            text = content,
                            fontSizeSp = current.draftFontSizeSp,
                            color = current.draftTextColor,
                            isVisible = true,
                        )
                    } else {
                        layer
                    }
                }
                current.copy(
                    textLayers = updated,
                    draftText = content,
                    statusMessage = "Updated text layer",
                )
            } else {
                // Offset new layers slightly if there are already layers so they don't completely stack
                val offsetStep = (current.textLayers.size % 3) * 0.08f
                val newLayer = TextLayer(
                    text = content,
                    fontSizeSp = current.draftFontSizeSp,
                    color = current.draftTextColor,
                    normalizedY = (0.5f + offsetStep).coerceIn(0.2f, 0.85f),
                )
                current.copy(
                    textLayers = current.textLayers + newLayer,
                    selectedTextLayerId = newLayer.id,
                    draftText = content,
                    statusMessage = "Added text layer",
                )
            }
        }
    }

    fun startNewTextLayer() {
        _uiState.update { current ->
            current.copy(
                selectedTextLayerId = null,
                draftText = "",
                activeToolTab = StudioToolTab.TEXT,
            )
        }
    }

    fun selectTextLayer(layerId: String) {
        _uiState.update { current ->
            val target = current.textLayers.firstOrNull { it.id == layerId } ?: return@update current
            current.copy(
                selectedTextLayerId = target.id,
                draftText = target.text,
                draftFontSizeSp = target.fontSizeSp,
                draftTextColor = target.color,
                activeToolTab = StudioToolTab.TEXT,
            )
        }
    }

    fun moveTextLayer(layerId: String, deltaNormX: Float, deltaNormY: Float) {
        _uiState.update { current ->
            current.copy(
                selectedTextLayerId = layerId,
                textLayers = current.textLayers.map { layer ->
                    if (layer.id == layerId) {
                        layer.copy(
                            normalizedX = (layer.normalizedX + deltaNormX).coerceIn(0.08f, 0.92f),
                            normalizedY = (layer.normalizedY + deltaNormY).coerceIn(0.08f, 0.92f),
                        )
                    } else {
                        layer
                    }
                },
            )
        }
    }

    fun centerSelectedTextLayer() {
        _uiState.update { current ->
            val targetId = current.selectedTextLayerId ?: return@update current
            current.copy(
                textLayers = current.textLayers.map { layer ->
                    if (layer.id == targetId) {
                        layer.copy(normalizedX = 0.5f, normalizedY = 0.5f)
                    } else {
                        layer
                    }
                },
            )
        }
    }

    fun toggleTextLayerVisibility(layerId: String) {
        _uiState.update { current ->
            current.copy(
                textLayers = current.textLayers.map { layer ->
                    if (layer.id == layerId) layer.copy(isVisible = !layer.isVisible) else layer
                },
            )
        }
    }

    fun removeTextLayer(layerId: String) {
        _uiState.update { current ->
            val remaining = current.textLayers.filterNot { it.id == layerId }
            val nextSelected = if (current.selectedTextLayerId == layerId) {
                remaining.lastOrNull()
            } else {
                current.selectedTextLayer
            }
            current.copy(
                textLayers = remaining,
                selectedTextLayerId = nextSelected?.id,
                draftText = nextSelected?.text ?: "",
                draftFontSizeSp = nextSelected?.fontSizeSp ?: 30f,
                draftTextColor = nextSelected?.color ?: TextStyleColor.WHITE,
            )
        }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleLoop() {
        _uiState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun buildNotesText(): String = MediaUtils.buildProjectNotesText(_uiState.value)

    fun shareProjectNotes(context: Context) {
        val notes = buildNotesText()
        MediaUtils.sharePlainText(context, notes, "Share Motion Studio Notes")
    }

    fun writeNotesToUri(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        val notes = buildNotesText()
        viewModelScope.launch {
            runCatching {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(notes.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                _uiState.update { it.copy(statusMessage = "Saved project notes") }
            }.onFailure {
                _uiState.update { it.copy(statusMessage = "Could not save notes") }
            }
        }
    }

    fun exportSnapshotToUri(context: Context, uri: Uri, videoPositionMs: Long) {
        val appContext = context.applicationContext
        val snapshotState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val bitmap = MediaUtils.renderSnapshotBitmap(
                context = appContext,
                clip = snapshotState.activeClip,
                filters = snapshotState.filters,
                textLayers = snapshotState.textLayers,
                videoPositionMs = videoPositionMs,
            )
            if (bitmap == null) {
                _uiState.update {
                    it.copy(isExporting = false, statusMessage = "Unable to render active frame")
                }
                return@launch
            }
            val result = runCatching {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
            }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    statusMessage = if (result.isSuccess) "Exported edited frame" else "Export failed",
                )
            }
        }
    }

    fun shareSnapshot(context: Context, videoPositionMs: Long) {
        val appContext = context.applicationContext
        val snapshotState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val bitmap = MediaUtils.renderSnapshotBitmap(
                context = appContext,
                clip = snapshotState.activeClip,
                filters = snapshotState.filters,
                textLayers = snapshotState.textLayers,
                videoPositionMs = videoPositionMs,
            )
            if (bitmap == null) {
                _uiState.update {
                    it.copy(isExporting = false, statusMessage = "Unable to render active frame")
                }
                return@launch
            }
            runCatching {
                val file = MediaUtils.exportBitmapToCache(appContext, bitmap)
                bitmap.recycle()
                MediaUtils.shareFile(context, file, "image/png", "Share Motion Studio Frame")
            }.onFailure {
                _uiState.update { it.copy(statusMessage = "Could not share frame") }
            }
            _uiState.update { it.copy(isExporting = false) }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
