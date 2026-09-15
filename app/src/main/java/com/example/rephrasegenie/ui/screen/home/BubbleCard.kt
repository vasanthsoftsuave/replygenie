package com.example.rephrasegenie.ui.screen.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.SecondaryButton
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.theme.AppTheme

/**
 * The two permissions the bubble needs, and the switch that puts it on screen.
 *
 * The permission rows stay put once granted, marked in green, rather than disappearing. A row that
 * vanishes on success leaves nothing behind to check against later — and these are settings the
 * system can revoke without telling the app, so "it was there yesterday" is not the same as "it is
 * on now". The switch sits below them in the same shape, because it is the third thing in the same
 * list: two permissions, then the thing they are for.
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

    SectionCard(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

        StatusRow(
            icon = Icons.Outlined.Layers,
            title = "Draw over other apps",
            description = "Lets the bubble sit on top of whatever app you are typing in.",
            granted = overlayGranted,
        ) {
            SecondaryButton(
                text = "Allow",
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

        StatusRow(
            icon = Icons.Outlined.Accessibility,
            title = "RephraseGenie service",
            description = "Reads the field you are typing in when you tap the bubble, and writes " +
                "the result back into it.",
            granted = serviceEnabled,
        ) {
            SecondaryButton(
                text = "Open",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
            )
        }

        StatusRow(
            icon = Icons.Outlined.AutoAwesome,
            title = "Show the bubble",
            description = "Stays on screen in every app until you turn it off. Drag it anywhere.",
            granted = null,
            enabled = hasAccess,
        ) {
            Switch(
                checked = bubbleEnabled,
                onCheckedChange = onBubbleEnabledChange,
                enabled = hasAccess,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onAccent,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.input,
                    uncheckedBorderColor = colors.inputBorder,
                ),
            )
        }

        FieldHint(
            when {
                !hasAccess -> "Grant both permissions above to switch the bubble on."
                bubbleEnabled -> "Tap the bubble to rephrase what you typed. Long-press it to " +
                    "pick a tone."
                else -> "The bubble stays hidden everywhere until you turn this back on."
            }
        )
    }
}

/**
 * One row of the list: an icon, what it is, and on the right either a green tick or whatever
 * control is needed to get there.
 *
 * [granted] is null for rows that are not a permission, which is how the bubble switch shares the
 * shape without pretending to be one.
 */
@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    control: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (granted == true) colors.success.copy(alpha = 0.14f) else colors.input,
                    RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted == true) colors.success else colors.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) colors.textPrimary else colors.textMuted,
            )
            FieldHint(description)
        }

        if (granted == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .background(colors.success.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.success,
                )
            }
        } else {
            control()
        }
    }
}
