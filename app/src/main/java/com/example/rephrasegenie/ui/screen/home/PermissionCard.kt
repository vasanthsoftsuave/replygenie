package com.example.rephrasegenie.ui.screen.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.theme.AppTheme

/**
 * Explains why the accessibility service is needed and sends the user straight to the setting.
 *
 * This is the app's biggest drop-off point, so the wording says plainly what the service does and
 * what it does not do.
 */
@Composable
fun PermissionCard(
    serviceEnabled: Boolean,
    overlayGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val ready = serviceEnabled && overlayGranted

    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Rephrase bubble")
            Text(
                text = if (ready) "On ✓" else "Off",
                style = MaterialTheme.typography.titleSmall,
                color = if (ready) colors.success else colors.textMuted,
            )
        }

        if (!overlayGranted) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Allow RephraseGenie to draw over other apps, so the bubble can " +
                        "appear next to the text you are typing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                SecondaryButton(
                    text = "Allow drawing over apps",
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        )
                    },
                )
            }
        }

        if (ready) {
            FieldHint(
                "Type in any app, then tap the bubble to rephrase. " +
                    "Long-press it to pick a different tone."
            )
        } else if (!serviceEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Turn on the RephraseGenie service so the bubble can appear while " +
                        "you type in other apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                FieldHint(
                    "It reads the field you are typing in only when you tap the bubble, and " +
                        "writes the result back into that same field. It never reads other " +
                        "people's messages."
                )
                SecondaryButton(
                    text = "Open accessibility settings",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                )
            }
        }
    }
}
