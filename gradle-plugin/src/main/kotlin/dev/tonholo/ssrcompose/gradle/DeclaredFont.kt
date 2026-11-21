package dev.tonholo.ssrcompose.gradle

/** One font file as the app declares it, with [file] relative to the module. */
internal data class DeclaredFont(
    val file: String,
    val weight: Int,
    val italic: Boolean,
)
