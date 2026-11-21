package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.document.internal.requireSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How a box is drawn behind its content: a background, a border and a shape, sizes in dp.
 */
@InternalSsrComposeApi
@Serializable
public data class Decoration(
    val background: DocumentColor? = null,
    val border: Border? = null,
    val shape: Shape = Shape.Rectangle,
) {
    @Serializable
    public data class Border(val width: Float, val color: DocumentColor) {
        init {
            requireSize(value = width, what = "border width")
        }
    }

    @Serializable
    public sealed interface Shape {
        @Serializable
        @SerialName("Rectangle")
        public data object Rectangle : Shape

        @Serializable
        @SerialName("Rounded")
        public data class Rounded(
            val topStart: Float,
            val topEnd: Float,
            val bottomEnd: Float,
            val bottomStart: Float,
        ) : Shape {
            init {
                listOf(topStart, topEnd, bottomEnd, bottomStart).forEach { radius ->
                    requireSize(value = radius, what = "corner radius")
                }
            }
        }
    }
}
