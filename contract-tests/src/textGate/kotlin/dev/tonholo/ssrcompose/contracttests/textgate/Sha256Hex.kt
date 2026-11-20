package dev.tonholo.ssrcompose.contracttests.textgate

import java.security.MessageDigest
import java.util.Locale

object Sha256Hex {
    private const val ALGORITHM = "SHA-256"
    private const val HEX_FORMAT = "%02x"

    fun of(bytes: ByteArray): String = MessageDigest.getInstance(ALGORITHM)
        .digest(bytes)
        .joinToString(separator = "") { String.format(Locale.ROOT, HEX_FORMAT, it) }
}
