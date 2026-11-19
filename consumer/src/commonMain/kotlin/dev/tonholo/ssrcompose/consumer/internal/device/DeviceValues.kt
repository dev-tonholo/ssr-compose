package dev.tonholo.ssrcompose.consumer.internal.device

import dev.tonholo.ssrcompose.core.handshake.DeviceContext

/**
 * Where the consumer reads what it knows about the device. The session reads [current] for the introduction
 * and again whenever it changes.
 *
 * The window size in [current] is zero before the first layout. The producer accepts 1 to 100,000,
 * so wait for a positive size before the introduction.
 */
internal interface DeviceValues {
    val current: DeviceContext
}
