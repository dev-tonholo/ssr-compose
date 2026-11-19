package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.core.handshake.RefusalKind

/**
 * Why `SsrScreen` cannot show its screen. The app can show its own message for each case.
 */
public sealed interface ScreenProblem {
    /** The producer refused the app. [reason] is written for the user. */
    public data class Refused(val kind: RefusalKind, val reason: String) : ScreenProblem

    /** The screen or the connection is not available now. [reason] is written for the user. */
    public data class Unavailable(val reason: String) : ScreenProblem

    /** The connection to the producer failed or ended. */
    public data object ConnectionEnded : ScreenProblem

    /** The app could not read, or did not expect, what the producer sent. It should not happen. */
    public data object AnswerUnreadable : ScreenProblem

    /** The app received something it cannot draw for the placed [component]. It should not happen. */
    public data class Mismatch(val component: String) : ScreenProblem
}
