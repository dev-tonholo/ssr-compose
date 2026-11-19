package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.text.LocalSsrComposeStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource

/** The words a problem message shows: what happened, and the label of its retry button. */
internal data class ProblemTexts(val message: String, val retryLabel: String)

/**
 * Chooses the [ProblemTexts] of [problem] from `LocalSsrComposeStringResourceManager`.
 * The reason of a producer is returned as given.
 */
@Composable
internal fun problemTexts(problem: ScreenProblem): ProblemTexts {
    val strings = LocalSsrComposeStringResourceManager.current
    val message = when (problem) {
        is ScreenProblem.Refused -> problem.reason
        is ScreenProblem.Unavailable -> problem.reason
        ScreenProblem.ConnectionEnded ->
            strings.stringResource(resource = SsrComposeStringResource.ScreenProblemConnectionEnded)
        ScreenProblem.AnswerUnreadable ->
            strings.stringResource(resource = SsrComposeStringResource.ScreenProblemAnswerUnreadable)
        is ScreenProblem.Mismatch ->
            strings.stringResource(resource = SsrComposeStringResource.ScreenProblemMismatch)
    }
    return ProblemTexts(
        message = message,
        retryLabel = strings.stringResource(resource = SsrComposeStringResource.ScreenProblemRetry),
    )
}
