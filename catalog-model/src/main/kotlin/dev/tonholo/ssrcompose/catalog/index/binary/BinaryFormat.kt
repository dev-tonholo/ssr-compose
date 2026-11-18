package dev.tonholo.ssrcompose.catalog.index.binary

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
internal const val KNOWN_FLAGS = FLAG_REQUIRED or FLAG_NULLABLE

internal const val ACCEPTS_ANY = 0
internal const val ACCEPTS_CONSUMER = 1
internal const val ACCEPTS_PRODUCER = 2
