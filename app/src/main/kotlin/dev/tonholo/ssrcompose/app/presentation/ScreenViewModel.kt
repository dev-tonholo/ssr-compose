package dev.tonholo.ssrcompose.app.presentation

import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenEvent
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val DEFAULT_ERROR_MESSAGE = "Could not load the screen"

class ScreenViewModel(
    private val service: ScreenService,
    private val screenName: String,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val state: StateFlow<ScreenUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: ScreenEvent) {
        when (event) {
            ScreenEvent.Retry -> load()
        }
    }

    private fun load() {
        mutableState.value = ScreenUiState.Loading
        viewModelScope.launch(dispatcher) {
            mutableState.value = fetchScreen()
        }
    }

    private suspend fun fetchScreen(): ScreenUiState =
        runCatching { service.screen(name = screenName) }
            .onFailure { failure -> if (failure is CancellationException) throw failure }
            .fold(
                onSuccess = { tree -> ScreenUiState.Loaded(tree = tree) },
                onFailure = { failure -> ScreenUiState.Failed(message = failure.message ?: DEFAULT_ERROR_MESSAGE) },
            )
}
