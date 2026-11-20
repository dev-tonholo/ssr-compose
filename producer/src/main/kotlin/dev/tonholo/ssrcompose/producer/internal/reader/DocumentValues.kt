package dev.tonholo.ssrcompose.producer.internal.reader

/**
 * Builds a value of the document, such as a node or a border.
 *
 * @throws NotSendableException with [NotSendableReason.OutOfBounds] when the document refuses the value, for example
 * for a negative size.
 */
internal inline fun <T> documentValue(build: () -> T): T = try {
    build()
} catch (invalid: IllegalArgumentException) {
    refuse(reason = NotSendableReason.OutOfBounds, cause = invalid)
}
