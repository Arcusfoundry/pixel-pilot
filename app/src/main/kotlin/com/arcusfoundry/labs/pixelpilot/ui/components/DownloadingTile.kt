package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel.PendingDownload

@Composable
fun DownloadingTile(download: PendingDownload, modifier: Modifier = Modifier) {
    val isFailed = download.errorMessage != null
    Box(
        modifier = modifier
            .width(140.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
    ) {
        if (download.thumbnailUrl != null) {
            AsyncImage(
                model = download.thumbnailUrl,
                contentDescription = download.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Darken to keep progress text legible over busy thumbnails.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isFailed) "Failed" else "Downloading",
                style = MaterialTheme.typography.labelSmall,
                color = if (isFailed) MaterialTheme.colorScheme.error.copy(alpha = 0.95f)
                else Color.White.copy(alpha = 0.85f)
            )
            Column {
                if (isFailed) {
                    Text(
                        text = download.errorMessage ?: "Download failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 2
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(download.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}
