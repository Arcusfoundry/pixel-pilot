package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun HexColorInput(
    color: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Re-sync on external color changes (wheel / preset), but let the user edit freely
    // in between. remember key = external color means partial-typed state survives
    // recompositions until the parent commits a new color.
    var text by remember(color) { mutableStateOf(formatHex(color)) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(color))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                )
        )
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val sanitized = raw.trim()
                    .removePrefix("#")
                    .removePrefix("0x")
                    .removePrefix("0X")
                    .uppercase()
                    .filter { it in '0'..'9' || it in 'A'..'F' }
                    .take(6)
                text = sanitized
                if (sanitized.length == 6) {
                    val rgb = sanitized.toLong(16).toInt()
                    val argb = (0xFF shl 24) or (rgb and 0xFFFFFF)
                    if (argb != color) onColorChange(argb)
                }
            },
            singleLine = true,
            label = { Text("Hex") },
            prefix = { Text("#") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatHex(color: Int): String = "%06X".format(color and 0xFFFFFF)
