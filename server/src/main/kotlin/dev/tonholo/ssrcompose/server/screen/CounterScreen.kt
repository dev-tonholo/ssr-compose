package dev.tonholo.ssrcompose.server.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.protocol.CounterCard
import dev.tonholo.ssrcompose.protocol.CounterCardProps
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.server.action.rememberAction
import dev.tonholo.ssrcompose.server.tree.Reference
import java.time.Clock
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

const val TIME_CARD_TITLE = "Server time"
const val COUNTER_CARD_TITLE = "Counter"
const val TIME_FORMAT = "HH:mm:ss"
private val clockTick = 1.seconds
private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

@Composable
fun CounterScreen(clock: Clock) {
    var count by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(LocalTime.now(clock)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(clockTick)
            now = LocalTime.now(clock)
        }
    }
    Reference(
        contract = GreetingCard,
        props = GreetingCardProps(title = TIME_CARD_TITLE, message = timeFormatter.format(now)),
    )
    Reference(
        contract = CounterCard,
        props = CounterCardProps(
            title = COUNTER_CARD_TITLE,
            count = count,
            onAddOne = rememberAction { count++ },
        ),
    )
    CounterBanner(count = count, onTap = rememberAction { count++ })
}
