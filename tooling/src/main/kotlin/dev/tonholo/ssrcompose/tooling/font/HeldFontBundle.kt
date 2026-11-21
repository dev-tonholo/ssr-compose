package dev.tonholo.ssrcompose.tooling.font

/** One font bundle the server holds, declared as `fonts { bundle(version, location) }`. */
public data class HeldFontBundle(
    val version: String,
    val location: String,
)
