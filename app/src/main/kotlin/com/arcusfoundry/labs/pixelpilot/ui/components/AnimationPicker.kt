package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.Animation

@Composable
fun AnimationPicker(
    animationsByCategory: Map<String, List<Animation>>,
    selectedId: String?,
    onSelect: (Animation) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        val categoryOrder = listOf("Default", "Abstract", "Tech", "Nature", "Energy", "Classics")
        val sorted = categoryOrder.mapNotNull { cat -> animationsByCategory[cat]?.let { cat to it } } +
            animationsByCategory.filterKeys { it !in categoryOrder }.toList()

        sorted.forEach { (category, animations) ->
            Text(
                text = category,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(animations) { animation ->
                    AnimationCard(
                        animation = animation,
                        selected = selectedId == animation.id,
                        onClick = { onSelect(animation) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AnimationCard(
    animation: Animation,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fallback = Color(animation.defaultBackground).copy(alpha = 1f)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fallback)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        LiveAnimationThumbnail(
            animation = animation,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.75f)
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Top
            ) {
                Text(
                    text = animation.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (selected) AppliedPill()
            }
            Text(
                text = animation.displayName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/** Only rendered on the currently-applied card. Absence on others = less clutter. */
@Composable
fun AppliedPill() {
    val bg = Color(0xFFC3D95E)
    val onBg = Color(0xFF23270F)
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "APPLIED",
            style = MaterialTheme.typography.labelSmall,
            color = onBg,
            fontWeight = FontWeight.Bold
        )
    }
}
