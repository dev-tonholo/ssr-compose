package dev.tonholo.ssrcompose.consumer.internal.device

import dev.tonholo.ssrcompose.core.document.Viewport
import dev.tonholo.ssrcompose.core.handshake.DeviceContext

/**
 * The window of this device in dp, the space a server screen gets until `SsrScreen` measures its own.
 */
internal fun DeviceContext.windowViewport(): Viewport =
    Viewport(width = screenWidthPx / density, height = screenHeightPx / density)
