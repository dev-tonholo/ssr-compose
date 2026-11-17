package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.core.message.PlacedComponent

/**
 * What a binding receives: a placed component, read by parameter name.
 */
public class PlacedComponentScope internal constructor(internal val placed: PlacedComponent)
