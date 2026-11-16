package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

private const val INDENT = "  "
private const val CLASS_DISCRIMINATOR = "type"

private val strictPrimitives = SerializersModule {
    contextual(kClass = Int::class, serializer = WholeNumberSerializer)
    contextual(kClass = Boolean::class, serializer = FlagSerializer)
}

private val plainPrimitives = SerializersModule {
    contextual(kClass = Int::class, serializer = Int.serializer())
    contextual(kClass = Boolean::class, serializer = Boolean.serializer())
}

/**
 * The one [Json] that reads and writes the catalog file. Every option is written out so a library default cannot
 * change the bytes of a file or what the reader accepts. Numbers and flags are read strictly.
 */
@OptIn(ExperimentalSerializationApi::class)
internal val catalogJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = false
    isLenient = false
    prettyPrint = true
    prettyPrintIndent = INDENT
    coerceInputValues = false
    classDiscriminator = CLASS_DISCRIMINATOR
    classDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC
    useAlternativeNames = false
    namingStrategy = null
    decodeEnumsCaseInsensitive = false
    allowTrailingComma = false
    allowComments = false
    allowSpecialFloatingPointValues = false
    allowStructuredMapKeys = false
    useArrayPolymorphism = false
    serializersModule = strictPrimitives
    exceptionsWithDebugInfo = false
    maxNestingDepth = CatalogLimits.MAX_NESTING_DEPTH
}

/**
 * [catalogJson] with plain reads for numbers and flags.
 */
internal val shapeJson: Json = Json(from = catalogJson) {
    serializersModule = plainPrimitives
}
