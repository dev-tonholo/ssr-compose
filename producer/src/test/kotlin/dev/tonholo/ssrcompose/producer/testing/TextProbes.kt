package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextMark
import dev.tonholo.ssrcompose.producer.internal.text.scaledTextMark
import kotlin.math.abs
import kotlin.test.assertTrue

internal const val SAMPLE_TEXT = "Served by the producer"
internal const val TWO_LINES_TEXT = "first\nsecond"
internal const val SMALL_SIZE_SP = 16
internal const val LARGE_SIZE_SP = 24
internal const val LINE_HEIGHT_SP = 24
internal const val LINE_HEIGHT_EM = 1.5f
internal const val WIDE_SCENE_PX = 1000
private const val ONE_PX = 1f

/** The scene of the ADR 0023 probes: density 2.5, font scale 1.3 and the emulator's curve. */
internal fun emulatorSpec(): SceneSpec = SceneSpec(
    widthPx = WIDE_SCENE_PX,
    density = Density(density = TEST_DENSITY, fontScale = EMULATOR_FONT_SCALE),
    fontScaleCurve = { EMULATOR_CURVE },
)

/** The scene of font scale 1.04, in the band of the line height rule, with [BAND_CURVE]. */
internal fun bandSpec(): SceneSpec = SceneSpec(
    widthPx = WIDE_SCENE_PX,
    density = Density(density = TEST_DENSITY, fontScale = BAND_FONT_SCALE),
    fontScaleCurve = { BAND_CURVE },
)

internal fun heightOf(spec: SceneSpec, sizeSp: Int): Int =
    heightOf(spec = spec, text = SAMPLE_TEXT, style = robotoStyle(sizeSp = sizeSp))

/** The height in pixels of [text] in [style] drawn by `ScaledBasicText` in a scene made from [spec]. */
internal fun heightOf(spec: SceneSpec, text: String, style: TextStyle): Int {
    var height = 0
    withRenderedScene(
        spec = spec,
        content = { ScaledBasicText(text = text, style = style, onTextLayout = { height = it.size.height }) },
        read = {},
    )
    return height
}

/** The height in pixels of the sample text drawn by `ScaledBasicText` in a scene with no producer screen. */
internal fun bareHeightOf(spec: SceneSpec, sizeSp: Int): Int {
    var height = 0
    withBareScene(
        spec = spec,
        content = {
            ScaledBasicText(
                text = SAMPLE_TEXT,
                style = robotoStyle(sizeSp = sizeSp),
                onTextLayout = { height = it.size.height },
            )
        },
        read = {},
    )
    return height
}

internal fun bareMarkOf(spec: SceneSpec): ScaledTextMark? = withBareScene(
    spec = spec,
    content = { ScaledBasicText(text = SAMPLE_TEXT, style = robotoStyle(sizeSp = SMALL_SIZE_SP)) },
    read = { roots -> roots.single().textNode().scaledMark() },
)

internal fun markOf(spec: SceneSpec, style: TextStyle = robotoStyle(sizeSp = SMALL_SIZE_SP)): ScaledTextMark? =
    withRenderedScene(
        spec = spec,
        content = { ScaledBasicText(text = SAMPLE_TEXT, style = style) },
        read = { scene -> scene.read { roots -> roots.single().textNode().scaledMark() } },
    )

/**
 * The height in pixels of two lines of plain `BasicText` at density 2.5 and font scale 1, sized at [curve] of 16 sp and
 * spaced by [lineHeightSp], which is what the device draws on that curve.
 */
internal fun heightOfBasicText(
    curve: FontScaleCurve,
    lineHeightSp: Float = curve.spToDp(sp = LINE_HEIGHT_SP.toFloat()),
): Int {
    val size = curve.spToDp(sp = SMALL_SIZE_SP.toFloat())
    val style = robotoStyle(sizeSp = SMALL_SIZE_SP).copy(fontSize = size.sp, lineHeight = lineHeightSp.sp)
    var laidOut = 0
    withRenderedScene(
        spec = SceneSpec(widthPx = WIDE_SCENE_PX, density = Density(density = TEST_DENSITY, fontScale = 1f)),
        content = { BasicText(text = TWO_LINES_TEXT, style = style, onTextLayout = { laidOut = it.size.height }) },
        read = {},
    )
    return laidOut
}

internal fun assertHeights(expected: List<Float>, actual: List<Int>) {
    assertTrue(actual = expected.size == actual.size, message = "expected $expected px but was $actual px")
    expected.zip(actual).forEach { (want, got) ->
        assertTrue(actual = abs(want - got) <= ONE_PX, message = "expected $expected px but was $actual px")
    }
}

internal fun DocumentNode.firstText(): DocumentNode.Text = when (this) {
    is DocumentNode.Text -> this
    is DocumentNode.Box -> children.firstNotNullOf { child -> child.firstTextOrNull() }
    else -> error("no text in $this")
}

private fun DocumentNode.firstTextOrNull(): DocumentNode.Text? = runCatching { firstText() }.getOrNull()

private fun SemanticsNode.textNode(): SemanticsNode =
    if (SemanticsProperties.Text in config) this else children.firstNotNullOf { it.textNodeOrNull() }

private fun SemanticsNode.textNodeOrNull(): SemanticsNode? = runCatching { textNode() }.getOrNull()

private fun SemanticsNode.scaledMark(): ScaledTextMark? =
    if (config.contains(scaledTextMark)) config[scaledTextMark] else null
