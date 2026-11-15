package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class CounterCardProps(val title: String, val count: Int, val onAddOne: ActionId)

object CounterCard : ComponentContract<CounterCardProps> {
    override val id: String = "counter-card"
    override val propsSerializer: KSerializer<CounterCardProps> = CounterCardProps.serializer()
}
