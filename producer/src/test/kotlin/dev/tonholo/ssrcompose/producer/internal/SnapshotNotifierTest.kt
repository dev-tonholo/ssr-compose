package dev.tonholo.ssrcompose.producer.internal

import dev.tonholo.ssrcompose.producer.testing.FakeWriteObserverRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotNotifierTest {
    @Test
    fun `given several screens waiting, when they all join, then one write observer is registered`() = runTest {
        // Arrange
        val registry = FakeWriteObserverRegistry()
        val notifier = notifierWith(registry)

        // Act
        val screens = List(SCREEN_COUNT) { waitingScreen(notifier) }

        // Assert
        assertEquals(expected = ONE_OBSERVER, actual = registry.registered)
        screens.forEach { it.cancelAndJoin() }
    }

    @Test
    fun `given several screens waiting, when they leave one by one, then the observer goes with the last`() =
        runTest {
            // Arrange
            val registry = FakeWriteObserverRegistry()
            val notifier = notifierWith(registry)
            val screens = List(SCREEN_COUNT) { waitingScreen(notifier) }

            // Act
            val disposedWhileOneRemains = screens.dropLast(n = 1).map { screen ->
                screen.cancelAndJoin()
                registry.disposed
            }
            screens.last().cancelAndJoin()

            // Assert
            assertEquals(expected = List(SCREEN_COUNT - 1) { NO_OBSERVER }, actual = disposedWhileOneRemains)
            assertEquals(expected = ONE_OBSERVER, actual = registry.disposed)
        }

    @Test
    fun `given every screen has left, when a screen joins again, then a write observer is registered again`() =
        runTest {
            // Arrange
            val registry = FakeWriteObserverRegistry()
            val notifier = notifierWith(registry)
            waitingScreen(notifier).cancelAndJoin()

            // Act
            val returning = waitingScreen(notifier)

            // Assert
            assertEquals(expected = TWO_OBSERVERS, actual = registry.registered)
            assertEquals(expected = ONE_OBSERVER, actual = registry.registered - registry.disposed)
            returning.cancelAndJoin()
        }

    private fun TestScope.notifierWith(registry: FakeWriteObserverRegistry) =
        SnapshotNotifier(
            dispatcher = StandardTestDispatcher(scheduler = testScheduler),
            registerWriteObserver = registry.register,
        )

    private fun TestScope.waitingScreen(notifier: SnapshotNotifier): Job =
        launch { notifier.applyWritesWhileWaiting() }.also { runCurrent() }

    private companion object {
        const val SCREEN_COUNT = 3
        const val NO_OBSERVER = 0
        const val ONE_OBSERVER = 1
        const val TWO_OBSERVERS = 2
    }
}
