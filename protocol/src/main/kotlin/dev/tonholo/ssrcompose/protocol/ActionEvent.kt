package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.Serializable

@Serializable
data class ActionEvent(val actionId: ActionId)
