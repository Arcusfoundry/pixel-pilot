package com.arcusfoundry.labs.pixelpilot

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcusfoundry.labs.pixelpilot.render.AssetLoader
import com.arcusfoundry.labs.pixelpilot.ui.MainScreen
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel
import com.arcusfoundry.labs.pixelpilot.ui.theme.PixelPilotTheme
import com.arcusfoundry.labs.pixelpilot.wallpaper.VideoWallpaperService

class MainActivity : ComponentActivity() {

    private lateinit var vm: WallpaperViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AssetLoader.initialize(this)
        vm = ViewModelProvider(this)[WallpaperViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            PixelPilotTheme {
                val context = this

                val videoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let { vm.persistPickedVideo(context, it.toString()) }
                }

                MainScreen(
                    viewModel = vm,
                    onPickVideo = {
                        videoPicker.launch(arrayOf("video/*"))
                    },
                    onSetAsWallpaper = {
                        launchWallpaperPicker()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check whether Pixel Pilot is the active wallpaper on return from picker.
        if (::vm.isInitialized) vm.refreshActiveWallpaperStatus()
    }

    private fun launchWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@MainActivity, VideoWallpaperService::class.java)
            )
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: some OEMs don't expose this intent. Open the generic picker.
            runCatching { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) }
        }
    }
}
