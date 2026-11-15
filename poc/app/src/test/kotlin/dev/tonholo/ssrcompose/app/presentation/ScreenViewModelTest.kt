package dev.tonholo.ssrcompose.app.presentation

import app.cash.turbine.test
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenEvent
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenUiState
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class ScreenViewModelTest {
    private val firstTree = ColumnNode(children = emptyList())
    private val secondTree = ColumnNode(children = listOf(FragmentNode(document = byteArrayOf(1))))

    private fun TestScope.startedViewModel(service: FakeScreenService): ScreenViewModel {
        val viewModel = ScreenViewModel(service, "home", StandardTestDispatcher(testScheduler))
        runCurrent()
        return viewModel
    }

    @Test
    fun `given a session that emits two trees, when collected, then state follows each tree`() =
        runTest {
            // Arrange
            val service = FakeScreenService()

            // Act
            val viewModel = startedViewModel(service)

            // Assert
            viewModel.state.test {
                assertEquals(expected = ScreenUiState.Loading, actual = awaitItem())
                service.emit(firstTree)
                assertEquals(expected = ScreenUiState.Loaded(firstTree), actual = awaitItem())
                service.emit(secondTree)
                assertEquals(expected = ScreenUiState.Loaded(secondTree), actual = awaitItem())
            }
        }

    @Test
    fun `given a loaded session, when an Action event arrives, then the service receives that action id`() =
        runTest {
            // Arrange
            val service = FakeScreenService()
            val viewModel = startedViewModel(service)
            service.emit(firstTree)
            advanceUntilIdle()

            // Act
            viewModel.onEvent(ScreenEvent.Action(id = ActionId("action-1")))
            advanceUntilIdle()

            // Assert
            assertEquals(expected = listOf(ActionId("action-1")), actual = service.receivedActions)
        }

    @Test
    fun `given a session that fails, when Retry arrives, then a new session starts and state is Loaded`() =
        runTest {
            // Arrange
            val service = FakeScreenService()
            val viewModel = startedViewModel(service)
            advanceUntilIdle()
            service.fail(IllegalStateException("boom"))
            advanceUntilIdle()
            assertEquals(expected = ScreenUiState.Failed("boom"), actual = viewModel.state.value)

            // Act
            viewModel.onEvent(ScreenEvent.Retry)
            runCurrent()
            service.emit(firstTree)
            advanceUntilIdle()

            // Assert
            assertEquals(expected = 2, actual = service.sessionCount)
            assertEquals(expected = ScreenUiState.Loaded(firstTree), actual = viewModel.state.value)
        }

    @Test
    fun `given a session that fails after trees arrived, when it fails, then state is Failed`() =
        runTest {
            // Arrange
            val service = FakeScreenService()
            val viewModel = startedViewModel(service)
            service.emit(firstTree)
            advanceUntilIdle()

            // Act
            service.fail(IllegalStateException("lost"))
            advanceUntilIdle()

            // Assert
            assertEquals(expected = ScreenUiState.Failed("lost"), actual = viewModel.state.value)
        }

    @Test
    fun `given a session that ends without failing, when it completes, then state is Failed with the closed message`() =
        runTest {
            // Arrange
            val service = FakeScreenService()
            val viewModel = startedViewModel(service)
            service.emit(firstTree)
            advanceUntilIdle()

            // Act
            service.complete()
            advanceUntilIdle()

            // Assert
            assertEquals(
                expected = ScreenUiState.Failed("The server closed the connection"),
                actual = viewModel.state.value,
            )
        }
}
