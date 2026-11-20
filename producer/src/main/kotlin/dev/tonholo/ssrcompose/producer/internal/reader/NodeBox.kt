package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.toSize

/** Where something is drawn, in pixels: its top-left corner is [origin] in [coordinates]. */
internal class NodeBox(
    private val coordinates: LayoutCoordinates,
    private val origin: Offset,
    val size: Size,
) {
    /** Where [other]'s top-left corner is from this box's top-left corner, in pixels. */
    fun offsetOf(other: NodeBox): Offset = coordinates.localPositionOf(other.coordinates, other.origin) - origin

    fun sameAs(other: NodeBox): Boolean = size == other.size && offsetOf(other) == Offset.Zero
}

/** A node's box in dp, from the top-left corner of the node it is sent in. */
internal data class Placement(val x: Float, val y: Float, val width: Float, val height: Float)

internal fun LayoutCoordinates.toNodeBox(): NodeBox =
    NodeBox(coordinates = this, origin = Offset.Zero, size = size.toSize())

/** The box of the node's own semantics, which for a text is the box its lines are laid out in. */
internal fun SemanticsNode.ownBox(): NodeBox = NodeBox(
    coordinates = layoutInfo.coordinates.findRootCoordinates(),
    origin = positionInRoot,
    size = size.toSize(),
)

internal fun NodeBox.placedIn(parent: NodeBox, density: Float): Placement {
    val offset = parent.offsetOf(other = this)
    return Placement(
        x = offset.x / density,
        y = offset.y / density,
        width = size.width / density,
        height = size.height / density,
    )
}
