package dev.tonholo.ssrcompose.compiler.testing

/**
 * A Kotlin source file given inline, with its [fileName] and its [contents].
 */
internal data class SourceText(val fileName: String, val contents: String)
