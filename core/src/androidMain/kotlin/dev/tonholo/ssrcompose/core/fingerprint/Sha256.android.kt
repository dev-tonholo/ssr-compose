package dev.tonholo.ssrcompose.core.fingerprint

import java.security.MessageDigest

internal actual fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance(SHA256_ALGORITHM).digest(bytes)
