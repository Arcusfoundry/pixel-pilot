package com.arcusfoundry.labs.pixelpilot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel

@Composable
fun YouTubeDialog(
    downloadState: WallpaperViewModel.DownloadState,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    val running = downloadState is WallpaperViewModel.DownloadState.Running

    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("Add YouTube video") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !running,
                    placeholder = { Text("https://youtube.com/watch?v=...") }
                )
                Spacer(Modifier.height(8.dp))
                when (val ds = downloadState) {
                    is WallpaperViewModel.DownloadState.Running -> {
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
                    is WallpaperViewModel.DownloadState.Failed -> Text(
                        "Download failed: ${ds.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    is WallpaperViewModel.DownloadState.Done -> Text(
                        "Done. Video added.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    else -> Text(
                        "The video downloads to local storage. After the first download, no further network is used.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDownload(url) },
                enabled = url.isNotBlank() && !running
            ) { Text("Download") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !running
            ) { Text("Close") }
        }
    )
}
