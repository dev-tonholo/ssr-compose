package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.document.internal.requireMeasure
import dev.tonholo.ssrcompose.core.document.internal.requireSlotSpaces
import kotlinx.serialization.Serializable

/**
 * The size in dp the device measured for the app component whose node id is [id], and the space it gives each slot.
 *
 * @throws IllegalArgumentException when a size is not finite, negative or above 100,000 dp, or [slots] holds more
 * than 118 slots or a slot name longer than 64 characters.
 */
@Serializable
public data class Measurement(
    val id: Int,
    val width: Float,
    val height: Float,
    val slots: Map<String, SlotSpace> = emptyMap(),
) {
    init {
        requireMeasure(value = width, what = "measured width")
        requireMeasure(value = height, what = "measured height")
        requireSlotSpaces(slots = slots)
    }
}
