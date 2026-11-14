package dev.tonholo.ssrcompose.server.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.server.tree.Reference
import java.time.Clock
import java.time.LocalTime

const val HOME_CARD_MESSAGE = "This card is the app's own composable. The server only picked it."

@Composable
fun HomeScreen(clock: Clock) {
    val hour = remember { LocalTime.now(clock).hour }
    Reference(
        contract = GreetingCard,
        props = GreetingCardProps(title = greetingFor(hour = hour), message = HOME_CARD_MESSAGE),
    )
    ServerBanner()
}
