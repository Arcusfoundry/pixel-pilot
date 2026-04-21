package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneSettingsSheet(
    animation: Animation,
    currentValues: Map<String, Any?>,
    onValueChange: (String, Any) -> Unit,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "${animation.displayName} settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            for (spec in animation.settings) {
                when (spec) {
                    is SettingSpec.Text -> TextSettingRow(spec, currentValues, onValueChange)
                    is SettingSpec.IntRange -> IntRangeSettingRow(spec, currentValues, onValueChange)
                    is SettingSpec.Color -> ColorSettingRow(spec, currentValues, onValueChange)
                    is SettingSpec.Choice -> ChoiceSettingRow(spec, currentValues, onValueChange)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
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
        // Simple fallback: list each option as a tappable row. FilterChip row
        // could replace this later if the options count grows.
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
