package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.font.internal.requireFamily
import dev.tonholo.ssrcompose.core.font.internal.requireFileName
import dev.tonholo.ssrcompose.core.font.internal.requireFingerprint

/**
 * One font file of a bundle. [file] sits next to `fonts.json` and [fingerprint] is the SHA-256 of its bytes.
 *
 * @throws IllegalArgumentException when [family] is not a plain Kotlin identifier, is `default` or is a Windows
 * device name in any case, [weight] is outside 1 to 1,000, [file] is not
 * `<family>/<weight>-<normal or italic>.<ttf or otf>`, or [fingerprint] is not 64 lowercase hexadecimal digits.
 */
public data class FontEntry(
    val family: String,
    val weight: Int,
    val italic: Boolean,
    val file: String,
    val fingerprint: String,
) {
    init {
        requireFamily(family = family)
        require(weight in FONT_WEIGHTS) { "A font weight must be in $FONT_WEIGHTS, was $weight" }
        requireFileName(entry = this)
        requireFingerprint(fingerprint = fingerprint)
    }
}
