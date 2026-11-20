package dev.tonholo.ssrcompose.contracttests.signature

/**
 * Types that need the opt-in marker, by Kotlin metadata name, such as `pkg/Outer.Inner`.
 * A type nested in a marked type is marked too.
 */
internal class MarkedTypes(private val names: Set<String>) {
    fun marks(typeName: String): Boolean =
        names.any { name -> typeName == name || typeName.startsWith(prefix = "$name.") }
}
