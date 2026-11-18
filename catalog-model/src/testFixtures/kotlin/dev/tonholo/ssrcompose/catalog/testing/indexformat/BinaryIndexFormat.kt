package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog

internal const val BINARY_MAGIC = 0x53535249
internal const val BINARY_FORMAT_VERSION = 1
internal const val KIND_TEXT = 0
internal const val KIND_WHOLE = 1
internal const val KIND_DECIMAL = 2
internal const val KIND_FLAG = 3
internal const val KIND_BYTES = 4
internal const val KIND_CHOICE = 5
internal const val KIND_LIST = 6
internal const val PARAMETER_VALUE = 0
internal const val PARAMETER_CALLBACK = 1
internal const val PARAMETER_SLOT = 2
internal const val FLAG_REQUIRED = 1
internal const val FLAG_NULLABLE = 2

/**
 * A binary index with a table of strings and a table of component offsets. Opening it reads the component names
 * only. The parameters of a component are decoded when that component is asked for.
 */
object BinaryIndexFormat : CandidateIndexFormat {
    override val name: String = "binary"

    override fun write(catalog: Catalog): ByteArray = BinaryIndexWriter(catalog = catalog).write()

    override fun open(bytes: ByteArray): CandidateIndex = BinaryIndex.open(bytes = bytes)
}
