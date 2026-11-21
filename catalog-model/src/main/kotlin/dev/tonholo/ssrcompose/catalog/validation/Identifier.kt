package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.core.identifier.isKotlinIdentifier

internal fun requireIdentifier(text: String, what: String) {
    require(isKotlinIdentifier(text = text)) {
        "$what must be a Kotlin identifier of at most ${CatalogLimits.MAX_NAME_LENGTH} characters, " +
            "was \"${text.cutForMessage()}\""
    }
}
