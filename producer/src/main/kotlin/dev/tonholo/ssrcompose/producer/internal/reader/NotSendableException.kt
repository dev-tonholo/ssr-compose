package dev.tonholo.ssrcompose.producer.internal.reader

/** Stops a read at the first content the producer cannot send. */
internal class NotSendableException(val reason: NotSendableReason) : Exception(reason.description)

internal fun refuse(reason: NotSendableReason): Nothing = throw NotSendableException(reason = reason)
