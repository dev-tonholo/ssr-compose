package dev.tonholo.ssrcompose.producer.testing

import java.security.MessageDigest

/** The lowercase hexadecimal SHA-256 of [data], computed by the tests apart from the producer's own code. */
internal fun sha256Hex(data: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(data).joinToString(separator = "") { byte -> "%02x".format(byte) }
