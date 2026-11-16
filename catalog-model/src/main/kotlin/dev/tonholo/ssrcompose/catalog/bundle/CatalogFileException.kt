package dev.tonholo.ssrcompose.catalog.bundle

/**
 * Thrown when a catalog file cannot be read. Nothing is created from a file that fails.
 */
public class CatalogFileException(message: String, cause: Throwable? = null) : Exception(message, cause)
