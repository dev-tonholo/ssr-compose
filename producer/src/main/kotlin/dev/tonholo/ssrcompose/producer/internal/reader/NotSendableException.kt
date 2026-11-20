package dev.tonholo.ssrcompose.producer.internal.reader

/** Stops a read at the first content the producer cannot send. */
internal class NotSendableException(val reason: NotSendableReason, cause: Throwable? = null) :
    Exception(reason.description, cause)

internal fun refuse(reason: NotSendableReason, cause: Throwable? = null): Nothing =
    throw NotSendableException(reason = reason, cause = cause)
