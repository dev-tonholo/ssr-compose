package dev.tonholo.ssrcompose.consumer.testing

import android.os.Looper
import dev.tonholo.ssrcompose.core.connection.Connection
import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import dev.tonholo.ssrcompose.core.message.ScreenContent
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

/** Answers like a producer: it accepts or refuses the introduction and sends [content] for each `OpenScreen`. */
class FakeConnection(
    private val content: ScreenContent,
    private val refusal: Answer.Refused? = null,
) : Connection {
    private val codec = MessageCodec.json()
    private val inbox = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private var introduced = false

    @Volatile
    var closed = false
        private set

    private val offMainFrames = AtomicInteger(0)

    /** How many frames the session took from [incoming] on a thread other than the main thread. */
    val framesTakenOffMain: Int get() = offMainFrames.get()

    override val incoming: Flow<ByteArray> = inbox.receiveAsFlow().onEach {
        if (Looper.myLooper() != Looper.getMainLooper()) offMainFrames.incrementAndGet()
    }

    /** Rejects a frame the codec cannot read by throwing its [MessageDecodingException]. */
    override suspend fun send(frame: ByteArray) {
        if (introduced) answer(message = codec.decodeConsumerMessage(frame = frame)) else introduce(firstFrame = frame)
    }

    private fun introduce(firstFrame: ByteArray) {
        codec.decodeFirstFrame(frame = firstFrame)
        introduced = true
        val accepted = Answer.Accepted(contractVersion = codec.contractVersion)
        deliver(message = ProducerMessage.Answered(answer = refusal ?: accepted))
    }

    override suspend fun close() {
        closed = true
        inbox.close()
    }

    fun deliver(message: ProducerMessage) {
        inbox.trySend(element = codec.encode(message = message))
    }

    private fun answer(message: ConsumerMessage) {
        if (message is ConsumerMessage.OpenScreen) {
            deliver(message = ProducerMessage.Screen(screen = message.screen, content = content))
        }
    }
}
