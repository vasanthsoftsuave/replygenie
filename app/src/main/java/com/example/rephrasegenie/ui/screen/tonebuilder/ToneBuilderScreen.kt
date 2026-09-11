package com.example.rephrasegenie.ui.screen.tonebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.domain.model.ToneColors
import com.example.rephrasegenie.ui.components.AppScaffold
import com.example.rephrasegenie.ui.components.AppTextField
import com.example.rephrasegenie.ui.components.ErrorCard
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.FieldLabel
import com.example.rephrasegenie.ui.components.PrimaryButton
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.components.SwatchGrid
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.parseHexColor

/**
 * Three cards and a test area, per §11.
 *
 * The test runs the **cleaned** instruction, so what the user sees here is what the saved tone
 * will really produce.
 */
@Composable
fun ToneBuilderScreen(
    onDone: () -> Unit,
    viewModel: ToneBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    if (state.loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = colors.accent,
            )
        }
        return
    }

    AppScaffold(
        title = if (state.isEditing) "Edit tone" else "New tone",
        onBack = onDone,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.error?.let { ErrorCard(message = it) }

            // 1. Identity
            SectionCard {
                SectionTitle("Tone Identity", icon = Icons.Outlined.Label)

                FieldLabel("Name")
                AppTextField(
                    value = state.draft.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Professional",
                    enabled = !state.saving,
                )

                FieldLabel("Short description")
                AppTextField(
                    value = state.draft.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "Clear and businesslike, for work messages",
                    enabled = !state.saving,
                )
                FieldHint("Shown under the tone name. Optional.")
            }

            // 2. Colour
            SectionCard {
                SectionTitle("Tone Colour", icon = Icons.Outlined.Palette)
                FieldHint("Each tone needs its own colour, so the bubble always says which one is on.")

                SwatchGrid(
                    swatches = ToneColors.PALETTE,
                    selected = ToneColors.normalize(state.draft.color),
                    onSelect = viewModel::onColorSelected,
                    perRow = 6,
                    disabled = state.takenColors.keys,
                    swatchSize = 40,
                )

                val previewName = state.draft.name.ifBlank { "Your tone" }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(parseHexColor(state.draft.color), CircleShape),
                    )
                    Text(
                        text = previewName,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                    )
                }

                if (state.takenColors.isNotEmpty()) {
                    FieldHint("Dimmed colours are already used by another tone.")
                }
            }

            // 3. Instructions
            SectionCard {
                SectionTitle("Instructions", icon = Icons.Outlined.Notes)

                FieldLabel("How should the text sound?")
                AppTextField(
                    value = state.draft.rawInstruction,
                    onValueChange = viewModel::onInstructionChange,
                    placeholder = "Clear and businesslike. Short sentences, no slang.",
                    singleLine = false,
                    minLines = 3,
                    enabled = !state.saving,
                )
                FieldHint(
                    "Describe the style only. RephraseGenie rewords what you wrote — it never " +
                        "answers or replies, whatever the instruction says."
                )

                Spacer(Modifier.height(4.dp))

                FieldLabel("Example input (optional)")
                AppTextField(
                    value = state.draft.exampleInput,
                    onValueChange = viewModel::onExampleInputChange,
                    placeholder = "hey can you send me that file when you get a sec",
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.saving,
                )

                FieldLabel("Example output (optional)")
                AppTextField(
                    value = state.draft.exampleOutput,
                    onValueChange = viewModel::onExampleOutputChange,
                    placeholder = "Could you please send me that file when you have a moment?",
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.saving,
                )
                FieldHint("Examples are shown in the app only. They are never sent to the AI.")
            }

            // Test
            SectionCard {
                SectionTitle("Test", icon = Icons.Outlined.Science)
                FieldHint("Runs the tone exactly as it will behave once saved.")

                AppTextField(
                    value = state.sampleText,
                    onValueChange = viewModel::onSampleTextChange,
                    placeholder = ToneBuilderUiState.DEFAULT_SAMPLE,
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.isTesting,
                )

                if (state.isTesting) {
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
                            text = state.testStageLabel.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                        SecondaryButton(text = "Cancel", onClick = viewModel::cancelTest)
                    }
                } else {
                    SecondaryButton(
                        text = "Test",
                        icon = Icons.Outlined.PlayArrow,
                        onClick = viewModel::test,
                        enabled = state.draft.rawInstruction.isNotBlank() &&
                            state.sampleText.isNotBlank(),
                    )
                }

                state.testResult?.let { result ->
                    Spacer(Modifier.height(4.dp))
                    FieldLabel("Result")
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }

                state.cleanedPrompt?.let { cleaned ->
                    Spacer(Modifier.height(4.dp))
                    FieldLabel("Instruction actually used")
                    FieldHint(cleaned)
                }
            }

            PrimaryButton(
                text = if (state.isEditing) "Save changes" else "Create tone",
                icon = Icons.Outlined.Check,
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave,
                loading = state.saving,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
