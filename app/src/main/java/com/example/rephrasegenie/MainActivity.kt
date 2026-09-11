package com.example.rephrasegenie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.ui.navigation.RephraseGenieNavHost
import com.example.rephrasegenie.ui.navigation.Routes
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.RephraseGenieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RephraseGenieApp() }
    }
}

@Composable
private fun RephraseGenieApp(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RephraseGenieTheme(
        themeMode = state.settings.themeMode,
        accentHex = state.settings.accentColor,
    ) {
        if (state.loading) {
            // Painted in the theme background so there is no flash before the first screen.
            Box(Modifier.fillMaxSize().background(AppTheme.colors.background))
        } else {
            RephraseGenieNavHost(
                startDestination = if (state.hasIdentity) Routes.HOME else Routes.SETUP,
            )
        }
    }
}
