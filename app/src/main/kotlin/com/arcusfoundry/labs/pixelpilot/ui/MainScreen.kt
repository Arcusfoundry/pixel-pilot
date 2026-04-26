package com.arcusfoundry.labs.pixelpilot.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arcusfoundry.labs.pixelpilot.render.animations.AnimationRegistry
import com.arcusfoundry.labs.pixelpilot.source.WallpaperSource
import com.arcusfoundry.labs.pixelpilot.theme.SystemThemeApplier
import com.arcusfoundry.labs.pixelpilot.ui.components.AddCard
import com.arcusfoundry.labs.pixelpilot.ui.components.AnimationPicker
import com.arcusfoundry.labs.pixelpilot.ui.components.ColorWheel
import com.arcusfoundry.labs.pixelpilot.ui.components.HexColorInput
import com.arcusfoundry.labs.pixelpilot.ui.components.LabeledSlider
import com.arcusfoundry.labs.pixelpilot.ui.components.TintControls
import com.arcusfoundry.labs.pixelpilot.ui.components.SceneSettingsSheet
import com.arcusfoundry.labs.pixelpilot.ui.components.VideoCard
import com.arcusfoundry.labs.pixelpilot.ui.components.YouTubeDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WallpaperViewModel,
    onPickVideo: () -> Unit,
    onSetAsWallpaper: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val currentSource = viewModel.source

    var showYouTube by remember { mutableStateOf(false) }
    // Null with `settingsOpen == true` → sheet open for a non-animation source
    // (video). Non-null → animation-scoped sheet.
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsAnimation by remember {
        mutableStateOf<com.arcusfoundry.labs.pixelpilot.render.Animation?>(null)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // App window is translucent via Theme.PixelPilot (windowShowWallpaper=true),
            // so the actual live system wallpaper shows through the UI. No duplicate
            // renderer needed. If Pixel Pilot isn't the active wallpaper yet, whatever
            // IS active shows through — the activation banner prompts the user to fix.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                if (!viewModel.isPixelPilotActiveWallpaper) {
                    ActivationBanner(onActivate = onSetAsWallpaper)
                    Spacer(Modifier.height(12.dp))
                }
                viewModel.lastVideoError?.let { err ->
                    PlaybackErrorBanner(err)
                    Spacer(Modifier.height(12.dp))
                }
                val isVideoSource = currentSource is WallpaperSource.Video ||
                    currentSource is WallpaperSource.LocalFile
                if (isVideoSource) {
                    viewModel.lastVideoState?.let { state ->
                        if (!state.contains("READY")) {
                            PlaybackStateBanner(state)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                ) {
                    AnimationsPane(
                        viewModel = viewModel,
                        currentSource = currentSource,
                        selectedSource = viewModel.selectedTile,
                        onAddVideo = onPickVideo,
                        onAddYouTube = { showYouTube = true },
                        onApply = {
                            val applied = viewModel.applySelectedAsWallpaper()
                            // If Pixel Pilot isn't the system live wallpaper yet,
                            // hand off to the system picker so the user can
                            // confirm + pick home/lock/both.
                            if (applied && !viewModel.isPixelPilotActiveWallpaper) {
                                onSetAsWallpaper()
                            }
                        },
                        onOpenAnimationSettings = { animation ->
                            // Settings are per-tile and don't change the active
                            // wallpaper. Just sync the selection highlight.
                            viewModel.selectTile(WallpaperSource.Procedural(animation.id))
                            settingsAnimation = animation
                            settingsOpen = true
                        },
                        onOpenVideoSettings = { source ->
                            viewModel.selectTile(source)
                            settingsAnimation = null
                            settingsOpen = true
                        }
                    )
                    Spacer(Modifier.height(80.dp))
                    Footer()
                    Spacer(Modifier.height(24.dp))
                }
            }

            VersionStamp(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        }
    }

    if (showYouTube) {
        YouTubeDialog(
            downloadState = viewModel.downloadState,
            onDownload = { viewModel.downloadYouTube(it) },
            onDismiss = {
                showYouTube = false
                viewModel.clearDownloadState()
            }
        )
    }

    if (settingsOpen) {
        SceneSettingsSheet(
            viewModel = viewModel,
            animation = settingsAnimation,
            context = context,
            onDismiss = { settingsOpen = false }
        )
    }
}

@Composable
private fun AnimationsPane(
    viewModel: WallpaperViewModel,
    currentSource: WallpaperSource?,
    selectedSource: WallpaperSource?,
    onAddVideo: () -> Unit,
    onAddYouTube: () -> Unit,
    onApply: () -> Unit,
    onOpenAnimationSettings: (com.arcusfoundry.labs.pixelpilot.render.Animation) -> Unit,
    onOpenVideoSettings: (WallpaperSource) -> Unit
) {
    val userVideos = viewModel.recents.mapNotNull { WallpaperSource.parse(it) }

    Column {
        Text(
            text = "Your Videos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                AddCard(symbol = "+", label = "Add video", onClick = onAddVideo)
            }
            item {
                AddCard(symbol = "▶", label = "+ YouTube", onClick = onAddYouTube)
            }
            items(userVideos) { src ->
                val isSelected = selectedSource?.serialize() == src.serialize()
                val isActive = currentSource?.serialize() == src.serialize()
                VideoCard(
                    source = src,
                    selected = isSelected,
                    isActive = isActive,
                    onClick = { viewModel.selectTile(src) },
                    onApply = onApply,
                    onOpenSettings = { onOpenVideoSettings(src) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        AnimationPicker(
            animationsByCategory = AnimationRegistry.byCategory,
            selectedId = (selectedSource as? WallpaperSource.Procedural)?.animationId,
            activeId = (currentSource as? WallpaperSource.Procedural)?.animationId,
            onSelect = { viewModel.selectTile(WallpaperSource.Procedural(it.id)) },
            onApply = { onApply() },
            onOpenSettings = onOpenAnimationSettings
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
                    "When syncing system colors, also open the Themed Icons setting.",
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
            "Sets the picked color as the wallpaper color so Material You extracts from it.",
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
                    onSuccess = { "System colors synced." },
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
private fun PlaybackStateBanner(state: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                "Playback state",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlaybackErrorBanner(message: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                "Video playback error",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ActivationBanner(onActivate: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onActivate,
        color = MaterialTheme.colorScheme.primary,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                "Tap to activate Pixel Pilot",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Once it's your wallpaper, every selection applies instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun VersionStamp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val label = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION") info.versionCode.toLong()
            }
            "v${info.versionName} (${code})"
        } catch (_: Throwable) { "vUnknown" }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.45f),
        modifier = modifier
    )
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
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}
