package com.example.rephrasegenie.ui.screen.home

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.overlay.AccessibilityStatus
import com.example.rephrasegenie.ui.components.AppScaffold
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.IconActionButton

/**
 * Reports on the bubble and switches it on and off.
 *
 * There is no text box here on purpose: the bubble is the product, and a copy of it inside the app
 * would be a second thing to keep working that nobody uses once the overlay is running.
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-checked whenever the app comes back, so returning from system settings updates it.
    val lifecycleOwner = LocalLifecycleOwner.current
    var serviceEnabled by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceEnabled = AccessibilityStatus.isServiceEnabled(context)
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppScaffold(
        title = "RephraseGenie",
        actions = {
            IconActionButton(
                icon = Icons.Outlined.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FieldHint("Rewrite your text in any tone, anywhere.")

            BubbleCard(
                serviceEnabled = serviceEnabled,
                overlayGranted = overlayGranted,
                bubbleEnabled = state.settings.bubbleEnabled,
                onBubbleEnabledChange = viewModel::setBubbleEnabled,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
