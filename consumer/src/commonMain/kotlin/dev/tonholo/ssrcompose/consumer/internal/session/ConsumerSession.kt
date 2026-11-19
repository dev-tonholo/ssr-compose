package dev.tonholo.ssrcompose.consumer.internal.session

import androidx.compose.runtime.snapshotFlow
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.internal.device.DeviceValues
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.connection.Connection
import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_WINDOW_PX = 1
private const val CONNECTION_ENDED = "The connection to the server ended. Please try again."
private const val SEND_CANCELLED = "The connection cancelled a send"
private const val UNREADABLE_MESSAGE = "The app could not read the answer from the server."

private val unreadableMessage = ScreenProblem.Refused(
    kind = RefusalKind.IntroductionNotValid,
    reason = UNREADABLE_MESSAGE,
)

/**
 * One app's conversation with the producer about one screen.
 *
 * [run] waits for a window size, opens a connection with [openConnection], introduces the app, opens [screen] and
 * publishes what the producer sends in [state]. It runs until its coroutine is cancelled, which closes the connection.
 * Decoding happens in [decodingContext], so a large screen does not hold up the caller's thread.
 */
internal class ConsumerSession(
    private val screen: String,
    private val catalogVersion: CatalogVersion,
    private val app: AppIdentity,
    private val deviceValues: DeviceValues,
    private val openConnection: suspend () -> Connection,
    private val codec: MessageCodec,
    private val decodingContext: CoroutineContext,
) {
    val state: StateFlow<SessionState>
        field = MutableStateFlow<SessionState>(SessionState.Connecting)

    private val retries = Channel<Unit>(capacity = Channel.CONFLATED)
    private var lastRevision = 0L

    /** Asks for the screen again, or introduces the app again when the last conversation ended. */
    fun retry() {
        retries.trySend(element = Unit)
    }

    /** Replaces the screen of [revision] with a mismatch of [component]. A newer screen stays. */
    fun reportMismatch(revision: Long, component: String) {
        state.update { current ->
            if (current is SessionState.Showing && current.revision == revision) {
                SessionState.Problem(problem = ScreenProblem.Mismatch(component = component))
            } else {
                current
            }
        }
    }

    suspend fun run(): Nothing {
        while (true) {
            state.update { SessionState.Connecting }
            val device = devicesWithWindow().first()
            val ending = SessionState.Problem(problem = converse(device = device))
            state.update { ending }
            retries.receive()
        }
    }

    private fun devicesWithWindow() = snapshotFlow { deviceValues.current }
        .filter { device -> device.screenWidthPx >= MIN_WINDOW_PX && device.screenHeightPx >= MIN_WINDOW_PX }

    /**
     * A connection that opens after the session is cancelled is closed without being used.
     * A retry asked for before the conversation ends is dropped; one asked for while the connection closes is kept.
     */
    private suspend fun converse(device: DeviceContext): ScreenProblem {
        var connection: Connection? = null
        try {
            return unavailableOnFailure {
                val opened = openConnection().also { opened -> connection = opened }
                currentCoroutineContext().ensureActive()
                talkOver(connection = opened, device = device)
            }
        } finally {
            retries.tryReceive()
            withContext(context = NonCancellable) { connection?.let { opened -> runCatching { opened.close() } } }
        }
    }

    private suspend fun talkOver(connection: Connection, device: DeviceContext) = coroutineScope {
        val conversation = Conversation(connection = connection, device = device, scope = this)
        val ending = conversation.talk()
        coroutineContext.cancelChildren()
        ending ?: ScreenProblem.Unavailable(reason = CONNECTION_ENDED)
    }

    /** Rethrows only the cancellation of the session itself. Any other failure means the connection ended. */
    private suspend inline fun unavailableOnFailure(block: () -> ScreenProblem): ScreenProblem =
        runCatching(block).getOrElse {
            currentCoroutineContext().ensureActive()
            ScreenProblem.Unavailable(reason = CONNECTION_ENDED)
        }

    private inner class Conversation(
        private val connection: Connection,
        private val device: DeviceContext,
        private val scope: CoroutineScope,
    ) {
        private var accepted = false

        /** Returns the problem that ended the conversation, or null when the connection ended by itself. */
        suspend fun talk(): ScreenProblem? {
            val introduction = Introduction(
                contractVersion = codec.contractVersion,
                catalogVersion = catalogVersion,
                app = app,
                device = device,
            )
            connection.send(
                frame = codec.encodeFirstFrame(introduce = ConsumerMessage.Introduce(introduction = introduction)),
            )
            return connection.incoming.map(transform = ::receive).filterNotNull().firstOrNull()
        }

        private suspend fun receive(frame: ByteArray): ScreenProblem? =
            when (val message = decode(frame = frame)) {
                null -> unreadableMessage
                is ProducerMessage.Answered -> onAnswer(answer = message.answer)
                is ProducerMessage.Screen -> whenAccepted { onScreen(message = message) }
                is ProducerMessage.ScreenUnavailable -> whenAccepted { onUnavailable(message = message) }
            }

        private suspend fun decode(frame: ByteArray) = withContext(context = decodingContext) {
            try {
                codec.decodeProducerMessage(frame = frame)
            } catch (_: MessageDecodingException) {
                null
            }
        }

        private suspend fun onAnswer(answer: Answer): ScreenProblem? = when (answer) {
            is Answer.Refused -> ScreenProblem.Refused(kind = answer.kind, reason = answer.reason)
            is Answer.Accepted -> when {
                answer.contractVersion != codec.contractVersion -> unreadableMessage
                accepted -> null
                else -> {
                    onAccepted()
                    null
                }
            }
        }

        /** A retry asked for while connecting is dropped, because this opens the screen anyway. */
        private suspend fun onAccepted() {
            accepted = true
            retries.tryReceive()
            openScreen()
            scope.launchSending { followDevice() }
            scope.launchSending { answerRetries() }
        }

        private suspend fun openScreen() {
            connection.send(frame = codec.encode(message = ConsumerMessage.OpenScreen(screen = screen)))
        }

        private suspend fun followDevice() {
            devicesWithWindow()
                .distinctUntilChanged()
                .dropWhile { current -> current == device }
                .collect { current ->
                    connection.send(frame = codec.encode(message = ConsumerMessage.DeviceChanged(device = current)))
                }
        }

        private suspend fun answerRetries() {
            while (true) {
                retries.receive()
                state.update { SessionState.Connecting }
                openScreen()
            }
        }

        private inline fun whenAccepted(handle: () -> Unit): ScreenProblem? {
            if (!accepted) return unreadableMessage
            handle()
            return null
        }

        private fun onScreen(message: ProducerMessage.Screen) {
            if (message.screen != screen) return
            lastRevision += 1
            val showing = SessionState.Showing(revision = lastRevision, content = message.content)
            state.update { showing }
        }

        private fun onUnavailable(message: ProducerMessage.ScreenUnavailable) {
            if (message.screen != screen) return
            val unavailable = SessionState.Problem(problem = ScreenProblem.Unavailable(reason = message.reason))
            state.update { unavailable }
        }
    }
}

/** A send that the connection cancels while the conversation goes on ends the conversation as a failure. */
private fun CoroutineScope.launchSending(send: suspend () -> Unit) = launch {
    try {
        send()
    } catch (cancellation: CancellationException) {
        currentCoroutineContext().ensureActive()
        throw IllegalStateException(SEND_CANCELLED, cancellation)
    }
}
