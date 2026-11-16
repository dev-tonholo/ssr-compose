package dev.tonholo.ssrcompose.core.catalog

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

private const val MAX_LENGTH = 64

/**
 * A label the app developer sets. It has no order: only equal or not equal matters.
 *
 * @throws IllegalArgumentException when the text is empty, over 64
 * characters or holds a control character.
 */
@Serializable
@JvmInline
public value class CatalogVersion(public val value: String) {
    init {
        require(value.isNotEmpty()) { "A catalog version must not be empty" }
        require(value.length <= MAX_LENGTH) {
            "A catalog version must have at most $MAX_LENGTH characters"
        }
        require(value.none { it.isISOControl() }) { "A catalog version must not hold a control character" }
    }
}
