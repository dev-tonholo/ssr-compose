package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import dev.tonholo.ssrcompose.catalog.validation.cutForMessage
import kotlin.jvm.JvmInline

private const val HYPHEN = '-'
private const val LAST_DEVICE_PORT = 9
private val RESERVED_DEVICE_NAMES = setOf("con", "prn", "aux", "nul") +
    (1..LAST_DEVICE_PORT).flatMap { port -> listOf("com$port", "lpt$port") }

/**
 * The name of one part of a catalog. The part's file name is built from it, so it is always a plain file name.
 *
 * @throws IllegalArgumentException when the text is empty, is over 64 characters, holds anything other than
 * lowercase letters, digits and hyphens, or is a name Windows keeps for a device, such as `con` or `com1`.
 */
@JvmInline
public value class PartName(public val value: String) {
    init {
        require(value.isNotEmpty()) { "A part name must not be empty" }
        require(value.length <= CatalogLimits.MAX_NAME_LENGTH) {
            "A part name must have at most ${CatalogLimits.MAX_NAME_LENGTH} characters"
        }
        require(value.all { it.isAllowed() }) {
            "A part name must hold only lowercase letters, digits and hyphens, was \"${value.cutForMessage()}\""
        }
        require(value !in RESERVED_DEVICE_NAMES) {
            "The part name \"$value\" is a name Windows keeps for a device, so set another one"
        }
    }
}

private fun Char.isAllowed(): Boolean = this in 'a'..'z' || this in '0'..'9' || this == HYPHEN
