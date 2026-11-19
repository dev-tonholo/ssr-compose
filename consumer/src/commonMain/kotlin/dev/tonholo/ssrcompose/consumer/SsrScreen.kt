package dev.tonholo.ssrcompose.consumer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import dev.tonholo.ssrcompose.consumer.internal.device.rememberDeviceValues
import dev.tonholo.ssrcompose.consumer.internal.screen.PlacedProblems
import dev.tonholo.ssrcompose.consumer.internal.screen.ProvisionalScreenContent
import dev.tonholo.ssrcompose.consumer.internal.session.ConsumerSession
import dev.tonholo.ssrcompose.consumer.internal.session.SessionState
import dev.tonholo.ssrcompose.consumer.problem.ScreenProblemMessage
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.connection.Connection
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.ScreenContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

/**
 * Connects to the producer, opens the server screen called [screen] and shows what the server renders for it.
 * [bindings] draws the app's own composables that the server placed.
 *
 * @param catalogVersion the catalog version this build of the app holds.
 * @param connection opens a new connection for this screen alone. It is called again when the app is introduced again
 * after a retry. Two screens must not share a connection, because the producer refuses a second introduction.
 * @param app who the app says it is.
 * @param unavailable shows a [ScreenProblem]. Its `retry` asks for the same screen again, or introduces the app again
 * after [ScreenProblem.Refused], [ScreenProblem.ConnectionEnded] or [ScreenProblem.AnswerUnreadable].
 * Without it, a [ScreenProblemMessage] with the library's texts and look is shown.
 */
@Composable
public fun SsrScreen(
    screen: String,
    bindings: ComponentBindings,
    catalogVersion: CatalogVersion,
    connection: suspend () -> Connection,
    app: AppIdentity,
    modifier: Modifier = Modifier,
    unavailable: @Composable (problem: ScreenProblem, retry: () -> Unit) -> Unit = { problem, retry ->
        ScreenProblemMessage(problem = problem, retry = retry, modifier = Modifier.fillMaxSize())
    },
) {
    val deviceValues = rememberDeviceValues()
    val latestConnection by rememberUpdatedState(newValue = connection)
    val session = remember(screen, catalogVersion, app) {
        ConsumerSession(
            screen = screen,
            catalogVersion = catalogVersion,
            app = app,
            deviceValues = deviceValues,
            openConnection = { latestConnection() },
            codec = MessageCodec.json(),
            decodingContext = Dispatchers.Default,
        )
    }
    LaunchedEffect(session) { withContext(context = Dispatchers.Main.immediate) { session.run() } }
    val state by session.state.collectAsState()

    Box(modifier = modifier) {
        when (val current = state) {
            SessionState.Connecting -> Unit
            is SessionState.Problem -> unavailable(current.problem, session::retry)
            is SessionState.Showing -> ShownScreen(
                shown = current,
                bindings = bindings,
                onMismatch = session::reportMismatch,
            )
        }
    }
}

@Composable
private fun ShownScreen(
    shown: SessionState.Showing,
    bindings: ComponentBindings,
    onMismatch: (revision: Long, component: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val problems = remember { PlacedProblems() }
    val latestOnMismatch by rememberUpdatedState(newValue = onMismatch)
    LaunchedEffect(problems, shown.revision) {
        snapshotFlow { problems.firstComponent(content = shown.content) }
            .filterNotNull()
            .collect { component -> latestOnMismatch(shown.revision, component) }
    }
    ProvisionalScreenContent(
        content = shown.content,
        bindings = bindings,
        problems = problems,
        modifier = modifier.placedOnlyWithoutProblems(problems = problems, content = shown.content),
    )
}

/** Keeps content with a reported problem off the screen in the frame that composed it. */
private fun Modifier.placedOnlyWithoutProblems(problems: PlacedProblems, content: ScreenContent): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints = constraints)
        layout(width = placeable.width, height = placeable.height) {
            if (problems.firstComponent(content = content) == null) placeable.place(x = 0, y = 0)
        }
    }
