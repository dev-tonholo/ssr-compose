package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.ScrollState
import dev.tonholo.ssrcompose.core.document.DocumentNode

private const val STATE_PROPERTY = "state"
private const val VERTICAL_PROPERTY = "isVertical"

/** Whether this `scroll` modifier scrolls vertically. */
internal fun InspectedModifier.isVerticalScroll(): Boolean = property(VERTICAL_PROPERTY) as Boolean

/**
 * Reads a `verticalScroll` or `horizontalScroll` container at [placement], holding [children]. Its content is
 * its own size plus how far it can scroll.
 */
internal fun InspectedModifier.readScroll(
    id: Int,
    placement: Placement,
    density: Float,
    children: List<DocumentNode>,
): DocumentNode.Scroll {
    val scrollRange = (property(STATE_PROPERTY) as ScrollState).maxValue / density
    val vertical = isVerticalScroll()
    return DocumentNode.Scroll(
        id = id,
        x = placement.x,
        y = placement.y,
        width = placement.width,
        height = placement.height,
        orientation = if (vertical) DocumentNode.Scroll.Orientation.Vertical else
            DocumentNode.Scroll.Orientation.Horizontal,
        contentWidth = placement.width + if (vertical) 0f else scrollRange,
        contentHeight = placement.height + if (vertical) scrollRange else 0f,
        children = children,
    )
}
