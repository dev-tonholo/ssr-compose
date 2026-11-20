package dev.tonholo.ssrcompose.consumer.testing

import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.Theme
import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test

private val UNREADABLE_FRAME = byteArrayOf(1, 2, 3)
private val OTHER_CONTRACT_VERSION = ContractVersion(value = 99)
private val DEVICE = DeviceContext(
    density = 2.5f,
    screenWidthPx = 1080,
    screenHeightPx = 2400,
    theme = Theme.Light,
    language = "en-US",
    fontScale = 1f,
    layoutDirection = LayoutDirection.Ltr,
)

private val codec = MessageCodec.json()

private fun introduceFrame(contractVersion: ContractVersion = codec.contractVersion): ByteArray {
    val introduction = Introduction(
        contractVersion = contractVersion,
        catalogVersion = CATALOG_VERSION,
        app = APP,
        device = DEVICE,
    )
    return codec.encodeFirstFrame(introduce = ConsumerMessage.Introduce(introduction = introduction))
}

class FakeConnectionTest {
    @Test
    fun given_unreadable_first_bytes__when_sent__then_they_are_rejected_and_a_valid_introduction_still_works() =
        runBlocking<Unit> {
            // Arrange
            val connection = FakeConnection(content = contentOf(greeting()))

            // Act
            assertFailsWith<MessageDecodingException> { connection.send(frame = UNREADABLE_FRAME) }
            connection.send(frame = introduceFrame())
            connection.send(frame = codec.encode(message = ConsumerMessage.OpenScreen(screen = SCREEN)))

            // Assert
            val answers = connection.incoming.take(count = 2).toList().map { codec.decodeProducerMessage(frame = it) }
            assertEquals(
                expected = listOf(
                    ProducerMessage.Answered(answer = Answer.Accepted(contractVersion = codec.contractVersion)),
                    ProducerMessage.Screen(screen = SCREEN, content = contentOf(greeting())),
                ),
                actual = answers,
            )
        }

    @Test
    fun given_an_introduction_of_another_contract_version__when_sent__then_it_is_rejected() = runBlocking<Unit> {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()))
        val frame = introduceFrame(contractVersion = OTHER_CONTRACT_VERSION)

        // Act
        val failure = runCatching { connection.send(frame = frame) }.exceptionOrNull()

        // Assert
        assertIs<MessageDecodingException>(value = failure)
    }
}
