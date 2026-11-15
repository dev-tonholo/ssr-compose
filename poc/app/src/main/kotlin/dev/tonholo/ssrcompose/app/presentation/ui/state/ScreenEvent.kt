package dev.tonholo.ssrcompose.app.presentation.ui.state

import dev.tonholo.ssrcompose.protocol.ActionId

sealed interface ScreenEvent {
    data object Retry : ScreenEvent
    data class Action(val id: ActionId) : ScreenEvent
}
