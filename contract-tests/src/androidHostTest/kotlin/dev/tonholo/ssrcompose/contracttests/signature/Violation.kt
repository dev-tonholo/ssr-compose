package dev.tonholo.ssrcompose.contracttests.signature

/**
 * A public declaration whose signature names a marked type.
 *
 * @property declaration the declaring class and the declaration name, such as `pkg/Owner.function`.
 * @property markedType the marked type the signature names.
 */
internal data class Violation(val declaration: String, val markedType: String)
