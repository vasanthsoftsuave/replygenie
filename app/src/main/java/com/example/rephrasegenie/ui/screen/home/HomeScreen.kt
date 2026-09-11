package com.example.rephrasegenie.ui.screen.home

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.overlay.AccessibilityStatus
import com.example.rephrasegenie.ui.components.AppTextField
import com.example.rephrasegenie.ui.components.ErrorCard
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.FieldLabel
import com.example.rephrasegenie.ui.components.PrimaryButton
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.components.ToneChip
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.parseHexColor

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "RephraseGenie",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            SecondaryButton(text = "Settings", onClick = onOpenSettings)
        }

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

        PermissionCard(serviceEnabled = serviceEnabled, overlayGranted = overlayGranted)

        // Try it here without leaving the app. This is how the feature gets tested before the
        // overlay permissions are granted.
        SectionCard {
            SectionTitle("Try it")

            FieldLabel("Your text")
            AppTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChange,
                placeholder = "hey did u finish the slides yet",
                singleLine = false,
                minLines = 3,
                enabled = !state.isWorking,
            )

            Spacer(Modifier.height(4.dp))
            FieldLabel("Tone")

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.tones, key = { it.id }) { tone ->
                    ToneChip(
                        name = tone.name,
                        color = parseHexColor(tone.color),
                        selected = tone.id == state.selectedToneId,
                        onClick = { viewModel.onToneSelected(tone.id) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (state.isWorking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent,
                    )
                    Text(
                        text = state.stageLabel.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    SecondaryButton(text = "Cancel", onClick = viewModel::cancel)
                }
            } else {
                PrimaryButton(
                    text = "Rephrase",
                    onClick = viewModel::rephrase,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canRephrase,
                )
                if (state.inputText.isBlank()) {
                    FieldHint("Type something above to rephrase it.")
                }
            }
        }

        state.error?.let { message ->
            ErrorCard(message = message, onRetry = viewModel::rephrase)
        }

        state.result?.let { result ->
            SectionCard {
                SectionTitle("Result")
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(
                        text = "Copy",
                        onClick = { clipboard.setText(AnnotatedString(result)) },
                    )
                    SecondaryButton(
                        text = "Use as input",
                        onClick = viewModel::useResultAsInput,
                    )
                    SecondaryButton(text = "Undo", onClick = viewModel::undo)
                }
            }
        }

        RecentRephrasesCard(recent = state.recent)

        Spacer(Modifier.height(24.dp))
    }
}
