package dev.tonholo.ssrcompose.app.presentation.ui.state

sealed interface ScreenEvent {
    data object Retry : ScreenEvent
}
