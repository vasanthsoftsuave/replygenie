package com.example.rephrasegenie.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.rephrasegenie.ui.theme.AppTheme

/**
 * The frame every screen sits in.
 *
 * Two jobs, both of which used to be missing:
 *
 * - **Nothing draws under the notch.** The window is edge-to-edge, so without this the first line
 *   of a scrolling screen ran straight into the status bar and the camera cut-out. The title bar
 *   takes the top inset, and [content] is handed padding that already clears it.
 * - **The bar gets out of the way.** This is the Compose equivalent of a CoordinatorLayout with
 *   `scroll|enterAlways`: scrolling down slides the title off the top, scrolling up brings it
 *   straight back. Set [collapsing] to false to pin it instead, which suits a screen whose own
 *   header — a search box, say — has to stay put.
 *
 * Pass a null [title] for a screen that draws its own header, such as Setup. The insets are still
 * applied, so its content clears the notch the same way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    collapsing: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = AppTheme.colors
    val scrollBehavior = if (collapsing) {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.textPrimary,
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconActionButton(
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                onClick = onBack,
                            )
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        // Once the content has moved under it, the bar needs its own surface or
                        // the title and the text below it run together.
                        scrolledContainerColor = colors.header,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textSecondary,
                        actionIconContentColor = colors.textSecondary,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        content = content,
    )
}
