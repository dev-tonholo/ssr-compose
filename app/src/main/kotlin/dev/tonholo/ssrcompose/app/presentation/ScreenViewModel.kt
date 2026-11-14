package dev.tonholo.ssrcompose.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenEvent
import dev.tonholo.ssrcompose.app.presentation.ui.state.ScreenUiState
import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val DEFAULT_ERROR_MESSAGE = "Could not load the screen"

class ScreenViewModel(
    private val service: ScreenService,
    private val screenName: String,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow<ScreenUiState>(ScreenUiState.Loading)
    val state: StateFlow<ScreenUiState> = mutableState.asStateFlow()

    private var outgoingEvents = Channel<ActionEvent>(Channel.BUFFERED)
    private var sessionJob: Job? = null

    init {
        startSession()
    }

    fun onEvent(event: ScreenEvent) {
        when (event) {
            ScreenEvent.Retry -> startSession()
            is ScreenEvent.Action -> outgoingEvents.trySend(ActionEvent(actionId = event.id))
        }
    }

    private fun startSession() {
        sessionJob?.cancel()
        outgoingEvents.close()
        outgoingEvents = Channel(Channel.BUFFERED)
        mutableState.value = ScreenUiState.Loading
        sessionJob = viewModelScope.launch(dispatcher) { followSession(events = outgoingEvents) }
    }

    private suspend fun followSession(events: Channel<ActionEvent>) {
        service.session(name = screenName, events = events.receiveAsFlow())
            .catch { failure ->
                if (failure is CancellationException) throw failure
                mutableState.value = ScreenUiState.Failed(message = failure.message ?: DEFAULT_ERROR_MESSAGE)
            }
            .collect { tree -> mutableState.value = ScreenUiState.Loaded(tree = tree) }
    }
}
