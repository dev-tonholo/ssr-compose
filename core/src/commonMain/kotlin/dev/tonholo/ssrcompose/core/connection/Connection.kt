package dev.tonholo.ssrcompose.core.connection

import kotlinx.coroutines.flow.Flow

/**
 * A way to move frames between consumer and producer. It delivers frames in order and without loss, or it ends.
 */
public interface Connection {
    /** Frames from the other end, in order. The flow completes when the connection ends. */
    public val incoming: Flow<ByteArray>

    public suspend fun send(frame: ByteArray)

    public suspend fun close()
}
