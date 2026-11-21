package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.document.internal.requireMeasure
import kotlinx.serialization.Serializable

/**
 * The space in dp an app component gives one of its slots. A null side is unbounded.
 *
 * @throws IllegalArgumentException when a side is not finite, negative or above 100,000 dp.
 */
@InternalSsrComposeApi
@Serializable
public data class SlotSpace(val maxWidth: Float?, val maxHeight: Float?) {
    init {
        maxWidth?.let { requireMeasure(value = it, what = "slot max width") }
        maxHeight?.let { requireMeasure(value = it, what = "slot max height") }
    }
}
