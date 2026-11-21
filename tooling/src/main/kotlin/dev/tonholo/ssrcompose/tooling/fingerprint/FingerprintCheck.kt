package dev.tonholo.ssrcompose.tooling.fingerprint

import dev.tonholo.ssrcompose.catalog.bundle.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.tooling.StepException

/** Checks a file against the fingerprint it is pinned to: the lowercase hexadecimal SHA-256 of its bytes. */
public object FingerprintCheck {
    /**
     * @param subject what the file is, such as `font file brand/400-normal.ttf`. The failure names it.
     * @throws StepException when the SHA-256 of [bytes] is not [fingerprint].
     */
    public fun require(bytes: ByteArray, fingerprint: String, subject: String) {
        if (Fingerprint.of(bytes = bytes) != fingerprint) {
            throw StepException(message = "The $subject does not match its fingerprint $fingerprint")
        }
    }
}
