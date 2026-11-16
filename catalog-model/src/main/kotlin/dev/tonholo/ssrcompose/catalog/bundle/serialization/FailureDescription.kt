package dev.tonholo.ssrcompose.catalog.bundle.serialization

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.JsonDecodingException

/**
 * What went wrong, in words that never repeat text from the file.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun describe(exception: IllegalArgumentException): String = when (exception) {
    is MissingFieldException -> "it has no ${exception.missingFields.joinToString()}"
    is JsonDecodingException -> "a field is not known or has the wrong type${pathOf(exception = exception)}"
    else -> exception.message.orEmpty()
}

internal fun describe(exception: CatalogFileException): String = exception.message.orEmpty()

@OptIn(ExperimentalSerializationApi::class)
private fun pathOf(exception: JsonDecodingException): String = exception.path?.let { path -> " at $path" }.orEmpty()
