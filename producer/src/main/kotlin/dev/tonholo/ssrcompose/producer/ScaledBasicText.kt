package dev.tonholo.ssrcompose.producer

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.producer.internal.text.ScaledText

/**
 * A copy of foundation's `BasicText` for a [String] that lays the text out at the size the device draws it, from the
 * device's font scale curve, and marks it so the producer can send it. The compiler plugin puts it in place of each
 * call to `BasicText` in server code. Server code never calls it. Outside a producer screen it is `BasicText`.
 *
 * The parameters are those of `BasicText`.
 */
@InternalSsrComposeApi
@Composable
public fun ScaledBasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    ScaledText(style = style, modifier = modifier) { scaledModifier, scaledStyle ->
        BasicText(
            text = text,
            modifier = scaledModifier,
            style = scaledStyle,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            color = color,
            autoSize = autoSize,
        )
    }
}

/**
 * A copy of foundation's `BasicText` for an [AnnotatedString], like the one for a [String].
 *
 * The parameters are those of `BasicText`.
 */
@InternalSsrComposeApi
@Composable
public fun ScaledBasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null,
    autoSize: TextAutoSize? = null,
) {
    ScaledText(style = style, modifier = modifier) { scaledModifier, scaledStyle ->
        BasicText(
            text = text,
            modifier = scaledModifier,
            style = scaledStyle,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            inlineContent = inlineContent,
            color = color,
            autoSize = autoSize,
        )
    }
}
