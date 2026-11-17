package dev.tonholo.ssrcompose.catalog.fragment

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import dev.tonholo.ssrcompose.catalog.validation.cutForMessage

private const val SEPARATOR = '/'
private const val DOT = "."
private const val FILE_NAME_SEPARATOR = ".."
private const val FILE_SUFFIX = ".fragment.json"
private val NOT_IN_A_RELATIVE_PATH = setOf('\\', ':')

/**
 * The marked composables of one source file, written by the compiler plugin and merged into a part later.
 *
 * @property source where the file is, by the folders of its package and its name, such as `app/home/Banner.kt`.
 * It is never a path of the machine that built it, so the same sources give the same fragment everywhere.
 * [isValidSource] says which sources are allowed.
 * @throws IllegalArgumentException when [source] is not valid.
 */
public data class CatalogFragment(public val source: String, public val components: List<ComponentDescriptor>) {
    /**
     * The name of the fragment's file. It is a plain file name of at most 255 bytes and differs for every
     * [source], on a file system that tells upper and lower case apart.
     */
    public val fileName: String = fileNameOf(source = source)

    init {
        require(isValidSource(source = source)) {
            "A fragment source must be a relative path of folders and a file name, with a fragment file name of " +
                "at most ${CatalogLimits.MAX_FRAGMENT_FILE_NAME_BYTES} bytes, was \"${source.cutForMessage()}\""
        }
    }

    public companion object {
        /**
         * True when [source] is not empty, is not absolute, holds no backslash or colon, has no segment that is
         * empty, starts or ends with a dot, or holds two dots in a row, and gives a file name of at most 255 bytes
         * in UTF-8.
         */
        public fun isValidSource(source: String): Boolean =
            source.isNotEmpty() &&
                source.none { it in NOT_IN_A_RELATIVE_PATH } &&
                source.split(SEPARATOR).all { segment -> segment.isPlainSegment() } &&
                fileNameOf(source = source).encodeToByteArray().size <= CatalogLimits.MAX_FRAGMENT_FILE_NAME_BYTES

        private fun fileNameOf(source: String): String =
            source.replace(oldValue = SEPARATOR.toString(), newValue = FILE_NAME_SEPARATOR) + FILE_SUFFIX
    }
}

private fun String.isPlainSegment(): Boolean =
    isNotEmpty() && !startsWith(DOT) && !endsWith(DOT) && !contains(FILE_NAME_SEPARATOR)
