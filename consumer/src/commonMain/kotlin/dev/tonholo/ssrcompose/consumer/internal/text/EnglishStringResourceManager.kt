package dev.tonholo.ssrcompose.consumer.internal.text

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResourceManager

/** The library's own text, in English. It is the default of `LocalSsrComposeStringResourceManager`. */
internal object EnglishStringResourceManager : SsrComposeStringResourceManager {
    @Composable
    override fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String =
        when (resource) {
            SsrComposeStringResource.ScreenProblemRetry -> "Try again"
            SsrComposeStringResource.ScreenProblemMismatch -> "This screen cannot be shown right now."
            SsrComposeStringResource.ScreenProblemConnectionEnded ->
                "The connection to the server ended. Please try again."
            SsrComposeStringResource.ScreenProblemAnswerUnreadable ->
                "The app could not read the answer from the server."
        }
}
