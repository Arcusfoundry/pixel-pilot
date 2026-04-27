package com.arcusfoundry.labs.pixelpilot.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
    onActivateWallpaper: () -> Unit
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
        // Tap-outside-to-close: when settings dropdown is open, taps anywhere
        // not consumed by a child (tile click, slider, etc.) dismiss it. The
        // clickable is conditional so it doesn't intercept ripples elsewhere.
        val outerModifier = if (settingsOpen) {
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { settingsOpen = false }
        } else {
            Modifier.fillMaxSize()
        }
        Box(modifier = outerModifier) {
            // App window is translucent via Theme.PixelPilot (windowShowWallpaper=true),
            // so the actual live system wallpaper shows through the UI. No duplicate
            // renderer needed.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                ) {
                    AnimationsPane(
                        viewModel = viewModel,
                        currentSource = currentSource,
                        settingsOpen = settingsOpen,
                        settingsAnimation = settingsAnimation,
                        settingsSource = if (settingsAnimation == null) currentSource else null,
                        onAddVideo = onPickVideo,
                        onAddYouTube = { showYouTube = true },
                        onActivateWallpaper = onActivateWallpaper,
                        onOpenAnimationSettings = { animation ->
                            val cur = (currentSource as? WallpaperSource.Procedural)?.animationId
                            if (cur != animation.id) {
                                viewModel.selectSource(WallpaperSource.Procedural(animation.id))
                            }
                            settingsAnimation = animation
                            settingsOpen = true
                        },
                        onOpenVideoSettings = { source ->
                            if (currentSource?.serialize() != source.serialize()) {
                                viewModel.selectSource(source)
                            }
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
            // Bottom-right placement avoids overlap with Pixel's centered
            // top camera cutout. Drawn last so it sits on top of the content
            // Column.
            ShuffleButton(
                enabled = viewModel.shuffleEnabled,
                onToggle = { viewModel.toggleShuffle() },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(bottom = 18.dp, end = 18.dp)
            )
            // Full-screen nag overlay. Shown whenever Pixel Pilot is not the
            // active live wallpaper. Replaces tile-tap dialogs and the
            // smaller top button — single, prominent activation prompt that
            // dismisses on its own once the user sets the wallpaper.
            if (!viewModel.isPixelPilotActiveWallpaper) {
                ActivationNag(onActivate = onActivateWallpaper)
            }
        }
    }

    if (showYouTube) {
        YouTubeDialog(
            onSubmit = { viewModel.startBackgroundDownload(it) },
            onDismiss = { showYouTube = false }
        )
    }

    // Note: tile-scoped settings now live inline in the AnimationsPane as a
    // dropdown anchored under the row containing the active tile. The
    // SceneSettingsSheet is no longer used for that flow.
}

@Composable
private fun AnimationsPane(
    viewModel: WallpaperViewModel,
    currentSource: WallpaperSource?,
    settingsOpen: Boolean,
    settingsAnimation: com.arcusfoundry.labs.pixelpilot.render.Animation?,
    settingsSource: WallpaperSource?,
    onAddVideo: () -> Unit,
    onAddYouTube: () -> Unit,
    onActivateWallpaper: () -> Unit,
    onOpenAnimationSettings: (com.arcusfoundry.labs.pixelpilot.render.Animation) -> Unit,
    onOpenVideoSettings: (WallpaperSource) -> Unit
) {
    val userVideos = viewModel.recents.mapNotNull { WallpaperSource.parse(it) }

    // Tile click writes prefs.source. The "Set as wallpaper" button at the
    // top of the screen handles activation when Pixel Pilot isn't yet the
    // live wallpaper — tile taps stay quiet and don't surprise-launch any
    // system dialogs.
    val applySource: (WallpaperSource) -> Unit = { src ->
        viewModel.selectSource(src)
    }

    Column {
        Text(
            text = "Your Videos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        val videosState = androidx.compose.foundation.lazy.rememberLazyListState()
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = videosState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AddCard(symbol = "+", label = "Add video", onClick = onAddVideo)
                }
                item {
                    AddCard(symbol = "▶", label = "+ YouTube", onClick = onAddYouTube)
                }
                items(viewModel.pendingDownloads, key = { it.id }) { download ->
                    com.arcusfoundry.labs.pixelpilot.ui.components.DownloadingTile(download = download)
                }
                items(userVideos) { src ->
                    val selected = currentSource?.serialize() == src.serialize()
                    VideoCard(
                        source = src,
                        selected = selected,
                        isFavorite = viewModel.isFavorite(src),
                        onClick = { applySource(src) },
                        onToggleFavorite = { viewModel.toggleFavorite(src) },
                        onOpenSettings = { onOpenVideoSettings(src) }
                    )
                }
            }
            com.arcusfoundry.labs.pixelpilot.ui.components.ScrollHintArrow(
                state = videosState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
            )
        }
        // Inline settings dropdown for the active video tile.
        if (settingsOpen && settingsSource != null && settingsSource !is WallpaperSource.Procedural) {
            val activeVideoIndex = userVideos.indexOfFirst {
                it.serialize() == settingsSource.serialize()
            }
            if (activeVideoIndex >= 0) {
                // Tile index inside the row = 2 (Add video, +YouTube) + pending + index
                val rowIndex = 2 + viewModel.pendingDownloads.size + activeVideoIndex
                val pointerOffset = computePointerOffsetDp(videosState, rowIndex)
                com.arcusfoundry.labs.pixelpilot.ui.components.SettingsDropdown(
                    viewModel = viewModel,
                    animation = null,
                    source = settingsSource,
                    pointerHorizontalOffset = pointerOffset
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (viewModel.recommendedVideos.isNotEmpty()) {
            Text(
                text = "Recommended",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                val recState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyRow(
                    state = recState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.recommendedVideos, key = { it.videoId }) { video ->
                        com.arcusfoundry.labs.pixelpilot.ui.components.RecommendedVideoCard(
                            video = video,
                            onDownload = { viewModel.startBackgroundDownload(video.youtubeUrl) }
                        )
                    }
                }
                com.arcusfoundry.labs.pixelpilot.ui.components.ScrollHintArrow(
                    state = recState,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        AnimationPicker(
            animationsByCategory = AnimationRegistry.byCategory,
            selectedId = (currentSource as? WallpaperSource.Procedural)?.animationId,
            isFavorite = { animId ->
                viewModel.isFavorite(WallpaperSource.Procedural(animId))
            },
            onSelect = { applySource(WallpaperSource.Procedural(it.id)) },
            onToggleFavorite = { animation ->
                viewModel.toggleFavorite(WallpaperSource.Procedural(animation.id))
            },
            onOpenSettings = onOpenAnimationSettings,
            settingsAnimation = if (settingsOpen) settingsAnimation else null,
            settingsContent = { rowState, indexInRow ->
                val pointerOffset = computePointerOffsetDp(rowState, indexInRow)
                com.arcusfoundry.labs.pixelpilot.ui.components.SettingsDropdown(
                    viewModel = viewModel,
                    animation = settingsAnimation,
                    source = null,
                    pointerHorizontalOffset = pointerOffset
                )
            }
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

/**
 * Compute the on-screen X offset (in dp) of the center of [targetIndex] inside
 * [state]'s LazyRow, suitable for positioning a chevron pointer above it. If
 * the target tile isn't currently visible, falls back to the viewport center.
 */
@Composable
private fun computePointerOffsetDp(
    state: androidx.compose.foundation.lazy.LazyListState,
    targetIndex: Int
): androidx.compose.ui.unit.Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val info = state.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
    val centerPx = if (info != null) {
        info.offset + info.size / 2
    } else {
        state.layoutInfo.viewportSize.width / 2
    }
    return with(density) { centerPx.toDp() } - 6.dp
}

@Composable
private fun ActivationNag(onActivate: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            // Consume taps so the user can't dismiss without activating —
            // this is the nag, after all.
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* swallow */ },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Pixel Pilot",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Set Pixel Pilot as your live wallpaper to begin. " +
                    "For the best experience, choose Both — home and lock screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = onActivate,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Set as wallpaper",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ShuffleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    val fg = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onToggle),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Two crossed arrows, ASCII-stable across fonts. "↑↓" reads as
        // shuffle/randomize; the unicode "⤭" used previously didn't render
        // on default Pixel/GrapheneOS fonts so the button looked empty.
        Text(
            text = "⇄",
            style = MaterialTheme.typography.titleLarge,
            color = fg,
            fontWeight = FontWeight.Bold
        )
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
