package dev.tonholo.ssrcompose.catalog.bundle

import java.security.MessageDigest

private const val ALGORITHM = "SHA-256"

internal fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance(ALGORITHM).digest(bytes)
