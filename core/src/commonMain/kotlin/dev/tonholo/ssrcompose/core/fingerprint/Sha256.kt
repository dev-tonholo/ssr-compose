package dev.tonholo.ssrcompose.core.fingerprint

internal const val SHA256_ALGORITHM: String = "SHA-256"

internal expect fun sha256(bytes: ByteArray): ByteArray
