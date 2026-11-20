package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.document.TextStyleSpec

/** Left and right are turned into start and end for the text's own direction. */
internal fun TextAlign.toAlign(direction: LayoutDirection): TextStyleSpec.Align {
    val leftToRight = direction == LayoutDirection.Ltr
    return when (this) {
        TextAlign.Center -> TextStyleSpec.Align.Center
        TextAlign.End -> TextStyleSpec.Align.End
        TextAlign.Justify -> TextStyleSpec.Align.Justify
        TextAlign.Left -> if (leftToRight) TextStyleSpec.Align.Start else TextStyleSpec.Align.End
        TextAlign.Right -> if (leftToRight) TextStyleSpec.Align.End else TextStyleSpec.Align.Start
        else -> TextStyleSpec.Align.Start
    }
}
