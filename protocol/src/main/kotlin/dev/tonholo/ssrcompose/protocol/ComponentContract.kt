package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface ComponentContract<P> {
    val id: String
    val propsSerializer: KSerializer<P>
}

@Serializable
data class GreetingCardProps(val title: String, val message: String)

object GreetingCard : ComponentContract<GreetingCardProps> {
    override val id: String = "greeting-card"
    override val propsSerializer: KSerializer<GreetingCardProps> = GreetingCardProps.serializer()
}
