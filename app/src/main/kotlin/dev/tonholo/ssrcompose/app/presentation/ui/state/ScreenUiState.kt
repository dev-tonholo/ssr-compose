package dev.tonholo.ssrcompose.app.presentation.ui.state

import dev.tonholo.ssrcompose.protocol.ScreenNode

sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Loaded(val tree: ScreenNode) : ScreenUiState
    data class Failed(val message: String) : ScreenUiState
}
