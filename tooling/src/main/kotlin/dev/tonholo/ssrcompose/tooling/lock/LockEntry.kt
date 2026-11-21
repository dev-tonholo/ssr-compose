package dev.tonholo.ssrcompose.tooling.lock

/**
 * One held version in a lock file: where it was fetched from, as declared, and the fingerprint of the file that
 * covers the whole bundle (a catalog's manifest or a font bundle's `fonts.json`).
 */
public data class LockEntry(
    val version: String,
    val location: String,
    val fingerprint: String,
)
