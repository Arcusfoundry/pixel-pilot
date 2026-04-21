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
import androidx.compose.material3.Switch
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
import com.arcusfoundry.labs.pixelpilot.ui.components.ColorWheel
import com.arcusfoundry.labs.pixelpilot.ui.components.HexColorInput
import com.arcusfoundry.labs.pixelpilot.ui.components.LabeledSlider
import com.arcusfoundry.labs.pixelpilot.ui.components.MediaSection
import com.arcusfoundry.labs.pixelpilot.ui.components.TintControls
import com.arcusfoundry.labs.pixelpilot.ui.components.WallpaperPreviewSurface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            WallpaperPreviewSurface(
                source = currentSource,
                params = viewModel.renderParams(),
                modifier = Modifier.fillMaxSize()
            )
            // Scrim so UI text stays readable over bright animations.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.72f),
                            0.45f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Black.copy(alpha = 0.65f)
                        )
                    )
            )
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
        Text(
            "Playback",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
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

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        SystemIntegrationSection(viewModel, context)
    }
}

@Composable
private fun SystemIntegrationSection(viewModel: WallpaperViewModel, context: Context) {
    Column {
        Text(
            "System integration",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        // Themed icons row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Sync themed icons too",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "When syncing system colors, also open the Themed Icons setting so you can enable it alongside the new accent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = viewModel.syncThemedIcons,
                onCheckedChange = viewModel::updateSyncThemedIcons
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Sync system colors",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Sets the picked color as the wallpaper so Material You extracts from it. Replaces the live wallpaper — re-apply it after.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        ColorWheel(
            initialColor = viewModel.tintColor,
            onColorChange = viewModel::updateTintColor
        )
        Spacer(Modifier.height(4.dp))
        HexColorInput(
            color = viewModel.tintColor,
            onColorChange = viewModel::updateTintColor
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = {
                val result = SystemThemeApplier.applyThemeColor(context, viewModel.tintColor)
                val baseMsg = result.fold(
                    onSuccess = { "System colors synced. Re-apply live wallpaper when ready." },
                    onFailure = { "Failed: ${it.message}" }
                )
                Toast.makeText(context, baseMsg, Toast.LENGTH_LONG).show()
                if (result.isSuccess && viewModel.syncThemedIcons) {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sync system colors")
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
