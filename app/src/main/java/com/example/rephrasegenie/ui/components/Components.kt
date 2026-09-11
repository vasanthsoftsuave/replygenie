package com.example.rephrasegenie.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.rephrasegenie.ui.theme.AppTheme

/** The card section used throughout the Windows app: 10dp radius, 1dp border, 20x18 padding. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(10.dp))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** A card heading, with an optional icon that says at a glance what the card is for. */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                // Decorative: the heading beside it already says what this is.
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = AppTheme.colors.textSecondary,
        modifier = modifier,
    )
}

@Composable
fun FieldHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AppTheme.colors.textMuted,
        modifier = modifier,
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = AppTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = placeholder?.let {
            { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted) }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.input,
            unfocusedContainerColor = colors.input,
            disabledContainerColor = colors.input,
            focusedBorderColor = colors.inputFocusBorder,
            unfocusedBorderColor = colors.inputBorder,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.accent,
        ),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.accent.copy(alpha = 0.45f),
            disabledContentColor = colors.onAccent.copy(alpha = 0.7f),
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = colors.onAccent,
            )
        } else {
            ButtonLabel(text = text, icon = icon, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, colors.inputBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.input,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.input.copy(alpha = 0.5f),
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 10.dp,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = colors.textSecondary,
            )
        } else {
            ButtonLabel(text = text, icon = icon, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Icon-only, for actions whose icon is unambiguous — back, settings, close.
 *
 * [contentDescription] is required rather than optional: with no label beside it, the icon is the
 * only thing a screen reader has to go on.
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The tone pill from the Windows tone picker: a coloured dot plus the name. */
@Composable
fun ToneChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .background(
                if (selected) colors.accentTint else colors.input,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (selected) color else colors.inputBorder,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
    }
}

/** A persistent error card with an optional retry, replacing the Windows toast that vanished. */
@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.dangerBackground, RoundedCornerShape(6.dp))
            .border(1.dp, colors.dangerBorder, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.danger,
        )
        if (onRetry != null) {
            SecondaryButton(text = "Retry", onClick = onRetry, icon = Icons.Filled.Refresh)
        }
    }
}

/** Shared by the buttons above so a label with an icon lines up the same way everywhere. */
@Composable
private fun ButtonLabel(
    text: String,
    icon: ImageVector?,
    style: TextStyle,
) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
    }
    Text(text, style = style)
}
