package com.example.rephrasegenie.ui.screen.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.ui.components.AppScaffold
import com.example.rephrasegenie.ui.components.AppTextField
import com.example.rephrasegenie.ui.components.ErrorCard
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.FieldLabel
import com.example.rephrasegenie.ui.components.PrimaryButton
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.theme.AppTheme

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    LaunchedEffect(state.saved) {
        if (state.saved) onSetupComplete()
    }

    // No title bar: the branded header below is this screen's title. The scaffold is still here
    // for its window insets, so nothing runs under the status bar or the camera cut-out.
    AppScaffold(title = null) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RephraseGenie",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.accent,
                )
                Text(
                    text = "Rewrite your text in any tone, anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("Username")
                AppTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    placeholder = "Your name",
                    enabled = !state.saving,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("OpenAI API Key")
                AppTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    placeholder = "sk-...",
                    isPassword = true,
                    enabled = !state.saving,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                FieldHint("Stored securely on this device only. Never sent anywhere but OpenAI.")
                FieldHint("Get a key at platform.openai.com/api-keys")

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SecondaryButton(
                        text = "Test Connection",
                        icon = Icons.Outlined.WifiTethering,
                        onClick = viewModel::testConnection,
                        enabled = !state.saving,
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
            }

            PrimaryButton(
                text = "Save & Continue",
                icon = Icons.Outlined.ArrowForward,
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave,
                loading = state.saving,
            )

            state.error?.let { ErrorCard(message = it) }

            FieldHint(
                "AI-generated rewrites can be inaccurate or miss context " +
                    "— always review before sending."
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
