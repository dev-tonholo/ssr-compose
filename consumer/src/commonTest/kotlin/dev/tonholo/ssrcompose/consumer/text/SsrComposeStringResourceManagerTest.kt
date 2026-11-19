package dev.tonholo.ssrcompose.consumer.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import kotlin.test.Test
import kotlin.test.assertEquals

private const val RETRY_TEXT_FROM_CONTRACT = "Try again"
private const val MISMATCH_TEXT_FROM_CONTRACT = "This screen cannot be shown right now."
private const val CONNECTION_ENDED_TEXT_FROM_CONTRACT = "The connection to the server ended. Please try again."
private const val ANSWER_UNREADABLE_TEXT_FROM_CONTRACT = "The app could not read the answer from the server."
private const val APP_RETRY_TEXT = "Tentar de novo"

private val englishTextFromContract = mapOf(
    SsrComposeStringResource.ScreenProblemRetry to RETRY_TEXT_FROM_CONTRACT,
    SsrComposeStringResource.ScreenProblemMismatch to MISMATCH_TEXT_FROM_CONTRACT,
    SsrComposeStringResource.ScreenProblemConnectionEnded to CONNECTION_ENDED_TEXT_FROM_CONTRACT,
    SsrComposeStringResource.ScreenProblemAnswerUnreadable to ANSWER_UNREADABLE_TEXT_FROM_CONTRACT,
)

private class AppStrings(private val fallback: SsrComposeStringResourceManager) : SsrComposeStringResourceManager {
    @Composable
    override fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String =
        when (resource) {
            SsrComposeStringResource.ScreenProblemRetry -> APP_RETRY_TEXT
            else -> fallback.stringResource(resource = resource, arguments = arguments)
        }
}

private fun readEveryKey(): Map<SsrComposeStringResource, String> {
    val read = mutableMapOf<SsrComposeStringResource, String>()
    composeOnce {
        val strings = LocalSsrComposeStringResourceManager.current
        SsrComposeStringResource.entries.forEach { key ->
            read[key] = strings.stringResource(resource = key)
        }
    }
    return read
}

class SsrComposeStringResourceManagerTest {
    @Test
    fun `given no manager provided, when every key is read, then each one is the English text of the contract`() {
        // Arrange
        val expected = englishTextFromContract

        // Act
        val actual = readEveryKey()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given an app manager that replaces one key, when every key is read, then the rest fall back to English`() {
        // Arrange
        val expected = englishTextFromContract + (SsrComposeStringResource.ScreenProblemRetry to APP_RETRY_TEXT)
        val actual = mutableMapOf<SsrComposeStringResource, String>()

        // Act
        composeOnce {
            val fallback = LocalSsrComposeStringResourceManager.current
            val appStrings = AppStrings(fallback = fallback)
            CompositionLocalProvider(LocalSsrComposeStringResourceManager provides appStrings) {
                SsrComposeStringResource.entries.forEach { key ->
                    actual[key] = LocalSsrComposeStringResourceManager.current.stringResource(resource = key)
                }
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
