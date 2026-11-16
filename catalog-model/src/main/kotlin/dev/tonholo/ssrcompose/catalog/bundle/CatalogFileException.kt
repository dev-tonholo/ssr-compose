package dev.tonholo.ssrcompose.catalog.bundle

/**
 * Thrown when a manifest, a part or a bundle cannot be read. Nothing is created from a file that fails.
 */
public class CatalogFileException(message: String) : Exception(message)
