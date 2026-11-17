package dev.tonholo.ssrcompose.compiler.producer.throwaway

/** The index of one held catalog in the throwaway format of the bridge experiment. */
internal data class ThrowawayIndex(val components: List<ThrowawayComponent>)

internal data class ThrowawayComponent(
    val name: String,
    val parameters: List<ThrowawayParameter>,
)

internal data class ThrowawayParameter(
    val name: String,
    val kind: ThrowawayParameterKind,
    val isRequired: Boolean,
)

internal enum class ThrowawayParameterKind(val word: String, val canBeOptional: Boolean) {
    Text(word = "text", canBeOptional = true),
    ActionOfText(word = "action-of-text", canBeOptional = false),
    Slot(word = "slot", canBeOptional = true),
    SlotOfText(word = "slot-of-text", canBeOptional = false),
}
