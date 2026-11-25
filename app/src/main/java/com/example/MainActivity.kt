package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.ui.screens.GalleryMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val galleryViewModel: GalleryViewModel = viewModel()
      val settings by galleryViewModel.settings.collectAsStateWithLifecycle()
      val isDarkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      MyApplicationTheme(
        darkTheme = isDarkTheme,
        dynamicColor = settings.dynamicColor
      ) {
        Surface(modifier = Modifier.fillMaxSize()) {
          GalleryMainScreen(viewModel = galleryViewModel)
        }
      }
    }
  }
}

