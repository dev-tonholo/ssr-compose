package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.SemanticsNode

private const val GRAPHICS_LAYER_MODIFIER = "graphicsLayer"
private const val SHADOW_MODIFIER = "shadow"
private val translationIndices = setOf(Matrix.TranslateX, Matrix.TranslateY, Matrix.TranslateZ)
private val identityValues = Matrix().values
private val unchangedLayer = Modifier.graphicsLayer().inspectedProperties()
private val verticalScrollClip =
    Modifier.clipScrollableContainer(orientation = Orientation.Vertical).inspectedProperties()
private val horizontalScrollClip =
    Modifier.clipScrollableContainer(orientation = Orientation.Horizontal).inspectedProperties()

/**
 * Refuses a node drawn through a graphics layer that changes it. A layout with a graphics layer is a node of its own,
 * so a layer around a node is refused when that layout's node is read. A scroll container keeps the clip its scroll
 * modifier adds on the axis it scrolls.
 *
 * @throws NotSendableException for a layer that scales, rotates, moves, fades, clips or shadows, or is set in a block.
 */
internal fun List<InspectedModifier>.refuseGraphicsLayers() {
    val allowedLayers = listOf(unchangedLayer) + named(SCROLL_MODIFIER).map { it.scrollClip() }
    val changing = any { modifier ->
        modifier.name == SHADOW_MODIFIER ||
            modifier.name == GRAPHICS_LAYER_MODIFIER && modifier.properties !in allowedLayers
    }
    if (changing) refuse(reason = NotSendableReason.GraphicsLayer)
}

private fun InspectedModifier.scrollClip(): Map<String, Any?> =
    if (isVerticalScroll()) verticalScrollClip else horizontalScrollClip

/**
 * Refuses a node that a layer scales or rotates on its way to the scene's root, also a layer no inspector shows, such
 * as one a custom layout places with `placeWithLayer`. Alpha or a clip set inside such a layer stays unseen.
 *
 * @throws NotSendableException when the transform from the node to the root is more than a move.
 */
internal fun SemanticsNode.refuseTransformedLayout() {
    val coordinates = layoutInfo.coordinates
    val toRoot = Matrix()
    coordinates.findRootCoordinates().transformFrom(sourceCoordinates = coordinates, matrix = toRoot)
    if (!toRoot.isMove()) refuse(reason = NotSendableReason.GraphicsLayer)
}

private fun Matrix.isMove(): Boolean =
    values.withIndex().all { (index, value) -> index in translationIndices || value == identityValues[index] }
