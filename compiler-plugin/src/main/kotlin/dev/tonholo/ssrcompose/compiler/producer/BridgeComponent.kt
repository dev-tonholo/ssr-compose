package dev.tonholo.ssrcompose.compiler.producer

/** A component of a held catalog, in the shape a created bridge function can take. */
internal data class BridgeComponent(
    val name: String,
    val parameters: List<BridgeParameter>,
)

internal data class BridgeParameter(
    val name: String,
    val kind: BridgeParameterKind,
    val isRequired: Boolean,
)

/** The parameter shapes the plugin can create so far. Every other shape in a catalog is a compile error. */
internal enum class BridgeParameterKind {
    Text,
    ActionOfText,
    Slot,
    SlotOfText,
}
