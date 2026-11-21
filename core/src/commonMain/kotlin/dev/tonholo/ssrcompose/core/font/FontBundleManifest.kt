package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.font.internal.FONT_ORDER
import dev.tonholo.ssrcompose.core.font.internal.requireFamiliesDistinctIgnoringCase
import dev.tonholo.ssrcompose.core.font.internal.requireSortedAndDistinct

/**
 * The `fonts.json` of a font bundle: its [version] and its [fonts].
 *
 * @throws IllegalArgumentException when [fonts] repeats a family, weight and style, is not sorted by them, or holds
 * two families that differ only by case.
 */
public data class FontBundleManifest(
    val version: FontBundleVersion,
    val fonts: List<FontEntry>,
) {
    init {
        requireSortedAndDistinct(fonts = fonts)
        requireFamiliesDistinctIgnoringCase(fonts = fonts)
    }

    public companion object {
        /** The name of the file, next to the font files. */
        public const val FILE_NAME: String = "fonts.json"

        /**
         * The manifest of [fonts] given in any order.
         *
         * @throws IllegalArgumentException when [fonts] repeats a family, weight and style, or holds two families
         * that differ only by case.
         */
        public fun sortedOf(version: FontBundleVersion, fonts: List<FontEntry>): FontBundleManifest =
            FontBundleManifest(version = version, fonts = fonts.sortedWith(comparator = FONT_ORDER))
    }
}
