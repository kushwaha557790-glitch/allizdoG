package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FilterPreset
import com.example.model.FilterSettings
import com.example.model.StudioToolTab
import java.util.Locale

@Composable
fun ToolsDeck(
    activeTab: StudioToolTab,
    filters: FilterSettings,
    onSelectTab: (StudioToolTab) -> Unit,
    onApplyPreset: (FilterPreset) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onResetFilters: () -> Unit,
    onQuickAddText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.section_tools),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (filters.isModified) {
                    Text(
                        text = filters.preset.label.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Text, Filters, Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudioToolTile(
                    icon = Icons.Rounded.TextFields,
                    title = stringResource(R.string.tool_text),
                    subtitle = stringResource(R.string.tool_text_sub),
                    isActive = activeTab == StudioToolTab.TEXT,
                    onClick = {
                        onSelectTab(StudioToolTab.TEXT)
                        onQuickAddText()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_text"),
                )
                StudioToolTile(
                    icon = Icons.Rounded.Contrast,
                    title = stringResource(R.string.tool_filters),
                    subtitle = stringResource(R.string.tool_filters_sub),
                    isActive = activeTab == StudioToolTab.FILTERS,
                    onClick = { onSelectTab(StudioToolTab.FILTERS) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_filters"),
                )
                StudioToolTile(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.tool_color),
                    subtitle = stringResource(R.string.tool_color_sub),
                    isActive = activeTab == StudioToolTab.COLOR,
                    onClick = {
                        onSelectTab(StudioToolTab.COLOR)
                        if (filters.contrast == 1f && filters.saturation == 1f) {
                            onApplyPreset(FilterPreset.COLOR_BOOST)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_color"),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Blur, Brightness, Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudioToolTile(
                    icon = Icons.Rounded.BlurOn,
                    title = stringResource(R.string.tool_blur),
                    subtitle = stringResource(R.string.tool_blur_sub),
                    isActive = activeTab == StudioToolTab.BLUR,
                    onClick = {
                        onSelectTab(StudioToolTab.BLUR)
                        if (filters.blurRadiusDp < 0.5f) {
                            onBlurChange(5f)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_blur"),
                )
                StudioToolTile(
                    icon = Icons.Rounded.Brightness6,
                    title = stringResource(R.string.tool_brightness),
                    subtitle = stringResource(R.string.tool_brightness_sub),
                    isActive = activeTab == StudioToolTab.BRIGHTNESS,
                    onClick = {
                        onSelectTab(StudioToolTab.BRIGHTNESS)
                        if (filters.brightness == 1f) {
                            onBrightnessChange(1.18f)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_brightness"),
                )
                StudioToolTile(
                    icon = Icons.Rounded.RestartAlt,
                    title = stringResource(R.string.tool_reset),
                    subtitle = stringResource(R.string.tool_reset_sub),
                    isActive = false,
                    onClick = onResetFilters,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_reset"),
                )
            }

            // Contextual adjustment drawer for Filters / Color / Blur / Brightness
            AnimatedVisibility(visible = activeTab != StudioToolTab.TEXT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                        .padding(12.dp),
                ) {
                    when (activeTab) {
                        StudioToolTab.FILTERS -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterPreset.entries.forEach { preset ->
                                    val selected = filters.preset == preset
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onApplyPreset(preset) },
                                        label = {
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        modifier = Modifier.testTag("preset_${preset.name.lowercase(Locale.US)}"),
                                    )
                                }
                            }
                        }

                        StudioToolTab.COLOR -> {
                            GradeSliderRow(
                                label = "Contrast",
                                valueLabel = "${String.format(Locale.US, "%.2f", filters.contrast)}x",
                                value = filters.contrast,
                                valueRange = 0.5f..1.8f,
                                onValueChange = onContrastChange,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GradeSliderRow(
                                label = "Saturation",
                                valueLabel = "${String.format(Locale.US, "%.2f", filters.saturation)}x",
                                value = filters.saturation,
                                valueRange = 0f..2.0f,
                                onValueChange = onSaturationChange,
                            )
                        }

                        StudioToolTab.BLUR -> {
                            GradeSliderRow(
                                label = "Soft Blur",
                                valueLabel = "${String.format(Locale.US, "%.1f", filters.blurRadiusDp)} dp",
                                value = filters.blurRadiusDp,
                                valueRange = 0f..14f,
                                onValueChange = onBlurChange,
                            )
                        }

                        StudioToolTab.BRIGHTNESS -> {
                            GradeSliderRow(
                                label = "Brightness",
                                valueLabel = "${String.format(Locale.US, "%.2f", filters.brightness)}x",
                                value = filters.brightness,
                                valueRange = 0.5f..1.7f,
                                onValueChange = onBrightnessChange,
                            )
                        }

                        StudioToolTab.TEXT -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GradeSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
    }
}
