package dev.tonholo.ssrcompose.producer.internal.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

private const val BAND_START_FONT_SCALE = 1.03f
private const val BAND_END_FONT_SCALE = 1.05f

/** How one text is laid out on the device's curve: its own density, its style and what it is marked with. */
internal class TextScaling(val density: Density, val style: TextStyle, val mark: ScaledTextMark)

/**
 * Lays out the text that [content] draws as the device draws it, or as given when there is no scene, the font size
 * is not in sp or [scaled] is false, as it is for a text with `autoSize`, whose chosen size the reader cannot see.
 * [content] gets the modifier and the style to pass on to `BasicText`.
 */
@Composable
internal inline fun ScaledText(
    style: TextStyle,
    modifier: Modifier,
    scaled: Boolean,
    crossinline content: @Composable (modifier: Modifier, style: TextStyle) -> Unit,
) {
    val scene = LocalScaledTextScene.current.takeIf { scaled }
    val ambient = LocalDensity.current
    val scaling = remember(scene, style, ambient) { scene?.let { style.scaledOn(scene = it, ambient = ambient) } }
    if (scaling == null) {
        content(modifier, style)
    } else {
        val mark = remember(scaling.mark) { Modifier.semantics { this[scaledTextMark] = scaling.mark } }
        CompositionLocalProvider(LocalDensity provides scaling.density) {
            content(modifier.then(mark), scaling.style)
        }
    }
}

/**
 * Builds the [TextScaling] for this style on [scene] using [DEFAULT_FONT_SIZE_SP] when `fontSize` is unspecified,
 * adjusting sp `lineHeight` to the curve in the `1.03..1.05` `fontScale` band, and marking the resolved sp sizes
 * in [ScaledTextMark].
 *
 * @param scene the device's font scale curve and font scale for the screen.
 * @param ambient the ambient [Density], whose pixel density is preserved.
 * @return the [TextScaling] for this style, or `null` when `fontSize` is neither positive sp nor unspecified, which
 * the reader refuses.
 */
internal fun TextStyle.scaledOn(scene: ScaledTextScene, ambient: Density): TextScaling? {
    val size = when {
        fontSize.isSp && fontSize.value > 0f -> fontSize.value
        fontSize.isUnspecified -> DEFAULT_FONT_SIZE_SP
        else -> return null
    }
    val lineHeightSp = lineHeight.takeIf { it.isSp }?.value
    val factor = scene.curve.spToDp(sp = size) / size
    return TextScaling(
        density = Density(density = ambient.density, fontScale = factor),
        style = lineHeightSp?.let { height ->
            copy(lineHeight = layoutLineHeight(height = height, scene = scene, factor = factor).sp)
        } ?: this,
        mark = ScaledTextMark(sizeSp = size, lineHeightSp = lineHeightSp),
    )
}

private fun layoutLineHeight(height: Float, scene: ScaledTextScene, factor: Float): Float =
    if (scene.fontScale in BAND_START_FONT_SCALE..BAND_END_FONT_SCALE) {
        scene.curve.spToDp(sp = height) / factor
    } else {
        height
    }
