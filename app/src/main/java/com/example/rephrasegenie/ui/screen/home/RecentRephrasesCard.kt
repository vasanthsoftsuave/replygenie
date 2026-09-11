package com.example.rephrasegenie.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rephrasegenie.ui.components.EmptyState
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.components.SectionCard
import com.example.rephrasegenie.ui.components.SectionTitle
import com.example.rephrasegenie.ui.theme.AppTheme
import com.example.rephrasegenie.ui.theme.parseHexColor
import java.time.Duration
import java.time.Instant

/**
 * The last few rephrases.
 *
 * Shows the tone, when it ran and how long it took — never the text. That rule comes from the
 * Windows app and holds everywhere: only lengths are ever stored, so only lengths can be shown.
 */
@Composable
fun RecentRephrasesCard(
    recent: List<RecentRephrase>,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    SectionCard(modifier = modifier) {
        SectionTitle("Recent rephrases")

        if (recent.isEmpty()) {
            EmptyState("Nothing yet. Your rephrases will be listed here.")
            FieldHint("Only the tone, timing and length are kept — never your text.")
            return@SectionCard
        }

        recent.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(parseHexColor(row.toneColor), CircleShape),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.toneName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                    FieldHint(
                        "${row.inputLength} → ${row.outputLength} characters · " +
                            "%.1fs".format(row.durationMs / 1000f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (row.succeeded) "Done" else "Blocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.succeeded) colors.success else colors.danger,
                    )
                    FieldHint(relativeTime(row.timestamp))
                }
            }
        }

        FieldHint("Only the tone, timing and length are kept — never your text.")
    }
}

/** Rounded to the nearest useful unit. An exact timestamp would be noise here. */
private fun relativeTime(instant: Instant): String {
    val elapsed = Duration.between(instant, Instant.now())
    val minutes = elapsed.toMinutes()
    val hours = elapsed.toHours()
    val days = elapsed.toDays()

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
