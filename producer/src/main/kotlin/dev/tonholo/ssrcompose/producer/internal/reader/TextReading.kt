package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily

/** The size `BasicText` uses for a style with no font size, in sp. */
internal const val DEFAULT_FONT_SIZE_SP = 14f
private const val TEXT_SEPARATOR = ""
private val SENT_OVERFLOWS = mapOf(
    TextOverflow.Clip to DocumentNode.Text.Overflow.Clip,
    TextOverflow.Ellipsis to DocumentNode.Text.Overflow.Ellipsis,
)

/**
 * Reads a text node at [placement].
 *
 * @throws NotSendableException for several styles, no font family, `FontFamily.Default`, a family that is not the
 * bundled Roboto, a font size that is not in sp, or a style the document cannot hold.
 */
internal fun SemanticsNode.readText(placement: Placement): DocumentNode.Text {
    val texts = config[SemanticsProperties.Text]
    if (texts.any { it.hasSeveralStyles() }) refuse(reason = NotSendableReason.SeveralStyles)
    val input = textLayout().layoutInput
    if (!input.softWrap || input.style.hasUnsentDrawing()) refuse(reason = NotSendableReason.UnsupportedTextStyle)
    return DocumentNode.Text(
        id = id,
        x = placement.x,
        y = placement.y,
        width = placement.width,
        height = placement.height,
        text = texts.joinToString(separator = TEXT_SEPARATOR) { it.text },
        style = input.style.toSpec(alignment = input.style.textAlign.toAlign(direction = input.layoutDirection)),
        maxLines = input.maxLines.takeUnless { it == Int.MAX_VALUE },
        overflow = input.overflow.toOverflow(),
        semantics = readSemantics(),
    )
}

private fun AnnotatedString.hasSeveralStyles(): Boolean = spanStyles.isNotEmpty() || paragraphStyles.isNotEmpty()

private fun SemanticsNode.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.single()
}

private fun TextStyle.toSpec(alignment: TextStyleSpec.Align): TextStyleSpec {
    val size = when {
        fontSize.isSp -> fontSize.value
        fontSize.isEm -> refuse(reason = NotSendableReason.RelativeFontSize)
        else -> DEFAULT_FONT_SIZE_SP
    }
    return TextStyleSpec(
        family = fontFamily.toFamilyName(),
        size = size,
        weight = (fontWeight ?: FontWeight.Normal).weight,
        italic = fontStyle == FontStyle.Italic,
        color = color.takeOrElse { Color.Black }.toDocumentColor(),
        lineHeight = lineHeight.toSp(fontSize = size),
        align = alignment,
    )
}

private fun TextOverflow.toOverflow(): DocumentNode.Text.Overflow =
    SENT_OVERFLOWS[this] ?: refuse(reason = NotSendableReason.UnsupportedTextStyle)

private fun TextStyle.hasUnsentDrawing(): Boolean =
    letterSpacing != TextUnit.Unspecified ||
        textDecoration !in setOf(null, TextDecoration.None) ||
        brush != null ||
        shadow !in setOf(null, Shadow.None) ||
        baselineShift !in setOf(null, BaselineShift.None) ||
        textIndent !in setOf(null, TextIndent.None)

private fun Color.takeOrElse(fallback: () -> Color): Color = if (this == Color.Unspecified) fallback() else this

/** The bundled Roboto is written as no family, which the consumer draws with its own Roboto. */
private fun FontFamily?.toFamilyName(): String? = when (this) {
    null, FontFamily.Default -> refuse(reason = NotSendableReason.NoFontFamily)
    bundledRobotoFamily() -> null
    else -> refuse(reason = NotSendableReason.UnknownFontFamily)
}

private fun TextUnit.toSp(fontSize: Float): Float? = when {
    isSp -> value
    isEm -> value * fontSize
    else -> null
}
