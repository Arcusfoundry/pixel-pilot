package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel

@Composable
fun MediaSection(
    recents: List<String>,
    downloadState: WallpaperViewModel.DownloadState,
    currentSource: WallpaperSource?,
    onPickVideo: () -> Unit,
    onDownloadYouTube: (String) -> Unit,
    onSelectRecent: (WallpaperSource) -> Unit,
    modifier: Modifier = Modifier
) {
    var ytUrl by remember { mutableStateOf("") }

    Column(modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPickVideo, modifier = Modifier.weight(1f)) {
                Text("Pick video file")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "YouTube URL",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedTextField(
            value = ytUrl,
            onValueChange = { ytUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://youtube.com/watch?v=...") }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onDownloadYouTube(ytUrl) },
                enabled = ytUrl.isNotBlank() &&
                    downloadState !is WallpaperViewModel.DownloadState.Running
            ) {
                Text("Download to local")
            }
            if (downloadState is WallpaperViewModel.DownloadState.Failed) {
                OutlinedButton(onClick = { ytUrl = "" }) { Text("Clear") }
            }
        }
        when (val ds = downloadState) {
            is WallpaperViewModel.DownloadState.Running -> {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ds.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Downloading ${(ds.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is WallpaperViewModel.DownloadState.Failed -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Download failed: ${ds.reason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is WallpaperViewModel.DownloadState.Done -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Downloaded. Wallpaper updated.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {}
        }
        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Recent",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            recents.forEach { serialized ->
                val src = WallpaperSource.parse(serialized) ?: return@forEach
                val label = when (src) {
                    is WallpaperSource.Video -> src.uri.substringAfterLast('/').take(40)
                    is WallpaperSource.LocalFile -> src.path.substringAfterLast('/').take(40)
                    is WallpaperSource.Procedural -> src.animationId
                }
                val isCurrent = currentSource?.serialize() == serialized
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { onSelectRecent(src) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (src is WallpaperSource.LocalFile) "📁" else "🎬",
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCurrent) {
                        Text(
                            "current",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
