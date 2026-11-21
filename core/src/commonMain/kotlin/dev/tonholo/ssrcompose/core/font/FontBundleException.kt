package dev.tonholo.ssrcompose.core.font

/**
 * A font bundle file could not be read or written: it is over its size cap, or it is not a valid `fonts.json`.
 */
public class FontBundleException(message: String, cause: Throwable? = null) : Exception(message, cause)
