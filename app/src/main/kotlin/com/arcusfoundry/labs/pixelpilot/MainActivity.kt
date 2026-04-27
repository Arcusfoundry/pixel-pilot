package com.arcusfoundry.labs.pixelpilot

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.arcusfoundry.labs.pixelpilot.render.AssetLoader
import com.arcusfoundry.labs.pixelpilot.ui.MainScreen
import com.arcusfoundry.labs.pixelpilot.ui.WallpaperViewModel
import com.arcusfoundry.labs.pixelpilot.ui.theme.PixelPilotTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: WallpaperViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask the window manager to composite the live wallpaper behind our
        // activity (bypassing the launcher's icons/widgets). Requires theme
        // to have windowIsTranslucent+transparent background, which it does.
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
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
}
