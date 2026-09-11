package com.example.rephrasegenie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.parseHexColor

/**
 * A label, an explanation and a switch. The explanation carries the weight here: a bare toggle
 * called "Extra Content Safety Checks" tells the user nothing about what it costs them.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) colors.textPrimary else colors.textMuted,
            )
            description?.let { FieldHint(it) }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.input,
                uncheckedBorderColor = colors.inputBorder,
            ),
        )
    }
}

/** A row of mutually exclusive options, used for Dark / Light / System. */
@Composable
fun <T> SegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) colors.accentTint else colors.input,
                        RoundedCornerShape(6.dp),
                    )
                    .border(
                        1.dp,
                        if (isSelected) colors.accent else colors.inputBorder,
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) colors.accent else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * A grid of colour swatches, laid out in fixed rows.
 *
 * Deliberately not a LazyVerticalGrid: these grids sit inside a scrolling column, where a lazy
 * grid is measured with unbounded height and crashes.
 */
@Composable
fun SwatchGrid(
    swatches: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    perRow: Int = 8,
    disabled: Set<String> = emptySet(),
    swatchSize: Int = 36,
) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        swatches.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { hex ->
                    val isDisabled = hex in disabled
                    val isSelected = hex == selected
                    Box(
                        modifier = Modifier
                            .size(swatchSize.dp)
                            .background(
                                parseHexColor(hex).copy(alpha = if (isDisabled) 0.25f else 1f),
                                CircleShape,
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = when {
                                    isSelected -> colors.textPrimary
                                    isDisabled -> Color.Transparent
                                    else -> colors.cardBorder
                                },
                                shape = CircleShape,
                            )
                            .clickable(enabled = !isDisabled) { onSelect(hex) },
                    )
                }
                // Keeps a short last row aligned with the ones above it.
                repeat(perRow - row.size) { Spacer(Modifier.size(swatchSize.dp)) }
            }
        }
    }
}

/** The "Configured" pill next to the API key field. */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, tint: Color? = null) {
    val colors = AppTheme.colors
    val color = tint ?: colors.success
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Destructive actions: delete a tone, remove a blocked app. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val colors = AppTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.dangerBackground,
            contentColor = colors.danger,
            disabledContainerColor = colors.input,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

/** A hairline between rows inside a card. */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppTheme.colors.cardBorder),
    )
}

/** Shown where a list would otherwise be blank, so an empty card never looks like a failure. */
@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.input, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}
