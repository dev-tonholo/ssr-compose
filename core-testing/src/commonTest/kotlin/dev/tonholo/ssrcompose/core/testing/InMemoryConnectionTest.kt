package dev.tonholo.ssrcompose.core.testing

import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlinx.coroutines.test.runTest

class InMemoryConnectionTest {
    private val firstFrame = byteArrayOf(1, 2, 3)
    private val secondFrame = byteArrayOf(4)
    private val thirdFrame = byteArrayOf(5, 6)

    @Test
    fun `given a pair, when the first end sends frames, then the second end receives them in order`() = runTest {
        // Arrange
        val (consumerEnd, producerEnd) = InMemoryConnection.pair()

        // Act
        consumerEnd.send(frame = firstFrame)
        consumerEnd.send(frame = secondFrame)
        consumerEnd.send(frame = thirdFrame)

        // Assert
        producerEnd.incoming.test {
            assertContentEquals(expected = firstFrame, actual = awaitItem())
            assertContentEquals(expected = secondFrame, actual = awaitItem())
            assertContentEquals(expected = thirdFrame, actual = awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a pair, when the second end sends frames, then the first end receives them in order`() = runTest {
        // Arrange
        val (consumerEnd, producerEnd) = InMemoryConnection.pair()

        // Act
        producerEnd.send(frame = firstFrame)
        producerEnd.send(frame = secondFrame)

        // Assert
        consumerEnd.incoming.test {
            assertContentEquals(expected = firstFrame, actual = awaitItem())
            assertContentEquals(expected = secondFrame, actual = awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a pair, when one end is closed, then the other end's incoming flow completes`() = runTest {
        // Arrange
        val (consumerEnd, producerEnd) = InMemoryConnection.pair()

        // Act
        producerEnd.incoming.test {
            consumerEnd.close()

            // Assert
            awaitComplete()
        }
    }

    @Test
    fun `given a pair, when the second end is closed, then the first end's incoming flow completes`() = runTest {
        // Arrange
        val (consumerEnd, producerEnd) = InMemoryConnection.pair()

        // Act
        consumerEnd.incoming.test {
            producerEnd.close()

            // Assert
            awaitComplete()
        }
    }

    @Test
    fun `given frames sent before closing, when the end is closed, then the other end still receives them`() = runTest {
        // Arrange
        val (consumerEnd, producerEnd) = InMemoryConnection.pair()
        consumerEnd.send(frame = firstFrame)

        // Act
        consumerEnd.close()

        // Assert
        producerEnd.incoming.test {
            assertContentEquals(expected = firstFrame, actual = awaitItem())
            awaitComplete()
        }
    }
}
