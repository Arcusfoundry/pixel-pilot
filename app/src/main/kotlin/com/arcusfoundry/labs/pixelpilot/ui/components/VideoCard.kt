package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource

@Composable
fun VideoCard(
    source: WallpaperSource,
    selected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val path: String = when (source) {
        is WallpaperSource.Video -> source.uri
        is WallpaperSource.LocalFile -> source.path
        is WallpaperSource.Procedural -> return
    }
    val label = deriveLabel(source)
    val thumbnail = VideoThumbnailCache.thumbnailFor(path)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .border(2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f)
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
                FavoriteStarBadge(
                    favorited = isFavorite,
                    onClick = onToggleFavorite
                )
                if (selected) VideoGearButton(onClick = onOpenSettings)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FavoriteStarBadge(favorited: Boolean, onClick: () -> Unit) {
    val bg = if (favorited) MaterialTheme.colorScheme.primary
    else Color.Black.copy(alpha = 0.55f)
    val fg = if (favorited) MaterialTheme.colorScheme.onPrimary
    else Color.White.copy(alpha = 0.9f)
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = if (favorited) "★" else "☆",
            style = MaterialTheme.typography.titleMedium,
            color = fg
        )
    }
}

@Composable
private fun VideoGearButton(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClick() },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "⚙",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

private fun deriveLabel(source: WallpaperSource): String = when (source) {
    is WallpaperSource.Video -> {
        val tail = source.uri.substringAfterLast('/')
        if (tail.contains('%')) "Video" else tail.take(18)
    }
    is WallpaperSource.LocalFile -> {
        val tail = source.path.substringAfterLast('/')
        // YouTube downloads use synthetic names like "pp_12345_67890.mp4" —
        // shorten to just the timestamp portion so each is distinguishable.
        if (tail.startsWith("pp_")) {
            val parts = tail.removeSuffix(".mp4").split('_')
            val ts = parts.lastOrNull()?.toLongOrNull()
            if (ts != null) {
                val date = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ts))
                "YT · $date"
            } else tail.take(18)
        } else tail.take(18)
    }
    is WallpaperSource.Procedural -> source.animationId
}
