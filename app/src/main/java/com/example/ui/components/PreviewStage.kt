package com.example.ui.components

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.R
import com.example.model.FilterSettings
import com.example.model.MediaClip
import com.example.model.MediaType
import com.example.model.TextLayer
import com.example.util.MediaUtils
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PreviewStage(
    activeClip: MediaClip?,
    filters: FilterSettings,
    textLayers: List<TextLayer>,
    selectedTextLayerId: String?,
    exoPlayer: ExoPlayer,
    videoAspectRatio: Float,
    isPlaying: Boolean,
    isMuted: Boolean,
    isLooping: Boolean,
    videoPositionMs: Long,
    videoDurationMs: Long,
    hasAnyContent: Boolean,
    onImportClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
    onClearClick: () -> Unit,
    onSelectTextLayer: (String) -> Unit,
    onDragTextLayer: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.large,
            ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val stageBackground = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    MaterialTheme.colorScheme.background,
                ),
            )

            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 230.dp, max = 380.dp)
                    .aspectRatio(16f / 10f, matchHeightConstraintsFirst = false)
                    .clip(MaterialTheme.shapes.medium)
                    .background(stageBackground)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                    )
                    .testTag("preview_stage"),
            ) {
                val stageWidthPx = with(LocalDensity.current) { maxWidth.toPx() }.coerceAtLeast(1f)
                val stageHeightPx = with(LocalDensity.current) { maxHeight.toPx() }.coerceAtLeast(1f)
                val density = LocalDensity.current

                // Subtle studio grid lines on empty canvas
                StudioStageGridBackdrop()

                if (activeClip == null) {
                    EmptyStagePlaceholder(
                        onImportClick = onImportClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val androidColorMatrix = remember(filters) {
                        MediaUtils.buildAndroidColorMatrix(
                            contrast = filters.contrast,
                            saturation = filters.saturation,
                            brightness = filters.brightness,
                        )
                    }
                    val composeColorFilter = remember(androidColorMatrix) {
                        ColorFilter.colorMatrix(ColorMatrix(androidColorMatrix.array))
                    }

                    if (activeClip.mediaType == MediaType.IMAGE) {
                        val imageModifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (filters.blurRadiusDp > 0.2f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(filters.blurRadiusDp.dp)
                                } else {
                                    Modifier
                                },
                            )
                        AsyncImage(
                            model = activeClip.uri,
                            contentDescription = activeClip.displayName,
                            contentScale = ContentScale.Fit,
                            colorFilter = composeColorFilter,
                            modifier = imageModifier.testTag("preview_image"),
                        )
                        if (filters.blurRadiusDp > 0.2f && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = (filters.blurRadiusDp / 40f).coerceAtMost(0.28f))),
                            )
                        }
                    } else {
                        val videoRenderEffect = remember(filters, androidColorMatrix, density) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val colorEffect = RenderEffect.createColorFilterEffect(
                                    ColorMatrixColorFilter(androidColorMatrix),
                                )
                                val blurPx = with(density) { filters.blurRadiusDp.dp.toPx() }
                                val combined = if (blurPx > 0.5f) {
                                    val blurEffect = RenderEffect.createBlurEffect(
                                        blurPx,
                                        blurPx,
                                        Shader.TileMode.CLAMP,
                                    )
                                    RenderEffect.createChainEffect(blurEffect, colorEffect)
                                } else {
                                    colorEffect
                                }
                                combined.asComposeRenderEffect()
                            } else {
                                null
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(onClick = onPlayPauseToggle),
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    TextureView(ctx).also { tv ->
                                        exoPlayer.setVideoTextureView(tv)
                                    }
                                },
                                update = { tv ->
                                    exoPlayer.setVideoTextureView(tv)
                                },
                                modifier = Modifier
                                    .aspectRatio(videoAspectRatio.coerceIn(0.4f, 2.5f))
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (videoRenderEffect != null) {
                                            renderEffect = videoRenderEffect
                                        }
                                    }
                                    .testTag("preview_video"),
                            )

                            // Graceful filter feedback overlay on Android < 12 where RenderEffect is unavailable
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && filters.isModified) {
                                val brightDelta = (filters.brightness - 1f).coerceIn(-0.4f, 0.4f)
                                val overlayAlpha = abs(brightDelta) * 0.4f + (filters.blurRadiusDp / 40f)
                                val tint = if (brightDelta >= 0f) Color.White else Color.Black
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(tint.copy(alpha = overlayAlpha.coerceIn(0f, 0.3f))),
                                )
                            }
                        }
                    }
                }

                // Active snap guides when dragging near center
                var isDraggingLayer by remember { mutableStateOf(false) }
                val activeDragged = textLayers.firstOrNull { it.id == selectedTextLayerId }
                androidx.compose.animation.AnimatedVisibility(
                    visible = isDraggingLayer && activeDragged != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    if (activeDragged != null) {
                        val snapX = abs(activeDragged.normalizedX - 0.5f) < 0.03f
                        val snapY = abs(activeDragged.normalizedY - 0.5f) < 0.03f
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (snapX) {
                                drawLine(
                                    color = Color(0xFFFF704E),
                                    start = Offset(size.width / 2f, 0f),
                                    end = Offset(size.width / 2f, size.height),
                                    strokeWidth = 2f,
                                )
                            }
                            if (snapY) {
                                drawLine(
                                    color = Color(0xFFFF704E),
                                    start = Offset(0f, size.height / 2f),
                                    end = Offset(size.width, size.height / 2f),
                                    strokeWidth = 2f,
                                )
                            }
                        }
                    }
                }

                // Interactive draggable text overlay layers
                textLayers.filter { it.isVisible && it.text.isNotBlank() }.forEach { layer ->
                    val isSelected = layer.id == selectedTextLayerId
                    var layerWidthPx by remember { mutableStateOf(0) }
                    var layerHeightPx by remember { mutableStateOf(0) }

                    val posX = (layer.normalizedX * stageWidthPx - layerWidthPx / 2f).roundToInt()
                    val posY = (layer.normalizedY * stageHeightPx - layerHeightPx / 2f).roundToInt()

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(posX, posY) }
                            .widthIn(max = maxWidth * 0.88f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (layer.color.hasBackdropPill) {
                                    Modifier.background(Color(0xD40B0D12))
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onSelectTextLayer(layer.id) }
                            .pointerInput(layer.id, stageWidthPx, stageHeightPx) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDraggingLayer = true
                                        onSelectTextLayer(layer.id)
                                    },
                                    onDragEnd = { isDraggingLayer = false },
                                    onDragCancel = { isDraggingLayer = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDragTextLayer(
                                            layer.id,
                                            dragAmount.x / stageWidthPx,
                                            dragAmount.y / stageHeightPx,
                                        )
                                    },
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("overlay_text_layer"),
                    ) {
                        Text(
                            text = layer.text,
                            color = Color(layer.color.argb),
                            fontSize = layer.fontSizeSp.sp,
                            lineHeight = (layer.fontSizeSp * 1.18f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            onTextLayout = { result ->
                                layerWidthPx = result.size.width + 20
                                layerHeightPx = result.size.height + 8
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    offset = Offset(0f, 3f),
                                    blurRadius = 10f,
                                ),
                            ),
                        )
                    }
                }

                // Active clip badge in corner
                if (activeClip != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xB80B0D12))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (activeClip.mediaType == MediaType.VIDEO) "VIDEO" else "IMAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Video transport scrubber when a video clip is active
            if (activeClip?.mediaType == MediaType.VIDEO) {
                Spacer(modifier = Modifier.height(10.dp))
                VideoTransportBar(
                    isPlaying = isPlaying,
                    isMuted = isMuted,
                    isLooping = isLooping,
                    positionMs = videoPositionMs,
                    durationMs = videoDurationMs,
                    onPlayPauseToggle = onPlayPauseToggle,
                    onSeekTo = onSeekTo,
                    onToggleMute = onToggleMute,
                    onToggleLoop = onToggleLoop,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary stage control buttons (matching prototype: Import Media, Preview, Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onImportClick,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .minimumInteractiveComponentSize()
                        .testTag("import_media_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_import_media),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                val canPlayVideo = activeClip?.mediaType == MediaType.VIDEO
                FilledTonalButton(
                    onClick = onPlayPauseToggle,
                    enabled = canPlayVideo,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("preview_play_button"),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (isPlaying) R.string.action_preview_pause else R.string.action_preview_play,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                OutlinedButton(
                    onClick = onClearClick,
                    enabled = hasAnyContent,
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_project_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = stringResource(R.string.action_clear),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioStageGridBackdrop() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.16f)) {
        val step = 36.dp.toPx()
        var x = step
        while (x < size.width) {
            drawLine(
                color = Color(0xFF49556B),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f,
            )
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(
                color = Color(0xFF49556B),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += step
        }
    }
}

@Composable
private fun EmptyStagePlaceholder(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onImportClick)
            .padding(24.dp)
            .testTag("empty_stage_placeholder"),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    shape = CircleShape,
                ),
        ) {
            Text(
                text = stringResource(R.string.empty_stage_monogram),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_stage_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_stage_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VideoTransportBar(
    isPlaying: Boolean,
    isMuted: Boolean,
    isLooping: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleMute: () -> Unit,
    onToggleLoop: () -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_pause),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = "${MediaUtils.formatTimecode(positionMs)} / ${MediaUtils.formatTimecode(durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Slider(
            value = progress,
            onValueChange = { frac ->
                onSeekTo((frac * safeDuration).roundToInt().toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        IconButton(
            onClick = onToggleMute,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = stringResource(R.string.cd_mute_toggle),
                tint = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onToggleLoop,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                imageVector = if (isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.cd_loop_toggle),
                tint = if (isLooping) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
