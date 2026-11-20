package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.ActionId
import dev.tonholo.ssrcompose.core.document.internal.requirePlacement
import dev.tonholo.ssrcompose.core.document.internal.requireSize
import dev.tonholo.ssrcompose.core.document.internal.requireSwitcherSizes
import dev.tonholo.ssrcompose.core.document.internal.requireVariants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A node the producer composed, measured and placed. [x] and [y] are from the top-left corner of its parent and
 * [width] and [height] are its size, all in dp. [id] is unique in its document.
 * Every case throws [IllegalArgumentException] when a position is not finite or beyond 100,000 dp either way, or a
 * size is not finite or negative.
 */
@Serializable
public sealed interface DocumentNode {
    public val id: Int
    public val x: Float
    public val y: Float
    public val width: Float
    public val height: Float

    @Serializable
    @SerialName("Box")
    public data class Box(
        override val id: Int,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val decoration: Decoration? = null,
        val click: Click? = null,
        val semantics: NodeSemantics? = null,
        val children: List<DocumentNode> = emptyList(),
    ) : DocumentNode {
        init {
            requirePlacement(node = this)
        }
    }

    /** [maxLines], when given, is at least 1. */
    @Serializable
    @SerialName("Text")
    public data class Text(
        override val id: Int,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val text: String,
        val style: TextStyleSpec,
        val maxLines: Int? = null,
        val overflow: Overflow = Overflow.Clip,
        val click: Click? = null,
        val semantics: NodeSemantics? = null,
    ) : DocumentNode {
        init {
            requirePlacement(node = this)
            require(maxLines == null || maxLines >= 1) { "maxLines must be at least 1, was $maxLines" }
        }

        @Serializable
        public enum class Overflow {
            Clip,
            Ellipsis,
        }
    }

    /** Scrolls its content, of [contentWidth] by [contentHeight], on the device with no message. */
    @Serializable
    @SerialName("Scroll")
    public data class Scroll(
        override val id: Int,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val orientation: Orientation,
        val contentWidth: Float,
        val contentHeight: Float,
        val children: List<DocumentNode> = emptyList(),
    ) : DocumentNode {
        init {
            requirePlacement(node = this)
            requireSize(value = contentWidth, what = "content width")
            requireSize(value = contentHeight, what = "content height")
        }

        @Serializable
        public enum class Orientation {
            Vertical,
            Horizontal,
        }
    }

    /**
     * Shows one of its [variants]. Its box is the size of [selected], the variant the producer laid the screen out
     * with, and [largestWidth] by [largestHeight] is the size of its largest variant.
     *
     * @throws IllegalArgumentException when it has fewer than 2 or more than 16 variants, [selected] is not one of
     * them, or the largest size is smaller than its box or than a variant's extent.
     */
    @Serializable
    @SerialName("Switcher")
    public data class Switcher(
        override val id: Int,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val selected: Int,
        val largestWidth: Float,
        val largestHeight: Float,
        val variants: List<List<DocumentNode>>,
    ) : DocumentNode {
        init {
            requirePlacement(node = this)
            requireVariants(switcher = this)
            requireSwitcherSizes(switcher = this)
        }
    }

    /**
     * An app component, drawn by the app within [maxWidth] by [maxHeight] (null when unbounded). Its size is the one
     * the device last reported, or 0 before that. A parameter the screen did not give is absent from the maps.
     */
    @Serializable
    @SerialName("Component")
    public data class Component(
        override val id: Int,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val maxWidth: Float?,
        val maxHeight: Float?,
        val name: String,
        val values: Map<String, DataValue> = emptyMap(),
        val callbacks: Map<String, ActionId> = emptyMap(),
        val slots: Map<String, SlotContent> = emptyMap(),
    ) : DocumentNode {
        init {
            requirePlacement(node = this)
            maxWidth?.let { requireSize(value = it, what = "max width") }
            maxHeight?.let { requireSize(value = it, what = "max height") }
        }
    }
}
