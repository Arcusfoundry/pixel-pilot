package com.arcusfoundry.labs.pixelpilot.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.theme.SystemThemeApplier
import com.arcusfoundry.labs.pixelpilot.ui.components.AnimationPicker
import com.arcusfoundry.labs.pixelpilot.ui.components.LabeledSlider
import com.arcusfoundry.labs.pixelpilot.ui.components.MediaSection
import com.arcusfoundry.labs.pixelpilot.ui.components.TintControls

private enum class Tab(val label: String) {
    Animations("Animations"),
    Media("Media"),
    Customize("Customize")
}

@Composable
fun MainScreen(
    viewModel: WallpaperViewModel,
    onPickVideo: () -> Unit,
    onSetAsWallpaper: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var selectedTab by remember { mutableStateOf(Tab.Animations) }
    val currentSource = viewModel.source
    val currentLabel = currentSourceLabel(currentSource)

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            Header(currentLabel)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSetAsWallpaper,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Set as Wallpaper") }
            Spacer(Modifier.height(16.dp))

            TabBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(bottom = 24.dp)
            ) {
                when (selectedTab) {
                    Tab.Animations -> AnimationsPane(viewModel, currentSource)
                    Tab.Media -> MediaPane(viewModel, currentSource, onPickVideo)
                    Tab.Customize -> CustomizePane(viewModel, context)
                }
                Spacer(Modifier.height(24.dp))
                Footer()
            }
        }
    }
}

@Composable
private fun TabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        Tab.entries.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = Tab.entries.size),
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun AnimationsPane(
    viewModel: WallpaperViewModel,
    currentSource: WallpaperSource?
) {
    AnimationPicker(
        animationsByCategory = AnimationRegistry.byCategory,
        selectedId = (currentSource as? WallpaperSource.Procedural)?.animationId,
        onSelect = { viewModel.selectSource(WallpaperSource.Procedural(it.id)) }
    )
}

@Composable
private fun MediaPane(
    viewModel: WallpaperViewModel,
    currentSource: WallpaperSource?,
    onPickVideo: () -> Unit
) {
    Column {
        Text(
            "Add a video from your device or download one from YouTube. Downloaded videos live locally. No streaming after the initial download.",
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
    }
}

@Composable
private fun CustomizePane(viewModel: WallpaperViewModel, context: Context) {
    Column {
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

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text(
            "System theme color",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Drives Material You extraction. Replaces the live wallpaper temporarily — you'll need to re-apply it afterward.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        OutlinedButton(
            onClick = {
                val result = SystemThemeApplier.applyThemeColor(context, viewModel.tintColor)
                val msg = result.fold(
                    onSuccess = { "System theme color applied. Re-apply live wallpaper when ready." },
                    onFailure = { "Failed: ${it.message}" }
                )
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            },
            enabled = viewModel.tintKind == "static",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (viewModel.tintKind == "static") "Apply to System Theme"
                else "Pick a static tint color first"
            )
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
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
private fun Footer() {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
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
