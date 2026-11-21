package dev.tonholo.ssrcompose.core.font.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

private const val INDENT = "  "

/** The [Json] that writes font bundle files: fixed key order, two-space indent. */
@OptIn(ExperimentalSerializationApi::class)
internal val fontJson: Json = Json {
    prettyPrint = true
    prettyPrintIndent = INDENT
}
