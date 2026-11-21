package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.document.internal.requireViewportSide
import kotlinx.serialization.Serializable

/**
 * The space in dp the app gives a server screen.
 *
 * @throws IllegalArgumentException when a side is not above 0 or is above 100,000 dp.
 */
@InternalSsrComposeApi
@Serializable
public data class Viewport(val width: Float, val height: Float) {
    init {
        requireViewportSide(value = width, what = "viewport width")
        requireViewportSide(value = height, what = "viewport height")
    }
}
