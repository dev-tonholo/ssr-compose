package dev.tonholo.ssrcompose.consumer.internal.session

import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.core.message.ScreenContent

/** What a [ConsumerSession] can show at a given moment. */
internal sealed interface SessionState {
    data object Connecting : SessionState

    /** A screen from the producer. [revision] grows with every `Screen` message of the session. */
    data class Showing(val revision: Long, val content: ScreenContent) : SessionState

    data class Problem(val problem: ScreenProblem) : SessionState
}
