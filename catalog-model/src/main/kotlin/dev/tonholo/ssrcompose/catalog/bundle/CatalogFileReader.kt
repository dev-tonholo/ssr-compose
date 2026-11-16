package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonDecodingException

internal object CatalogFileReader {
    fun read(text: String, maxLength: Int): Catalog {
        requireWithinLength(text = text, maxLength = maxLength)
        return try {
            requireKnownFormat(text = text)
            decodeDocument(text = text).toCatalog()
        } catch (exception: IllegalArgumentException) {
            throw CatalogFileException(message = "The catalog file is not valid: ${describe(exception = exception)}")
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun describe(exception: IllegalArgumentException): String = when (exception) {
        is MissingFieldException -> "it has no ${exception.missingFields.joinToString()}"
        is JsonDecodingException -> "a field is not known or has the wrong type${pathOf(exception = exception)}"
        is SerializationException -> "a field is not known or has the wrong type"
        else -> exception.message.orEmpty()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun pathOf(exception: JsonDecodingException): String = exception.path?.let { path -> " at $path" }.orEmpty()

    private fun requireWithinLength(text: String, maxLength: Int) {
        if (text.length > maxLength) {
            throw CatalogFileException(message = "The catalog file is longer than $maxLength characters")
        }
    }

    private fun requireKnownFormat(text: String) {
        val header = shapeJson.decodeFromString(deserializer = CatalogHeader.serializer(), string = text)
        if (header.format != Catalog.CURRENT_FORMAT) {
            throw CatalogFileException(message = "The catalog file format ${header.format} is not known")
        }
    }

    private fun decodeDocument(text: String): CatalogDocument {
        shapeJson.decodeFromString(deserializer = CatalogDocument.serializer(), string = text)
        return catalogJson.decodeFromString(deserializer = CatalogDocument.serializer(), string = text)
    }
}
