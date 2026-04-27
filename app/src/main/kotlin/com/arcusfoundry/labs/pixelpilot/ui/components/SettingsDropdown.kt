package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.SettingSpec
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel

/**
 * Compact inline settings panel that anchors below the row containing the
 * selected tile. An upward chevron indicates which tile is being edited.
 * Sliders sit two-per-row to keep vertical footprint short.
 *
 * Replaces the full-screen ModalBottomSheet for tile-scoped tweaks. The user
 * dismisses by tapping anywhere outside the dropdown (handled by the parent).
 */
@Composable
fun SettingsDropdown(
    viewModel: WallpaperViewModel,
    animation: Animation?,
    source: WallpaperSource?,
    pointerHorizontalOffset: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        // Upward chevron pointer. Positioned via offset to align with the
        // selected tile's center.
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            Text(
                text = "▲",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = pointerHorizontalOffset)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    RoundedCornerShape(12.dp)
                )
                // Stop clicks inside the dropdown from reaching the parent's
                // dismiss handler.
                .clickable(
                    interactionSource = androidx.compose.runtime.remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                    indication = null
                ) { /* consume */ }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val title = animation?.displayName ?: "Settings"
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            // Two compact sliders per row.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactLabeledSlider(
                    label = "Speed",
                    value = viewModel.speed,
                    onValueChange = viewModel::updateSpeed,
                    valueRange = 0.05f..3f,
                    displayValue = "%.2fx".format(viewModel.speed),
                    modifier = Modifier.weight(1f)
                )
                CompactLabeledSlider(
                    label = "Size",
                    value = viewModel.scale,
                    onValueChange = viewModel::updateScale,
                    valueRange = 0.5f..3f,
                    displayValue = "%.2fx".format(viewModel.scale),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactLabeledSlider(
                    label = "Dim",
                    value = viewModel.dim,
                    onValueChange = viewModel::updateDim,
                    valueRange = 0f..0.9f,
                    displayValue = "${(viewModel.dim * 100).toInt()}%",
                    modifier = Modifier.weight(1f)
                )
                CompactLabeledSlider(
                    label = "Tint strength",
                    value = viewModel.tintStrength,
                    onValueChange = viewModel::updateTintStrength,
                    valueRange = 0f..1f,
                    displayValue = "${(viewModel.tintStrength * 100).toInt()}%",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))

            TintControls(
                tintKind = viewModel.tintKind,
                tintColor = viewModel.tintColor,
                rainbowCycleSeconds = viewModel.rainbowCycleSeconds,
                tintStrength = viewModel.tintStrength,
                onKindChange = viewModel::updateTintKind,
                onColorChange = viewModel::updateTintColor,
                onRainbowCycleChange = viewModel::updateRainbowCycle,
                onStrengthChange = viewModel::updateTintStrength
            )

            // Animation-specific scene settings.
            if (animation != null && animation.settings.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    "Scene",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                val currentValues = viewModel.sceneValues(animation)
                for (spec in animation.settings) {
                    SceneSettingRow(spec, currentValues) { k, v ->
                        viewModel.setSceneValue(animation.id, k, v)
                    }
                }
            }

            // Video-only: remove + delete file.
            if (animation == null && source != null && source !is WallpaperSource.Procedural) {
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { viewModel.removeVideoSource(source) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (source is WallpaperSource.LocalFile) "Delete download"
                        else "Remove from list",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SceneSettingRow(
    spec: SettingSpec,
    values: Map<String, Any?>,
    onChange: (String, Any) -> Unit
) {
    when (spec) {
        is SettingSpec.Text -> {
            var text by androidx.compose.runtime.remember(spec.key, values[spec.key]) {
                androidx.compose.runtime.mutableStateOf(
                    (values[spec.key] as? String) ?: spec.default
                )
            }
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { raw ->
                    val clipped = raw.take(spec.maxLength)
                    text = clipped
                    onChange(spec.key, clipped)
                },
                label = { Text(spec.label, style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
        is SettingSpec.IntRange -> {
            val current = (values[spec.key] as? Int) ?: spec.default
            CompactLabeledSlider(
                label = spec.label,
                value = current.toFloat(),
                onValueChange = { onChange(spec.key, it.toInt()) },
                valueRange = spec.min.toFloat()..spec.max.toFloat(),
                steps = (spec.max - spec.min - 1).coerceAtLeast(0),
                displayValue = current.toString()
            )
        }
        is SettingSpec.Color -> {
            // Heavy color UI is too tall for the dropdown — use a compact swatch
            // row instead. Picker available via the global tint controls above.
        }
        is SettingSpec.Choice -> {
            val current = (values[spec.key] as? String) ?: spec.default
            Spacer(Modifier.height(4.dp))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (option in spec.options) {
                    val isSelected = option == current
                    OutlinedButton(
                        onClick = { onChange(spec.key, option) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
