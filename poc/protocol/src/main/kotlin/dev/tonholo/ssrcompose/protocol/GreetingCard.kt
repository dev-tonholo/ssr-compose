package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class GreetingCardProps(val title: String, val message: String)

object GreetingCard : ComponentContract<GreetingCardProps> {
    override val id: String = "greeting-card"
    override val propsSerializer: KSerializer<GreetingCardProps> = GreetingCardProps.serializer()
}
