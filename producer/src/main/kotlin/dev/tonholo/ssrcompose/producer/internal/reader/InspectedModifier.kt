package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.semantics.SemanticsNode

internal const val BACKGROUND_MODIFIER = "background"
internal const val BORDER_MODIFIER = "border"
internal const val SCROLL_MODIFIER = "scroll"
internal const val PAINT_MODIFIER = "paint"

/** One modifier of a node, as its inspector info names it, with the coordinates it draws in. */
internal class InspectedModifier(
    val name: String,
    val properties: Map<String, Any?>,
    val coordinates: LayoutCoordinates,
) {
    fun property(name: String): Any? = properties[name]
}

internal fun SemanticsNode.inspectedModifiers(): List<InspectedModifier> =
    layoutInfo.getModifierInfo().mapNotNull { info ->
        val inspectable = info.modifier as? InspectableValue ?: return@mapNotNull null
        InspectedModifier(
            name = inspectable.nameFallback ?: return@mapNotNull null,
            properties = inspectable.properties(),
            coordinates = info.coordinates,
        )
    }

/** The inspector properties of a modifier with one element, to compare a node's modifiers against. */
internal fun Modifier.inspectedProperties(): Map<String, Any?> = (this as InspectableValue).properties()

private fun InspectableValue.properties(): Map<String, Any?> =
    inspectableElements.associate { element -> element.name to element.value }

internal fun List<InspectedModifier>.named(name: String): List<InspectedModifier> = filter { it.name == name }
