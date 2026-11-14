package dev.tonholo.ssrcompose.app.presentation

import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenEvent
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenUiState
import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ColumnNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

class ScreenViewModelTest {
    private val tree = ColumnNode(children = emptyList())

    @Test
    fun `given a service that returns a tree, when the view model starts, then state goes from Loading to Loaded`() =
        runTest {
            // Arrange
            val service = FakeScreenService(result = Result.success(tree))

            // Act
            val viewModel = ScreenViewModel(service, "home", StandardTestDispatcher(testScheduler))

            // Assert
            viewModel.state.test {
                assertEquals(expected = ScreenUiState.Loading, actual = awaitItem())
                assertEquals(expected = ScreenUiState.Loaded(tree), actual = awaitItem())
            }
        }

    @Test
    fun `given a service that throws, when the view model starts, then state is Failed with the message`() =
        runTest {
            // Arrange
            val service = FakeScreenService(result = Result.failure(IllegalStateException("boom")))

            // Act
            val viewModel = ScreenViewModel(service, "home", StandardTestDispatcher(testScheduler))

            // Assert
            viewModel.state.test {
                assertEquals(expected = ScreenUiState.Loading, actual = awaitItem())
                assertEquals(expected = ScreenUiState.Failed("boom"), actual = awaitItem())
            }
        }

    @Test
    fun `given a failed state, when Retry arrives and the service works, then state is Loaded`() =
        runTest {
            // Arrange
            val service = FakeScreenService(result = Result.failure(IllegalStateException("boom")))
            val viewModel = ScreenViewModel(service, "home", StandardTestDispatcher(testScheduler))
            viewModel.state.test {
                awaitItem()
                awaitItem()

                // Act
                service.result = Result.success(tree)
                viewModel.onEvent(ScreenEvent.Retry)

                // Assert
                assertEquals(expected = ScreenUiState.Loading, actual = awaitItem())
                assertEquals(expected = ScreenUiState.Loaded(tree), actual = awaitItem())
            }
        }
}
