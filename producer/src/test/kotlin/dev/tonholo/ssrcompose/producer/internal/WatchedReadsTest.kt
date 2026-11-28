package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import dev.tonholo.ssrcompose.producer.testing.afterCollectingGarbage
import java.lang.ref.WeakReference
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WatchedReadsTest {
    private val haveChangedAtEachTell = mutableListOf<Boolean>()
    private val reads: WatchedReads = WatchedReads(onChanged = ::noteTell)

    @AfterTest
    fun closeReads() {
        reads.close()
    }

    @Test
    fun `given a state the last read read, when it changes, then it tells with the change already noted`() {
        // Arrange
        val state = mutableIntStateOf(value = 0)
        reads.read { state.intValue }

        // Act
        Snapshot.withMutableSnapshot { state.intValue = 1 }

        // Assert
        assertEquals(expected = listOf(true), actual = haveChangedAtEachTell)
    }

    @Test
    fun `given a state only the read before read, when it changes, then it does not tell`() {
        // Arrange
        val earlier = mutableIntStateOf(value = 0)
        val later = mutableIntStateOf(value = 0)
        reads.read { earlier.intValue }
        reads.read { later.intValue }

        // Act
        Snapshot.withMutableSnapshot { earlier.intValue = 1 }

        // Assert
        assertEquals(expected = emptyList(), actual = haveChangedAtEachTell)
    }

    @Test
    fun `given a change that was noted, when it reads again, then no change is noted`() {
        // Arrange
        val state = mutableIntStateOf(value = 0)
        reads.read { state.intValue }
        Snapshot.withMutableSnapshot { state.intValue = 1 }

        // Act
        reads.read { state.intValue }

        // Assert
        assertFalse(actual = reads.haveChanged)
    }

    @Test
    fun `given closed reads, when a state they read changes, then they do not tell`() {
        // Arrange
        val state = mutableIntStateOf(value = 0)
        reads.read { state.intValue }
        reads.close()

        // Act
        Snapshot.withMutableSnapshot { state.intValue = 1 }

        // Assert
        assertEquals(expected = emptyList(), actual = haveChangedAtEachTell)
    }

    @Test
    fun `given a state another thread changes during the read, when the read ends, then the change is noted`() {
        // Arrange
        val state = mutableIntStateOf(value = 0)

        // Act
        reads.read {
            state.intValue
            thread { Snapshot.withMutableSnapshot { state.intValue = 1 } }.join()
        }

        // Assert
        assertEquals(expected = true, actual = reads.haveChanged)
    }

    @Test
    fun `given closed reads that are dropped, when garbage is collected, then what their callback holds is let go`() {
        // Arrange
        val state = mutableIntStateOf(value = 0)

        // Act
        val heldByCallback = callbackTargetOfClosedReads { state.intValue }

        // Assert
        assertNull(actual = heldByCallback.afterCollectingGarbage())
    }

    private fun callbackTargetOfClosedReads(read: () -> Int): WeakReference<Any> {
        val target = StringBuilder()
        WatchedReads(onChanged = { target.append(CHANGED) }).use { closed -> closed.read(block = read) }
        return WeakReference(target)
    }

    private fun noteTell() {
        haveChangedAtEachTell.add(reads.haveChanged)
    }

    private companion object {
        const val CHANGED = "changed"
    }
}
