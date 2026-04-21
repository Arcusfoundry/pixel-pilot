package com.arcusfoundry.labs.pixelpilot.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.Animation
import com.arcusfoundry.labs.pixelpilot.render.SettingSpec
import com.arcusfoundry.labs.pixelpilot.theme.SystemThemeApplier
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel

/**
 * Unified per-source settings sheet. Always shows global Playback, Tint, and
 * System Integration sections. If the currently-selected source is a
 * procedural animation with a non-empty `settings` list, prepends an
 * animation-specific section at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneSettingsSheet(
    viewModel: WallpaperViewModel,
    animation: Animation?,
    context: Context,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Cap at 40% of screen height. Content scrolls internally past that.
                .fillMaxHeight(0.4f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val title = animation?.displayName?.let { "$it settings" } ?: "Settings"
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Animation-specific section (only for animations with declared settings).
            if (animation != null && animation.settings.isNotEmpty()) {
                SectionHeader("Scene")
                val currentValues = viewModel.sceneValues(animation)
                for (spec in animation.settings) {
                    when (spec) {
                        is SettingSpec.Text -> TextSettingRow(spec, currentValues) { k, v ->
                            viewModel.setSceneValue(animation.id, k, v)
                        }
                        is SettingSpec.IntRange -> IntRangeSettingRow(spec, currentValues) { k, v ->
                            viewModel.setSceneValue(animation.id, k, v)
                        }
                        is SettingSpec.Color -> ColorSettingRow(spec, currentValues) { k, v ->
                            viewModel.setSceneValue(animation.id, k, v)
                        }
                        is SettingSpec.Choice -> ChoiceSettingRow(spec, currentValues) { k, v ->
                            viewModel.setSceneValue(animation.id, k, v)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
            }

            SectionHeader("Playback")
            LabeledSlider(
                label = "Speed",
                value = viewModel.speed,
                onValueChange = viewModel::updateSpeed,
                valueRange = 0.05f..3f,
                displayValue = "%.2fx".format(viewModel.speed)
            )
            LabeledSlider(
                label = "Size",
                value = viewModel.scale,
                onValueChange = viewModel::updateScale,
                valueRange = 0.5f..3f,
                displayValue = "%.2fx".format(viewModel.scale)
            )
            LabeledSlider(
                label = "Dim",
                value = viewModel.dim,
                onValueChange = viewModel::updateDim,
                valueRange = 0f..0.9f,
                displayValue = "${(viewModel.dim * 100).toInt()}%"
            )
            Spacer(Modifier.height(16.dp))

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
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun TextSettingRow(
    spec: SettingSpec.Text,
    values: Map<String, Any?>,
    onValueChange: (String, Any) -> Unit
) {
    var text by remember(spec.key, values[spec.key]) {
        mutableStateOf((values[spec.key] as? String) ?: spec.default)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val clipped = raw.take(spec.maxLength)
            text = clipped
            onValueChange(spec.key, clipped)
        },
        label = { Text(spec.label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun IntRangeSettingRow(
    spec: SettingSpec.IntRange,
    values: Map<String, Any?>,
    onValueChange: (String, Any) -> Unit
) {
    val current = (values[spec.key] as? Int) ?: spec.default
    LabeledSlider(
        label = spec.label,
        value = current.toFloat(),
        onValueChange = { f -> onValueChange(spec.key, f.toInt()) },
        valueRange = spec.min.toFloat()..spec.max.toFloat(),
        steps = ((spec.max - spec.min) / spec.step - 1).coerceAtLeast(0),
        displayValue = current.toString()
    )
}

@Composable
private fun ColorSettingRow(
    spec: SettingSpec.Color,
    values: Map<String, Any?>,
    onValueChange: (String, Any) -> Unit
) {
    val current = (values[spec.key] as? Int) ?: spec.default
    Column {
        Text(
            spec.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        ColorWheel(
            initialColor = current,
            onColorChange = { c -> onValueChange(spec.key, c) }
        )
        HexColorInput(
            color = current,
            onColorChange = { c -> onValueChange(spec.key, c) }
        )
    }
}

@Composable
private fun ChoiceSettingRow(
    spec: SettingSpec.Choice,
    values: Map<String, Any?>,
    onValueChange: (String, Any) -> Unit
) {
    val current = (values[spec.key] as? String) ?: spec.default
    Column {
        Text(
            spec.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        for ((id, label) in spec.options) {
            androidx.compose.material3.TextButton(
                onClick = { onValueChange(spec.key, id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (id == current) "● $label" else "○ $label",
                    color = if (id == current) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
