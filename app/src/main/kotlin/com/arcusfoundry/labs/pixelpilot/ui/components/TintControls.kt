package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TintControls(
    tintKind: String,
    tintColor: Int,
    rainbowCycleSeconds: Float,
    tintStrength: Float,
    onKindChange: (String) -> Unit,
    onColorChange: (Int) -> Unit,
    onRainbowCycleChange: (Float) -> Unit,
    onStrengthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = "Color Tint",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("none" to "None", "static" to "Static", "rainbow" to "Rainbow").forEach { (key, label) ->
                FilterChip(
                    selected = tintKind == key,
                    onClick = { onKindChange(key) },
                    label = { Text(label) }
                )
            }
            if (tintKind == "static") {
                Spacer(Modifier.padding(start = 8.dp))
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(tintColor))
                )
            }
        }
        AnimatedVisibility(visible = tintKind == "static") {
            Column {
                Spacer(Modifier.height(8.dp))
                ColorWheel(
                    initialColor = tintColor,
                    onColorChange = onColorChange
                )
                LabeledSlider(
                    label = "Intensity",
                    value = tintStrength,
                    onValueChange = onStrengthChange,
                    valueRange = 0f..1f,
                    displayValue = "${(tintStrength * 100).toInt()}%"
                )
            }
        }
        AnimatedVisibility(visible = tintKind == "rainbow") {
            Column {
                Spacer(Modifier.height(8.dp))
                LabeledSlider(
                    label = "Cycle time",
                    value = rainbowCycleSeconds,
                    onValueChange = onRainbowCycleChange,
                    valueRange = 5f..120f,
                    displayValue = "${rainbowCycleSeconds.toInt()}s"
                )
                LabeledSlider(
                    label = "Intensity",
                    value = tintStrength,
                    onValueChange = onStrengthChange,
                    valueRange = 0f..1f,
                    displayValue = "${(tintStrength * 100).toInt()}%"
                )
            }
        }
    }
}
