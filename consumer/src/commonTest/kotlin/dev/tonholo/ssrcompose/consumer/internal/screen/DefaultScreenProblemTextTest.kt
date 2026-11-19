package dev.tonholo.ssrcompose.consumer.internal.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.consumer.text.LocalSsrComposeStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResourceManager
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import kotlin.test.Test
import kotlin.test.assertEquals

private const val MISMATCHED_COMPONENT = "Amount"
private const val PRODUCER_REASON = "Please update the app"
private const val RETRY_TEXT_FROM_MANAGER = "manager text for retry"
private const val MISMATCH_TEXT_FROM_MANAGER = "manager text for mismatch"
private const val CONNECTION_ENDED_TEXT_FROM_MANAGER = "manager text for connection ended"
private const val ANSWER_UNREADABLE_TEXT_FROM_MANAGER = "manager text for answer unreadable"

private val textFromManager = mapOf(
    SsrComposeStringResource.ScreenProblemRetry to RETRY_TEXT_FROM_MANAGER,
    SsrComposeStringResource.ScreenProblemMismatch to MISMATCH_TEXT_FROM_MANAGER,
    SsrComposeStringResource.ScreenProblemConnectionEnded to CONNECTION_ENDED_TEXT_FROM_MANAGER,
    SsrComposeStringResource.ScreenProblemAnswerUnreadable to ANSWER_UNREADABLE_TEXT_FROM_MANAGER,
)

private class RecordingStringResourceManager : SsrComposeStringResourceManager {
    val requested = mutableSetOf<SsrComposeStringResource>()

    @Composable
    override fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String {
        requested += resource
        return textFromManager.getValue(key = resource)
    }
}

private fun keysRead(problem: ScreenProblem): Set<SsrComposeStringResource> {
    val manager = RecordingStringResourceManager()
    composeOnce {
        CompositionLocalProvider(LocalSsrComposeStringResourceManager provides manager) {
            problemTexts(problem = problem)
        }
    }
    return manager.requested
}

private fun textsOf(problem: ScreenProblem): ProblemTexts {
    lateinit var texts: ProblemTexts
    composeOnce {
        CompositionLocalProvider(LocalSsrComposeStringResourceManager provides RecordingStringResourceManager()) {
            texts = problemTexts(problem = problem)
        }
    }
    return texts
}

class DefaultScreenProblemTextTest {
    @Test
    fun `given a library written problem, when texts are composed, then both come from the manager`() {
        // Arrange
        val problems = listOf(
            ScreenProblem.Mismatch(component = MISMATCHED_COMPONENT),
            ScreenProblem.ConnectionEnded,
            ScreenProblem.AnswerUnreadable,
        )
        val expected = listOf(
            ProblemTexts(message = MISMATCH_TEXT_FROM_MANAGER, retryLabel = RETRY_TEXT_FROM_MANAGER),
            ProblemTexts(message = CONNECTION_ENDED_TEXT_FROM_MANAGER, retryLabel = RETRY_TEXT_FROM_MANAGER),
            ProblemTexts(message = ANSWER_UNREADABLE_TEXT_FROM_MANAGER, retryLabel = RETRY_TEXT_FROM_MANAGER),
        )

        // Act
        val actual = problems.map { problem -> textsOf(problem = problem) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a producer reason, when the texts are composed, then the message is the reason as given`() {
        // Arrange
        val reasons = listOf(
            ScreenProblem.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = PRODUCER_REASON),
            ScreenProblem.Unavailable(reason = PRODUCER_REASON),
        )

        // Act
        val actual = reasons.map { reason -> textsOf(problem = reason).message }

        // Assert
        assertEquals(expected = listOf(PRODUCER_REASON, PRODUCER_REASON), actual = actual)
    }

    @Test
    fun `given a mismatch, when the message is composed, then it reads the mismatch key and the retry key`() {
        // Arrange
        val expected = setOf(
            SsrComposeStringResource.ScreenProblemMismatch,
            SsrComposeStringResource.ScreenProblemRetry,
        )

        // Act
        val actual = keysRead(problem = ScreenProblem.Mismatch(component = MISMATCHED_COMPONENT))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a connection that ended, when composed, then it reads the connection key and the retry key`() {
        // Arrange
        val expected = setOf(
            SsrComposeStringResource.ScreenProblemConnectionEnded,
            SsrComposeStringResource.ScreenProblemRetry,
        )

        // Act
        val actual = keysRead(problem = ScreenProblem.ConnectionEnded)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given an unreadable answer, when composed, then it reads the unreadable key and the retry key`() {
        // Arrange
        val expected = setOf(
            SsrComposeStringResource.ScreenProblemAnswerUnreadable,
            SsrComposeStringResource.ScreenProblemRetry,
        )

        // Act
        val actual = keysRead(problem = ScreenProblem.AnswerUnreadable)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a producer reason, when the message is composed, then only the retry key is read`() {
        // Arrange
        val expected = setOf(SsrComposeStringResource.ScreenProblemRetry)
        val reasons = listOf(
            ScreenProblem.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = PRODUCER_REASON),
            ScreenProblem.Unavailable(reason = PRODUCER_REASON),
        )

        // Act
        val actual = reasons.map { reason -> keysRead(problem = reason) }

        // Assert
        assertEquals(expected = listOf(expected, expected), actual = actual)
    }
}
