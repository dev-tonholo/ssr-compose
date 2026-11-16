package dev.tonholo.ssrcompose.core.catalog

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

private const val MAX_LENGTH = 64
private const val SURROGATE_PAIR_LENGTH = 2

/**
 * A label the app developer sets. It has no order: only equal or not equal matters.
 *
 * @throws IllegalArgumentException when the text is empty, over 64
 * characters, holds a control character or holds an unpaired surrogate.
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
        require(!value.hasUnpairedSurrogate()) { "A catalog version must not hold an unpaired surrogate" }
    }
}

private fun String.hasUnpairedSurrogate(): Boolean {
    var index = 0
    while (index < length) {
        val current = this[index]
        if (current.isSurrogate()) {
            val paired = current.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()
            if (!paired) {
                return true
            }
            index += SURROGATE_PAIR_LENGTH
        } else {
            index++
        }
    }
    return false
}
