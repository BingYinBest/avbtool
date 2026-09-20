package com.android.avbtoolkit.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Liquid glass backdrop (Kyant Backdrop, Apache-2.0).
 *
 * [rememberLiquidGlass] returns a [LayerBackdrop]; wrap the screen content
 * with [liquidGlassLayer] to record it, then use [liquidGlassSurface] on a
 * floating element (top bar / banner) to sample it with vibrancy, blur and
 * a subtle lens refraction. Effects degrade gracefully on APIs without
 * RenderEffect support.
 */
@Composable
fun rememberLiquidGlass(): LayerBackdrop = rememberLayerBackdrop()

/** Records this layout (and its children) into [backdrop]. */
fun Modifier.liquidGlassLayer(backdrop: LayerBackdrop): Modifier =
    layerBackdrop(backdrop)

/** Draws a liquid glass surface sampling [backdrop] behind this element. */
fun Modifier.liquidGlassSurface(
    backdrop: LayerBackdrop,
    shape: () -> Shape = { RoundedCornerShape(24.dp) },
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = shape,
    effects = {
        vibrancy()
        blur(24.dp.toPx())
        lens(8.dp.toPx(), 16.dp.toPx())
    },
)

/**
 * Records [content] into a liquid glass backdrop and composites it with
 * [glass], which receives the backdrop for sampling.
 */
@Composable
fun LiquidGlassBox(
    content: @Composable () -> Unit,
    glass: @Composable (LayerBackdrop) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberLiquidGlass()
    Box(modifier = modifier.fillMaxSize()) {
        Box(Modifier.liquidGlassLayer(backdrop)) { content() }
        glass(backdrop)
    }
}

/**
 * Soft theme-colored gradient background. Render as the recorded layer of a
 * [rememberLiquidGlass] backdrop so glass surfaces have something to sample.
 */
@Composable
fun LiquidGlassBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.26f),
                            colors.tertiary.copy(alpha = 0.14f),
                            colors.surface,
                        )
                    )
                )
        )
    }
}

/**
 * Glass top bar sampling [backdrop]: back button + title on a liquid
 * surface with a light tint.
 */
@Composable
fun LiquidGlassTopBar(
    backdrop: LayerBackdrop,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .liquidGlassSurface(backdrop, shape = { RoundedCornerShape(0.dp) })
            .background(colors.surface.copy(alpha = 0.35f))
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}
