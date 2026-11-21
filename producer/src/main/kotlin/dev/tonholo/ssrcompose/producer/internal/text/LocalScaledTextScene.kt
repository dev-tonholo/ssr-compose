package dev.tonholo.ssrcompose.producer.internal.text

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * What the producer gives each screen to lay its texts out as the device draws them. It is null outside a producer
 * screen, where `ScaledBasicText` lays text out as `BasicText` does.
 */
internal val LocalScaledTextScene: ProvidableCompositionLocal<ScaledTextScene?> =
    compositionLocalOf { null }
