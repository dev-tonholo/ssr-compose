package dev.tonholo.ssrcompose.consumer

/**
 * Thrown when a binding reads a value of a placed component and the screen did not give what the binding asked for:
 * the value is absent or `None` for a read that needs one, it has another kind, or a choice entry is not in the enum.
 * The message names the component, the parameter, the expected kind and the found kind.
 */
public class PlacedValueException internal constructor(message: String) : RuntimeException(message)
