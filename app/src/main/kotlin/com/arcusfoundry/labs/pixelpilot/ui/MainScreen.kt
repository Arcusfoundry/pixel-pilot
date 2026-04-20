package com.arcusfoundry.labs.pixelpilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.ui.components.AnimationPicker
import com.arcusfoundry.labs.pixelpilot.ui.components.LabeledSlider
import com.arcusfoundry.labs.pixelpilot.ui.components.MediaSection
import com.arcusfoundry.labs.pixelpilot.ui.components.TintControls

@Composable
fun MainScreen(
    viewModel: WallpaperViewModel,
    onPickVideo: () -> Unit,
    onSetAsWallpaper: () -> Unit
) {
    val scroll = rememberScrollState()
    val currentSource = viewModel.source
    val currentLabel = currentSourceLabel(currentSource)

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp)
        ) {
            Header(currentLabel)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSetAsWallpaper,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Set as Wallpaper") }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Animations")
            Spacer(Modifier.height(4.dp))
            AnimationPicker(
                animationsByCategory = AnimationRegistry.byCategory,
                selectedId = (currentSource as? WallpaperSource.Procedural)?.animationId,
                onSelect = { viewModel.selectSource(WallpaperSource.Procedural(it.id)) }
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("Your Media")
            Spacer(Modifier.height(4.dp))
            Text(
                "Add a video from your device or download one from YouTube. Downloaded videos live locally on your phone. No streaming after the initial download.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            MediaSection(
                recents = viewModel.recents,
                downloadState = viewModel.downloadState,
                currentSource = currentSource,
                onPickVideo = onPickVideo,
                onDownloadYouTube = viewModel::downloadYouTube,
                onSelectRecent = viewModel::selectSource
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SectionTitle("Customize")
            Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(32.dp))
            Footer()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Header(currentLabel: String) {
    Column {
        Text(
            "Pixel Pilot",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Customization without compromise.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        currentLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun Footer() {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Arcus Foundry Labs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Built because nothing better existed. Provided as-is.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic
        )
    }
}

private fun currentSourceLabel(source: WallpaperSource?): String = when (source) {
    null -> "Nothing set"
    is WallpaperSource.Procedural -> AnimationRegistry.get(source.animationId)?.displayName ?: source.animationId
    is WallpaperSource.Video -> source.uri.substringAfterLast('/').take(50)
    is WallpaperSource.LocalFile -> source.path.substringAfterLast('/').take(50)
}
