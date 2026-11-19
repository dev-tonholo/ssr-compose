package dev.tonholo.ssrcompose.consumer.text

/**
 * Every text the library writes for the user. An app replaces a text through
 * [SsrComposeStringResourceManager].
 */
public enum class SsrComposeStringResource {
    /** The label of the retry button of every problem. */
    ScreenProblemRetry,

    /** Says that a screen cannot be shown because the app cannot draw part of it. */
    ScreenProblemMismatch,

    /** Says that the connection to the server ended. */
    ScreenProblemConnectionEnded,

    /** Says that the app could not read the answer from the server. */
    ScreenProblemAnswerUnreadable,
}
