package com.example.rephrasegenie.ui.screen.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Layers
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
import com.example.rephrasegenie.ui.components.SwitchRow
import com.example.rephrasegenie.ui.theme.AppTheme

/**
 * Whether the bubble is running, and the switch that turns it on and off.
 *
 * The switch only appears once both permissions are granted. Before that it would be a control
 * that changes nothing, which is worse than not offering it: the honest thing to show while access
 * is missing is how to grant it.
 *
 * This is the app's biggest drop-off point, so the wording says plainly what the service does and
 * what it does not do.
 */
@Composable
fun BubbleCard(
    serviceEnabled: Boolean,
    overlayGranted: Boolean,
    bubbleEnabled: Boolean,
    onBubbleEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val hasAccess = serviceEnabled && overlayGranted
    val running = hasAccess && bubbleEnabled

    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Rephrase bubble", icon = Icons.Outlined.AutoAwesome)
            Text(
                text = when {
                    running -> "On ✓"
                    hasAccess -> "Off"
                    else -> "Needs permission"
                },
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    running -> colors.success
                    hasAccess -> colors.textMuted
                    else -> colors.danger
                },
            )
        }

        if (hasAccess) {
            SwitchRow(
                title = "Show the bubble",
                description = "Appears next to whatever text field you tap into, in any app.",
                checked = bubbleEnabled,
                onCheckedChange = onBubbleEnabledChange,
            )

            FieldHint(
                if (bubbleEnabled) {
                    "Tap the bubble to rephrase what you typed. Long-press it to pick a tone."
                } else {
                    "The bubble stays hidden everywhere until you turn this back on."
                }
            )
            return@SectionCard
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
                    icon = Icons.Outlined.Layers,
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

        if (!serviceEnabled) {
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
                    icon = Icons.Outlined.Accessibility,
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
