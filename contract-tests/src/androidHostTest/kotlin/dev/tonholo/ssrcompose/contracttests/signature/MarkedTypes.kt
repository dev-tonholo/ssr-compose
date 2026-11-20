package dev.tonholo.ssrcompose.contracttests.signature

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi

/**
 * Types that need the opt-in marker, by Kotlin metadata name, such as `pkg/Outer.Inner`.
 * A type nested in a marked type is marked too.
 */
internal class MarkedTypes(private val names: Set<String>) {
    fun marks(typeName: String): Boolean =
        names.any { name -> typeName == name || typeName.startsWith(prefix = "$name.") }

    companion object {
        private val MARKER_DESCRIPTOR =
            "L${InternalSsrComposeApi::class.java.name.replace(oldChar = '.', newChar = '/')};"

        /**
         * Finds the classes that carry the marker. The marker is kept in the class file but is not visible to
         * reflection, so the class file is searched for the marker's descriptor.
         * A class that carries the marker only on a member also counts as marked, so the check can report too much.
         */
        fun readFrom(classes: List<Class<*>>): MarkedTypes =
            MarkedTypes(names = classes.filter(::carriesMarker).map { it.metadataName() }.toSet())

        private fun carriesMarker(type: Class<*>): Boolean {
            val classFile = checkNotNull(type.classLoader).getResourceAsStream(type.name.replace('.', '/') + ".class")
            val content = checkNotNull(classFile) { "No class file for $type" }.use { it.readBytes() }
            return String(content, Charsets.ISO_8859_1).contains(MARKER_DESCRIPTOR)
        }
    }
}
