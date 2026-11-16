package dev.tonholo.ssrcompose.core.testing

import dev.tonholo.ssrcompose.core.connection.Connection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One end of a connection that lives in memory, for tests. Create both ends with [pair].
 * Closing either end ends the connection. Frames sent before closing are still delivered.
 * [incoming] has one collector at a time.
 */
public class InMemoryConnection private constructor(
    private val inbox: Channel<ByteArray>,
    private val outbox: Channel<ByteArray>,
) : Connection {
    override val incoming: Flow<ByteArray> = inbox.receiveAsFlow()

    override suspend fun send(frame: ByteArray) {
        outbox.send(element = frame)
    }

    override suspend fun close() {
        outbox.close()
        inbox.close()
    }

    public companion object {
        public fun pair(): Pair<InMemoryConnection, InMemoryConnection> {
            val firstToSecond = Channel<ByteArray>(capacity = Channel.UNLIMITED)
            val secondToFirst = Channel<ByteArray>(capacity = Channel.UNLIMITED)
            return InMemoryConnection(inbox = secondToFirst, outbox = firstToSecond) to
                InMemoryConnection(inbox = firstToSecond, outbox = secondToFirst)
        }
    }
}
