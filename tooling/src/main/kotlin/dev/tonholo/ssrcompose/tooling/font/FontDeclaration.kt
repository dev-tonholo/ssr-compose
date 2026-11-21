package dev.tonholo.ssrcompose.tooling.font

import java.io.File

/** One font file the app declares in `fonts { family(family) { font(file, weight, italic) } }`. */
public data class FontDeclaration(
    val family: String,
    val weight: Int,
    val italic: Boolean,
    val file: File,
)
