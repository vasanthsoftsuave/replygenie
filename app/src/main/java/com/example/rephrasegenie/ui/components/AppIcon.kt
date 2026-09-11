package com.example.rephrasegenie.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.rephrasegenie.ui.theme.AppTheme
import kotlin.math.roundToInt

/**
 * A launcher icon, looked up by package name.
 *
 * Visible to us for launchable apps through the manifest `<queries>` block. An app that has since
 * been uninstalled draws as an empty tile rather than throwing, so the row is still there to
 * remove.
 */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Int = 32) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { size.dp.toPx() }.roundToInt().coerceAtLeast(1)

    val image: ImageBitmap? = remember(packageName, sizePx) {
        runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(width = sizePx, height = sizePx)
                .asImageBitmap()
        }.getOrNull()
    }

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(size.dp),
        )
    } else {
        Box(
            modifier
                .size(size.dp)
                .background(AppTheme.colors.input, RoundedCornerShape(6.dp)),
        )
    }
}
