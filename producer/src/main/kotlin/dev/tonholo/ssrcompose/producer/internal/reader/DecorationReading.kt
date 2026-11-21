package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import dev.tonholo.ssrcompose.core.document.Decoration
import dev.tonholo.ssrcompose.core.document.DocumentColor

private const val COLOR_PROPERTY = "color"
private const val BRUSH_PROPERTY = "brush"
private const val SHAPE_PROPERTY = "shape"
private const val WIDTH_PROPERTY = "width"
private val DECORATION_MODIFIERS = setOf(BACKGROUND_MODIFIER, BORDER_MODIFIER)

/** A decoration and the box it is drawn in. */
internal data class DecoratedBox(val decoration: Decoration, val box: NodeBox)

/**
 * Reads a node's background and border, or null when it has neither.
 *
 * @throws NotSendableException when a brush, a shape the document cannot hold, two of one kind, or a background
 * and a border in different boxes or shapes are found.
 */
internal fun List<InspectedModifier>.readDecoration(density: Density): DecoratedBox? {
    val parts = filter { it.name in DECORATION_MODIFIERS }.ifEmpty { return null }
    val box = parts.first().coordinates.toNodeBox()
    val shapes = parts.map { part -> part.readShape(box = box, density = density) }.distinct()
    val apart = parts.any { !it.coordinates.toNodeBox().sameAs(other = box) } || shapes.size > 1
    if (apart || named(BACKGROUND_MODIFIER).size > 1 || named(BORDER_MODIFIER).size > 1) {
        refuse(reason = NotSendableReason.DecorationsApart)
    }
    val decoration = Decoration(
        background = named(BACKGROUND_MODIFIER).singleOrNull()?.readColor(),
        border = named(BORDER_MODIFIER).singleOrNull()?.readBorder(),
        shape = shapes.single(),
    )
    return DecoratedBox(decoration = decoration, box = box)
}

private fun InspectedModifier.readBorder(): Decoration.Border {
    val width = property(WIDTH_PROPERTY) as Dp
    return Decoration.Border(width = width.value, color = readColor())
}

private fun InspectedModifier.readColor(): DocumentColor {
    val color = property(COLOR_PROPERTY) as? Color
        ?: (property(BRUSH_PROPERTY) as? SolidColor)?.value
        ?: refuse(reason = NotSendableReason.Brush)
    return color.toDocumentColor()
}

internal fun Color.toDocumentColor(): DocumentColor = DocumentColor(argb = toArgb().toUInt())

private fun InspectedModifier.readShape(box: NodeBox, density: Density): Decoration.Shape =
    when (val shape = property(SHAPE_PROPERTY)) {
        RectangleShape -> Decoration.Shape.Rectangle
        is RoundedCornerShape -> shape.toRounded(size = box.size, density = density)
        else -> refuse(reason = NotSendableReason.UnknownShape)
    }

private fun RoundedCornerShape.toRounded(size: Size, density: Density): Decoration.Shape.Rounded {
    fun radius(corner: CornerSize): Float =
        corner.toPx(shapeSize = size, density = density) / density.density
    return Decoration.Shape.Rounded(
        topStart = radius(topStart),
        topEnd = radius(topEnd),
        bottomEnd = radius(bottomEnd),
        bottomStart = radius(bottomStart),
    )
}
