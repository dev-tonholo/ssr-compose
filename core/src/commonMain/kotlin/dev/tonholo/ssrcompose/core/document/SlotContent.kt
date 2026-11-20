package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.document.internal.requireSize
import kotlinx.serialization.Serializable

/**
 * The server content of one slot of an app component, laid out by the producer in the space the device reported
 * for the slot. [nodes] are placed from the slot's top-left corner. Sizes are in dp.
 */
@Serializable
public data class SlotContent(
    val width: Float,
    val height: Float,
    val nodes: List<DocumentNode>,
) {
    init {
        requireSize(value = width, what = "slot width")
        requireSize(value = height, what = "slot height")
    }
}
