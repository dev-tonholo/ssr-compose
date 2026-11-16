package dev.tonholo.ssrcompose.catalog.bundle

import kotlin.jvm.JvmInline

private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_MEBIBYTES = 16

/**
 * The most bytes a manifest or a part may have. A file over its cap is refused before it is parsed.
 *
 * @throws IllegalArgumentException when [bytes] is negative.
 */
@JvmInline
public value class SizeCap(public val bytes: Int) {
    init {
        require(bytes >= 0) { "A size cap must not be negative" }
    }

    public companion object {
        /** 16 MiB for each file. */
        public val DEFAULT: SizeCap = SizeCap(bytes = DEFAULT_MEBIBYTES * BYTES_PER_MEBIBYTE)
    }
}
