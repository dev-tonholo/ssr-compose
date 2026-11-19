@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.tonholo.ssrcompose.consumer.internal.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.internal.device.DeviceValues
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.connection.Connection
import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import dev.tonholo.ssrcompose.core.handshake.Theme
import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import dev.tonholo.ssrcompose.core.message.ScreenContent
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val SCREEN = "checkout"
private const val OTHER_SCREEN = "profile"
private const val COMPONENT = "Greeting"
private const val OTHER_COMPONENT = "Banner"
private const val REASON = "Please update the app"
private const val UNAVAILABLE_REASON = "The screen is resting"
private const val STALE_REVISION_OFFSET = 1L
private const val LARGE_SCREEN_COMPONENTS = 10_000
private const val FOREIGN_CANCELLATION = "The transport cancelled its read"

private val CONTRACT_VERSION_OF_JSON_CODEC = ContractVersion(value = 1)
private val CATALOG_VERSION = CatalogVersion(value = "3")
private val APP = AppIdentity(version = "2.4.0", variant = "release", flavor = "play")
private val DEVICE = DeviceContext(
    density = 2.5f,
    screenWidthPx = 1080,
    screenHeightPx = 2400,
    theme = Theme.Dark,
    language = "pt-BR",
)
private val NO_WINDOW = DEVICE.copy(screenWidthPx = 0, screenHeightPx = 0)
private val ROTATED = DEVICE.copy(screenWidthPx = 2400, screenHeightPx = 1080)
private val ACCEPTED = ProducerMessage.Answered(
    answer = Answer.Accepted(contractVersion = CONTRACT_VERSION_OF_JSON_CODEC),
)
private val REFUSED = ProducerMessage.Answered(
    answer = Answer.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = REASON),
)
private val CONTENT = ScreenContent(components = listOf(PlacedComponent(name = COMPONENT)))
private val OTHER_CONTENT = ScreenContent(components = listOf(PlacedComponent(name = OTHER_COMPONENT)))
private val UNREADABLE_FRAME = byteArrayOf(1, 2, 3)

private class FakeDeviceValues(initial: DeviceContext) : DeviceValues {
    override var current: DeviceContext by mutableStateOf(initial)
}

private class FakeConnection(
    private val failOnSend: Boolean = false,
    private val failOnClose: Boolean = false,
    private val closing: CompletableDeferred<Unit>? = null,
) : Connection {
    private val codec = MessageCodec.json()
    private val inbox = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    val sent = mutableListOf<ByteArray>()
    var closed = false
        private set
    var sendFailure: Throwable? = null

    override val incoming: Flow<ByteArray> = inbox.receiveAsFlow()

    override suspend fun send(frame: ByteArray) {
        check(!failOnSend) { "The connection failed" }
        sendFailure?.let { throw it }
        sent += frame
    }

    override suspend fun close() {
        closing?.await()
        closed = true
        inbox.close()
        check(!failOnClose) { "The connection failed to close" }
    }

    fun deliver(message: ProducerMessage) {
        inbox.trySend(element = codec.encode(message = message))
    }

    fun deliverFrame(frame: ByteArray) {
        inbox.trySend(element = frame)
    }

    fun end() {
        inbox.close()
    }

    fun endWith(cause: Throwable) {
        inbox.close(cause = cause)
    }

    fun introduction(): Introduction = codec.decodeFirstFrame(frame = sent.first()).introduction

    fun messagesAfterIntroduction(): List<ConsumerMessage> =
        sent.drop(n = 1).map { frame -> codec.decodeConsumerMessage(frame = frame) }
}

private enum class Opening { AtOnce, Cancellable, IgnoringCancellation }

private class Harness(
    val scope: TestScope,
    device: DeviceContext = DEVICE,
    private val connections: MutableList<FakeConnection> = mutableListOf(FakeConnection()),
    private val openFailure: Throwable? = null,
    private val opening: Opening = Opening.AtOnce,
) {
    private var pendingOpen: Continuation<Connection>? = null
    var openCancelled = false
        private set
    val deviceValues = FakeDeviceValues(initial = device)
    var opened = 0
        private set
    val session = ConsumerSession(
        screen = SCREEN,
        catalogVersion = CATALOG_VERSION,
        app = APP,
        deviceValues = deviceValues,
        openConnection = {
            openFailure?.let { throw it }
            when (opening) {
                Opening.AtOnce -> connections[opened++]
                Opening.Cancellable -> suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { openCancelled = true }
                    pendingOpen = continuation
                }
                Opening.IgnoringCancellation -> suspendCoroutine { continuation -> pendingOpen = continuation }
            }
        },
        codec = MessageCodec.json(),
        decodingContext = UnconfinedTestDispatcher(scheduler = scope.testScheduler),
    )
    val connection: FakeConnection get() = connections.first()

    fun start(): Job {
        val dispatcher = UnconfinedTestDispatcher(scheduler = scope.testScheduler)
        return scope.backgroundScope.launch(context = dispatcher) { session.run() }
    }

    fun finishOpening() {
        checkNotNull(pendingOpen).resume(value = connections[opened++])
        scope.runCurrent()
    }

    fun changeDevice(device: DeviceContext) {
        deviceValues.current = device
        Snapshot.sendApplyNotifications()
        scope.runCurrent()
    }

    fun accept() {
        connection.deliver(message = ACCEPTED)
        scope.runCurrent()
    }

    fun deliverUnavailable(screen: String = SCREEN) {
        connection.deliver(message = ProducerMessage.ScreenUnavailable(screen = screen, reason = UNAVAILABLE_REASON))
        scope.runCurrent()
    }

    fun deliverScreen(content: ScreenContent = CONTENT, screen: String = SCREEN) {
        connection.deliver(message = ProducerMessage.Screen(screen = screen, content = content))
        scope.runCurrent()
    }
}

private fun Harness.showing(): SessionState.Showing = assertIs<SessionState.Showing>(value = session.state.value)

private fun Harness.problem(): ScreenProblem = assertIs<SessionState.Problem>(value = session.state.value).problem

class ConsumerSessionTest {
    @Test
    fun `given a window, when the session starts, then the first frame introduces the app`() = runTest {
        // Arrange
        val harness = Harness(scope = this)

        // Act
        harness.start()
        runCurrent()

        // Assert
        val expected = Introduction(
            contractVersion = CONTRACT_VERSION_OF_JSON_CODEC,
            catalogVersion = CATALOG_VERSION,
            app = APP,
            device = DEVICE,
        )
        assertEquals(expected = expected, actual = harness.connection.introduction())
    }

    @Test
    fun `given a window size of zero, when the session starts, then it opens no connection`() = runTest {
        // Arrange
        val harness = Harness(scope = this, device = NO_WINDOW)

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertEquals(expected = 0, actual = harness.opened)
        assertEquals(expected = SessionState.Connecting, actual = harness.session.state.value)
    }

    @Test
    fun `given a window of zero width only, when the session starts, then it opens no connection`() = runTest {
        // Arrange
        val harness = Harness(scope = this, device = DEVICE.copy(screenWidthPx = 0))

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertEquals(expected = 0, actual = harness.opened)
    }

    @Test
    fun `given a window of zero height only, when the session starts, then it opens no connection`() = runTest {
        // Arrange
        val harness = Harness(scope = this, device = DEVICE.copy(screenHeightPx = 0))

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertEquals(expected = 0, actual = harness.opened)
    }

    @Test
    fun `given a window that gets its size, when the size arrives, then the app is introduced with it`() = runTest {
        // Arrange
        val harness = Harness(scope = this, device = NO_WINDOW)
        harness.start()
        runCurrent()

        // Act
        harness.changeDevice(device = DEVICE)

        // Assert
        assertEquals(expected = DEVICE, actual = harness.connection.introduction().device)
    }

    @Test
    fun `given no answer yet, when the session waits, then it sends nothing after the introduction`() = runTest {
        // Arrange
        val harness = Harness(scope = this)

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertEquals(expected = 1, actual = harness.connection.sent.size)
    }

    @Test
    fun `given an accepted app, when the answer arrives, then the session opens the screen`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.accept()

        // Assert
        assertEquals(
            expected = listOf<ConsumerMessage>(ConsumerMessage.OpenScreen(screen = SCREEN)),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }

    @Test
    fun `given an open screen, when each Screen arrives, then the state shows its content`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.deliverScreen(content = CONTENT)
        val first = harness.showing()
        harness.deliverScreen(content = OTHER_CONTENT)
        val second = harness.showing()

        // Assert
        assertEquals(expected = CONTENT, actual = first.content)
        assertEquals(expected = OTHER_CONTENT, actual = second.content)
        assertTrue(actual = second.revision > first.revision)
    }

    @Test
    fun `given an open screen, when a Screen of another name arrives, then the state does not change`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen(content = CONTENT)

        // Act
        harness.deliverScreen(content = OTHER_CONTENT, screen = OTHER_SCREEN)

        // Assert
        assertEquals(expected = CONTENT, actual = harness.showing().content)
    }

    @Test
    fun `given no answer yet, when a Screen arrives, then the session ends as refused`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.deliverScreen()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
        assertTrue(actual = harness.connection.closed)
    }

    @Test
    fun `given a refusal, when it arrives, then the state holds it and the connection closes`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.connection.deliver(message = REFUSED)
        runCurrent()

        // Assert
        assertEquals(
            expected = ScreenProblem.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = REASON),
            actual = harness.problem(),
        )
        assertTrue(actual = harness.connection.closed)
    }

    @Test
    fun `given a frame the app cannot read, when it arrives, then the session ends as refused`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.connection.deliverFrame(frame = UNREADABLE_FRAME)
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
        assertTrue(actual = harness.connection.closed)
    }

    @Test
    fun `given an answer with another contract version, when it arrives, then the session ends as refused`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        val otherVersion = ContractVersion(value = CONTRACT_VERSION_OF_JSON_CODEC.value + 1)

        // Act
        harness.connection.deliver(
            message = ProducerMessage.Answered(answer = Answer.Accepted(contractVersion = otherVersion)),
        )
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
    }

    @Test
    fun `given no answer yet, when the connection ends, then the screen is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.connection.end()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given an accepted app, when the connection ends before a Screen, then the screen is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.connection.end()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given a shown screen, when the connection ends, then the screen is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen()

        // Act
        harness.connection.end()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given a failing connection, when the app is introduced, then the screen is unavailable`() = runTest {
        // Arrange
        val failing = FakeConnection(failOnSend = true)
        val harness = Harness(scope = this, connections = mutableListOf(failing))

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
        assertTrue(actual = failing.closed)
    }

    @Test
    fun `given a connection that cannot open, when the session starts, then the screen is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this, openFailure = IllegalStateException("No network"))

        // Act
        harness.start()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given an open screen, when ScreenUnavailable arrives, then the state holds its reason`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.deliverUnavailable()

        // Assert
        assertEquals(expected = ScreenProblem.Unavailable(reason = UNAVAILABLE_REASON), actual = harness.problem())
    }

    @Test
    fun `given an unavailable screen, when a Screen arrives, then the session shows it`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverUnavailable()

        // Act
        harness.deliverScreen(content = CONTENT)

        // Assert
        assertEquals(expected = CONTENT, actual = harness.showing().content)
    }

    @Test
    fun `given an accepted app, when a device value changes, then the producer is told`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.changeDevice(device = ROTATED)

        // Assert
        assertEquals(
            expected = ConsumerMessage.DeviceChanged(device = ROTATED),
            actual = harness.connection.messagesAfterIntroduction().last(),
        )
    }

    @Test
    fun `given no answer yet, when a device value changes, then the producer is not told`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.changeDevice(device = ROTATED)

        // Assert
        assertEquals(expected = 1, actual = harness.connection.sent.size)
    }

    @Test
    fun `given an accepted app, when the window size drops to zero, then the producer is not told`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.changeDevice(device = NO_WINDOW)

        // Assert
        assertEquals(
            expected = listOf<ConsumerMessage>(ConsumerMessage.OpenScreen(screen = SCREEN)),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }

    @Test
    fun `given a handshake in progress, when the session is cancelled, then the connection closes`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        val running = harness.start()
        runCurrent()

        // Act
        running.cancel()
        runCurrent()

        // Assert
        assertTrue(actual = harness.connection.closed)
    }

    @Test
    fun `given a refusal, when the user retries, then the app is introduced again on a new connection`() = runTest {
        // Arrange
        val second = FakeConnection()
        val harness = Harness(scope = this, connections = mutableListOf(FakeConnection(), second))
        harness.start()
        runCurrent()
        harness.connection.deliver(message = REFUSED)
        runCurrent()

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        assertEquals(expected = DEVICE, actual = second.introduction().device)
        assertEquals(expected = SessionState.Connecting, actual = harness.session.state.value)
    }

    @Test
    fun `given an unavailable screen, when the user retries, then the same connection asks again`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverUnavailable()

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        val openScreen = ConsumerMessage.OpenScreen(screen = SCREEN)
        assertEquals(
            expected = listOf<ConsumerMessage>(openScreen, openScreen),
            actual = harness.connection.messagesAfterIntroduction(),
        )
        assertEquals(expected = 1, actual = harness.opened)
        assertEquals(expected = SessionState.Connecting, actual = harness.session.state.value)
    }

    @Test
    fun `given a shown screen, when a mismatch is reported for it, then the state holds the component`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen()

        // Act
        harness.session.reportMismatch(revision = harness.showing().revision, component = COMPONENT)

        // Assert
        assertEquals(expected = ScreenProblem.Mismatch(component = COMPONENT), actual = harness.problem())
    }

    @Test
    fun `given a newer screen, when a mismatch is reported for the older one, then the newer screen stays`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen(content = CONTENT)
        val stale = harness.showing().revision
        harness.deliverScreen(content = OTHER_CONTENT)

        // Act
        harness.session.reportMismatch(revision = stale, component = COMPONENT)

        // Assert
        assertEquals(expected = OTHER_CONTENT, actual = harness.showing().content)
        assertTrue(actual = harness.showing().revision >= stale + STALE_REVISION_OFFSET)
    }

    @Test
    fun `given a mismatch, when the next Screen arrives, then the state shows it`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen(content = CONTENT)
        harness.session.reportMismatch(revision = harness.showing().revision, component = COMPONENT)

        // Act
        harness.deliverScreen(content = OTHER_CONTENT)

        // Assert
        assertEquals(expected = OTHER_CONTENT, actual = harness.showing().content)
    }

    @Test
    fun `given a mismatch, when the user retries, then the screen is asked for again`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen(content = CONTENT)
        harness.session.reportMismatch(revision = harness.showing().revision, component = COMPONENT)

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        val openScreen = ConsumerMessage.OpenScreen(screen = SCREEN)
        assertEquals(
            expected = listOf<ConsumerMessage>(openScreen, openScreen),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }

    @Test
    fun `given an accepted app, when Accepted arrives again, then the screen is opened once`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.accept()

        // Assert
        assertEquals(
            expected = listOf<ConsumerMessage>(ConsumerMessage.OpenScreen(screen = SCREEN)),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }

    @Test
    fun `given no answer yet, when ScreenUnavailable arrives, then the session ends as refused`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.deliverUnavailable()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
    }

    @Test
    fun `given a shown screen, when ScreenUnavailable of another name arrives, then the screen stays`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen(content = CONTENT)

        // Act
        harness.deliverUnavailable(screen = OTHER_SCREEN)

        // Assert
        assertEquals(expected = CONTENT, actual = harness.showing().content)
    }

    @Test
    fun `given a screen with thousands of components, when it arrives, then the state holds all of them`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()
        val large = ScreenContent(
            components = List(size = LARGE_SCREEN_COMPONENTS) { index -> PlacedComponent(name = "Item$index") },
        )

        // Act
        harness.deliverScreen(content = large)

        // Assert
        assertEquals(expected = large, actual = harness.showing().content)
    }

    @Test
    fun `given a retry during the handshake, when the app is refused, then the session waits for the user`() = runTest {
        // Arrange
        val harness = Harness(scope = this, connections = mutableListOf(FakeConnection(), FakeConnection()))
        harness.start()
        runCurrent()
        harness.session.retry()

        // Act
        harness.connection.deliver(message = REFUSED)
        runCurrent()

        // Assert
        assertEquals(expected = 1, actual = harness.opened)
        assertIs<ScreenProblem.Refused>(value = harness.problem())
    }

    @Test
    fun `given a connection that fails to close, when the app is refused, then it stays refused`() = runTest {
        // Arrange
        val failing = FakeConnection(failOnClose = true)
        val harness = Harness(scope = this, connections = mutableListOf(failing))
        harness.start()
        runCurrent()

        // Act
        failing.deliver(message = REFUSED)
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
    }

    @Test
    fun `given an accepted app, when a refusal arrives, then the session ends as refused`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.connection.deliver(message = REFUSED)
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Refused>(value = harness.problem())
        assertTrue(actual = harness.connection.closed)
    }

    @Test
    fun `given a window that hides and returns, when it returns, then the producer is told once`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.changeDevice(device = ROTATED)
        harness.changeDevice(device = NO_WINDOW)
        harness.changeDevice(device = ROTATED)

        // Assert
        val changes = harness.connection.messagesAfterIntroduction().filterIsInstance<ConsumerMessage.DeviceChanged>()
        assertEquals(expected = listOf(ConsumerMessage.DeviceChanged(device = ROTATED)), actual = changes)
    }

    @Test
    fun `given no answer yet, when the connection ends in a cancellation, then the screen is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()

        // Act
        harness.connection.endWith(cause = CancellationException(FOREIGN_CANCELLATION))
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given an accepted app, when the connection ends in a cancellation, then it is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.accept()

        // Act
        harness.connection.endWith(cause = CancellationException(FOREIGN_CANCELLATION))
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
    }

    @Test
    fun `given a foreign cancellation, when the user retries, then the app is introduced again`() = runTest {
        // Arrange
        val second = FakeConnection()
        val harness = Harness(scope = this, connections = mutableListOf(FakeConnection(), second))
        harness.start()
        runCurrent()
        harness.connection.endWith(cause = CancellationException(FOREIGN_CANCELLATION))
        runCurrent()

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        assertEquals(expected = DEVICE, actual = second.introduction().device)
    }

    @Test
    fun `given a handshake in progress, when the session is cancelled, then it ends without a problem`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        val running = harness.start()
        runCurrent()

        // Act
        running.cancel()
        runCurrent()

        // Assert
        assertFalse(actual = running.isActive)
        assertEquals(expected = SessionState.Connecting, actual = harness.session.state.value)
    }

    @Test
    fun `given a connection opening, when the session is cancelled, then the opening is cancelled`() = runTest {
        // Arrange
        val harness = Harness(scope = this, opening = Opening.Cancellable)
        val running = harness.start()
        runCurrent()

        // Act
        running.cancel()
        runCurrent()

        // Assert
        assertTrue(actual = harness.openCancelled)
        assertTrue(actual = running.isCompleted)
    }

    @Test
    fun `given an open that ignores cancel, when it opens late, then the connection closes unused`() = runTest {
        // Arrange
        val harness = Harness(scope = this, opening = Opening.IgnoringCancellation)
        val running = harness.start()
        runCurrent()

        // Act
        running.cancel()
        runCurrent()
        harness.finishOpening()

        // Assert
        assertTrue(actual = harness.connection.closed)
        assertTrue(actual = harness.connection.sent.isEmpty())
        assertTrue(actual = running.isCompleted)
    }

    @Test
    fun `given a device change while connecting, when accepted, then the newest values are sent`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.changeDevice(device = ROTATED)

        // Act
        harness.accept()

        // Assert
        assertEquals(
            expected = listOf(
                ConsumerMessage.OpenScreen(screen = SCREEN),
                ConsumerMessage.DeviceChanged(device = ROTATED),
            ),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }

    @Test
    fun `given an accepted app, when the connection cancels a device change send, then it is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        val running = harness.start()
        runCurrent()
        harness.accept()
        harness.connection.sendFailure = CancellationException(FOREIGN_CANCELLATION)

        // Act
        harness.changeDevice(device = ROTATED)

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
        assertTrue(actual = harness.connection.closed)
        assertTrue(actual = running.isActive)
    }

    @Test
    fun `given an open screen, when the connection cancels a retry send, then it is unavailable`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        val running = harness.start()
        runCurrent()
        harness.accept()
        harness.deliverScreen()
        harness.connection.sendFailure = CancellationException(FOREIGN_CANCELLATION)

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        assertIs<ScreenProblem.Unavailable>(value = harness.problem())
        assertTrue(actual = harness.connection.closed)
        assertTrue(actual = running.isActive)
    }

    @Test
    fun `given a send the connection cancelled, when the user retries, then the app is introduced again`() = runTest {
        // Arrange
        val second = FakeConnection()
        val harness = Harness(scope = this, connections = mutableListOf(FakeConnection(), second))
        harness.start()
        runCurrent()
        harness.accept()
        harness.connection.sendFailure = CancellationException(FOREIGN_CANCELLATION)
        harness.changeDevice(device = ROTATED)

        // Act
        harness.session.retry()
        runCurrent()

        // Assert
        assertEquals(expected = ROTATED, actual = second.introduction().device)
    }

    @Test
    fun `given an accepted app, when the session is cancelled, then it ends without a problem and closes`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        val running = harness.start()
        runCurrent()
        harness.accept()

        // Act
        running.cancel()
        runCurrent()

        // Assert
        assertTrue(actual = running.isCompleted)
        assertTrue(actual = harness.connection.closed)
        assertEquals(expected = SessionState.Connecting, actual = harness.session.state.value)
    }

    @Test
    fun `given a close that suspends, when the user retries during it, then the app is introduced again`() = runTest {
        // Arrange
        val closing = CompletableDeferred<Unit>()
        val second = FakeConnection()
        val harness = Harness(scope = this, connections = mutableListOf(FakeConnection(closing = closing), second))
        harness.start()
        runCurrent()
        harness.connection.deliver(message = REFUSED)
        runCurrent()

        // Act
        harness.session.retry()
        closing.complete(value = Unit)
        runCurrent()

        // Assert
        assertEquals(expected = 2, actual = harness.opened)
        assertEquals(expected = DEVICE, actual = second.introduction().device)
    }

    @Test
    fun `given a retry while connecting, when the app is accepted, then the screen is opened once`() = runTest {
        // Arrange
        val harness = Harness(scope = this)
        harness.start()
        runCurrent()
        harness.session.retry()

        // Act
        harness.accept()

        // Assert
        assertEquals(
            expected = listOf(ConsumerMessage.OpenScreen(screen = SCREEN)),
            actual = harness.connection.messagesAfterIntroduction(),
        )
    }
}
