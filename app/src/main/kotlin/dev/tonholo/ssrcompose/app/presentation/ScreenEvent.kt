package dev.tonholo.ssrcompose.app.presentation

sealed interface ScreenEvent {
    data object Retry : ScreenEvent
}
