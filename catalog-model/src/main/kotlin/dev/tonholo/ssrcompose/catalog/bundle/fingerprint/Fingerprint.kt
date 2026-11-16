package dev.tonholo.ssrcompose.catalog.bundle.fingerprint

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
}
