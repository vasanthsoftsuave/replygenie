package com.example.rephrasegenie.ui.screen.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.domain.model.ThemeMode
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.ui.components.AppIcon
import com.example.rephrasegenie.ui.components.AppScaffold
import com.example.rephrasegenie.ui.components.AppTextField
import com.example.rephrasegenie.ui.components.Badge
import com.example.rephrasegenie.ui.components.DangerButton
import com.example.rephrasegenie.ui.components.EmptyState
import com.example.rephrasegenie.ui.components.ErrorCard
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.FieldLabel
import com.example.rephrasegenie.ui.components.IconActionButton
import com.example.rephrasegenie.ui.components.PrimaryButton
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.components.SegmentedChoice
import com.example.rephrasegenie.ui.components.SwatchGrid
import com.example.rephrasegenie.ui.components.SwitchRow
import com.example.rephrasegenie.ui.components.ToneChip
import com.example.rephrasegenie.ui.screen.setup.ConnectionTest
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.PresetAccents
import com.example.rephrasegenie.ui.theme.parseHexColor
import com.example.rephrasegenie.ui.theme.toHexString

/**
 * The five cards from §11, following the Windows layout.
 *
 * Every change saves the moment it is made. There is no Save or Cancel button, except on the
 * profile card, where the API key has to be checked against OpenAI before it can be stored.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddBlockedApp: () -> Unit,
    onNewTone: () -> Unit,
    onEditTone: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    AppScaffold(title = "Settings", onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.error?.let { ErrorCard(message = it) }
            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.success,
                )
            }

            ProfileCard(state = state, viewModel = viewModel)
            AppearanceCard(state = state, viewModel = viewModel)
            BehaviourCard(state = state, viewModel = viewModel)
            BlockedAppsCard(state = state, viewModel = viewModel, onAddBlockedApp = onAddBlockedApp)
            CustomTonesCard(
                state = state,
                viewModel = viewModel,
                onNewTone = onNewTone,
                onEditTone = onEditTone,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    val colors = AppTheme.colors

    SectionCard {
        SectionTitle("Profile & AI", icon = Icons.Outlined.Person)

        FieldLabel("Username")
        AppTextField(
            value = state.usernameDraft,
            onValueChange = viewModel::onUsernameChange,
            placeholder = "Your name",
            enabled = !state.savingProfile,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FieldLabel("OpenAI API Key")
            if (state.hasStoredKey) Badge("Configured")
        }
        AppTextField(
            value = state.apiKeyDraft,
            onValueChange = viewModel::onApiKeyChange,
            placeholder = if (state.hasStoredKey) "Leave blank to keep the current key" else "sk-...",
            isPassword = true,
            enabled = !state.savingProfile,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        FieldHint("Stored securely on this device only. Never sent anywhere but OpenAI.")

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(
                text = "Test Connection",
                icon = Icons.Outlined.WifiTethering,
                onClick = viewModel::testConnection,
                enabled = !state.savingProfile,
                loading = state.connectionTest == ConnectionTest.TESTING,
            )
            state.connectionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (state.connectionTest) {
                        ConnectionTest.SUCCESS -> colors.success
                        ConnectionTest.FAILED -> colors.danger
                        else -> colors.textSecondary
                    },
                )
            }
        }

        PrimaryButton(
            text = "Save profile",
            icon = Icons.Outlined.Save,
            onClick = viewModel::saveProfile,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.profileChanged,
            loading = state.savingProfile,
        )
    }
}

@Composable
private fun AppearanceCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        SectionTitle("Appearance", icon = Icons.Outlined.Palette)

        FieldLabel("Theme")
        SegmentedChoice(
            options = listOf(
                ThemeMode.DARK to "Dark",
                ThemeMode.LIGHT to "Light",
                ThemeMode.SYSTEM to "System",
            ),
            selected = state.settings.themeMode,
            onSelect = viewModel::setThemeMode,
        )

        Spacer(Modifier.height(8.dp))

        FieldLabel("Accent colour")
        SwatchGrid(
            swatches = PresetAccents.map { it.second.toHexString() },
            selected = parseHexColor(state.settings.accentColor).toHexString(),
            onSelect = viewModel::setAccentColor,
            perRow = 8,
            swatchSize = 32,
        )
        FieldHint(PresetAccents.firstOrNull {
            it.second.toHexString() == parseHexColor(state.settings.accentColor).toHexString()
        }?.first ?: "Custom")
    }
}

@Composable
private fun BehaviourCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard {
        SectionTitle("Behaviour", icon = Icons.Outlined.Tune)

        FieldLabel("Default tone")
        FieldHint("Used when you tap the bubble. Long-press it to pick a different tone.")

        if (state.tones.isEmpty()) {
            EmptyState("No tones yet.")
        } else {
            // Wraps rather than scrolls sideways, so no tone can hide off the edge.
            state.tones.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { tone ->
                        ToneChip(
                            name = tone.name,
                            color = parseHexColor(tone.color),
                            selected = tone.id == state.settings.defaultToneId,
                            onClick = {
                                val next = if (tone.id == state.settings.defaultToneId) {
                                    null
                                } else {
                                    tone.id
                                }
                                viewModel.setDefaultTone(next)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        SwitchRow(
            title = "Rephrase bubble",
            description = "Show the bubble when you tap into a text field in any app.",
            checked = state.settings.bubbleEnabled,
            onCheckedChange = viewModel::setBubbleEnabled,
        )

        Spacer(Modifier.height(4.dp))

        SwitchRow(
            title = "Extra Content Safety Checks",
            description = "Runs additional moderation checks on generated text (adds slight " +
                "latency). Rewrites are always checked for accuracy regardless of this setting.",
            checked = state.settings.guardrailsEnabled,
            onCheckedChange = viewModel::setGuardrailsEnabled,
        )
    }
}

@Composable
private fun BlockedAppsCard(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onAddBlockedApp: () -> Unit,
) {
    val colors = AppTheme.colors

    SectionCard {
        SectionTitle("Blocked Apps", icon = Icons.Outlined.Block)
        FieldHint("RephraseGenie will never show the bubble in these apps.")

        if (state.blockedApps.isEmpty()) {
            EmptyState("No blocked apps. The bubble can appear anywhere you type.")
        } else {
            state.blockedApps.forEach { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppIcon(packageName = app.packageName, size = 28)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        FieldHint(app.packageName)
                    }
                    IconActionButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Stop blocking " + app.label,
                        tint = colors.danger,
                        onClick = { viewModel.unblockApp(app.packageName) },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SecondaryButton(
            text = "Add app",
            icon = Icons.Outlined.Add,
            onClick = onAddBlockedApp,
        )
    }
}

@Composable
private fun CustomTonesCard(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onNewTone: () -> Unit,
    onEditTone: (String) -> Unit,
) {
    val colors = AppTheme.colors
    var pendingDelete by remember { mutableStateOf<Tone?>(null) }

    SectionCard {
        SectionTitle("Custom Tones", icon = Icons.Outlined.Brush)
        FieldHint("Built-in tones cannot be edited or deleted.")

        if (state.customTones.isEmpty()) {
            EmptyState("No custom tones yet. Make one to write in your own voice.")
        } else {
            state.customTones.forEach { tone ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(
                        Modifier
                            .size(12.dp)
                            .background(parseHexColor(tone.color), CircleShape),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = tone.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                        )
                        if (tone.description.isNotBlank()) FieldHint(tone.description)
                    }
                    IconActionButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = "Edit " + tone.name,
                        onClick = { onEditTone(tone.id) },
                    )
                    IconActionButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Delete " + tone.name,
                        tint = colors.danger,
                        onClick = { pendingDelete = tone },
                    )
                }
            }
        }

        // Confirmed inline rather than in a dialog: deleting a tone is not recoverable, and the
        // row the user is about to lose stays visible while they decide.
        pendingDelete?.let { tone ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Delete ${tone.name}? This cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.danger,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DangerButton(
                        text = "Delete",
                        icon = Icons.Outlined.Delete,
                        onClick = {
                            viewModel.deleteTone(tone.id)
                            pendingDelete = null
                        },
                    )
                    SecondaryButton(text = "Cancel", onClick = { pendingDelete = null })
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        SecondaryButton(text = "New tone", icon = Icons.Outlined.Add, onClick = onNewTone)
    }
}
