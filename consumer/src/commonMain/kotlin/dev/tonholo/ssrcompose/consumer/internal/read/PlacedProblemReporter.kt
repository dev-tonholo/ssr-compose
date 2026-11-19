package dev.tonholo.ssrcompose.consumer.internal.read

import dev.tonholo.ssrcompose.core.message.PlacedComponent

/**
 * Hears about a placed component that a binding could not read, while a session shows it.
 */
internal fun interface PlacedProblemReporter {
    fun report(placed: PlacedComponent)
}
