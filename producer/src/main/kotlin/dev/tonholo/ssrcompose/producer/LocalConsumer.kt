package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Provides the [Consumer] of the open producer screen.
 */
val LocalConsumer: ProvidableCompositionLocal<Consumer> = staticCompositionLocalOf {
    error("LocalConsumer was read outside a producer screen")
}
