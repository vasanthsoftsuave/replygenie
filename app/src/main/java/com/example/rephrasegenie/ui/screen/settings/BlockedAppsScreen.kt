package com.example.rephrasegenie.ui.screen.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.rephrasegenie.ui.components.AppIcon
import com.example.rephrasegenie.ui.components.AppScaffold
import com.example.rephrasegenie.ui.components.AppTextField
import com.example.rephrasegenie.ui.components.EmptyState
import com.example.rephrasegenie.ui.components.FieldHint
import com.example.rephrasegenie.ui.theme.AppTheme

/**
 * Picks the apps the bubble must never appear in.
 *
 * The list comes from launcher activities via the manifest `<queries>` block, never from the
 * restricted QUERY_ALL_PACKAGES permission.
 */
@Composable
fun BlockedAppsScreen(
    onBack: () -> Unit,
    viewModel: BlockedAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors

    // Pinned rather than collapsing: the search box below the title has to stay where the user
    // left it while the list underneath scrolls.
    AppScaffold(title = "Blocked Apps", onBack = onBack, collapsing = false) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            FieldHint("Tap an app to block or unblock it. The bubble never appears in a blocked app.")
            Spacer(Modifier.height(12.dp))

            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Search apps",
                leadingIcon = Icons.Outlined.Search,
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = colors.accent,
                    )
                }

                state.visibleApps.isEmpty() -> EmptyState(
                    if (state.query.isBlank()) {
                        "No apps found on this device."
                    } else {
                        "No app matches that search."
                    }
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(state.visibleApps, key = { it.packageName }) { app ->
                        val isBlocked = app.packageName in state.blocked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggle(app.packageName) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIcon(packageName = app.packageName, size = 32)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary,
                                )
                                FieldHint(app.packageName)
                            }
                            Icon(
                                imageVector = if (isBlocked) {
                                    Icons.Outlined.Block
                                } else {
                                    Icons.Outlined.CheckCircle
                                },
                                contentDescription = if (isBlocked) "Blocked" else "Allowed",
                                tint = if (isBlocked) colors.danger else colors.textMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
