package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog

private const val BYTE_MASK = 0xFF
private const val HEX_RADIX = 16
private const val HEX_DIGITS_PER_BYTE = 2

/**
 * The fingerprint of a catalog file: the SHA-256 of its bytes, in lowercase hexadecimal.
 */
public object Fingerprint {
    public fun of(bytes: ByteArray): String = sha256(bytes = bytes).joinToString(separator = "") { byte ->
        (byte.toInt() and BYTE_MASK).toString(radix = HEX_RADIX).padStart(length = HEX_DIGITS_PER_BYTE, padChar = '0')
    }

    /** The fingerprint of the file [CatalogFile.write] gives for [catalog], as UTF-8 bytes. */
    public fun of(catalog: Catalog): String = of(bytes = CatalogFile.write(catalog = catalog).encodeToByteArray())
}
