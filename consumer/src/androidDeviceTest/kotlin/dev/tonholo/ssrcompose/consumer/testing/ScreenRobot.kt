package dev.tonholo.ssrcompose.consumer.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import dev.tonholo.ssrcompose.consumer.ComponentBindings
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.SsrScreen
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.connection.Connection
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import java.util.concurrent.atomic.AtomicInteger

const val SCREEN = "checkout"
const val GREETING = "Greeting"
const val AMOUNT = "Amount"
const val OTHER_AMOUNT = "OtherAmount"
const val PAY_NOW = "Pay now"
const val MISMATCH_PREFIX = "mismatch:"
const val CENTS = "cents"

const val LATER_AMOUNT = "LaterAmount"
const val LAZY_AMOUNT = "LazyAmount"
const val SCROLLED_AMOUNT = "ScrolledAmount"
const val DIALOG_AMOUNT = "DialogAmount"
const val POPUP_AMOUNT = "PopupAmount"
const val READ_LATER_LABEL = "Reads its amount later"

private const val TITLE = "title"
private const val WAIT_MILLIS = 10_000L
private const val LAST_ROW = 59
private val LIST_HEIGHT = 200.dp
private val AMOUNT_BOX_SIZE = 10.dp

val CATALOG_VERSION = CatalogVersion(value = "3")
val APP = AppIdentity(version = "2.4.0", variant = "release")

fun greeting(title: String = PAY_NOW) =
    PlacedComponent(name = GREETING, values = mapOf(TITLE to DataValue.Text(value = title)))

fun contentOf(vararg components: PlacedComponent) = ScreenContent.Components(components = components.toList())

/** Shows `SsrScreen` with test bindings and counts how often an amount reaches the screen. */
class ScreenRobot(private val composeRule: ComposeContentTestRule) {
    private val placedAmounts = AtomicInteger(0)
    private val drawnAmounts = AtomicInteger(0)

    val placements: Int get() = placedAmounts.get()
    val draws: Int get() = drawnAmounts.get()

    val bindings = ComponentBindings {
        bind(GREETING) { placed -> BasicText(text = placed.text(name = TITLE)) }
        bind(AMOUNT) { placed -> AmountBox(cents = placed.whole(name = CENTS)) }
        bind(OTHER_AMOUNT) { placed -> AmountBox(cents = placed.whole(name = CENTS)) }
        bind(LATER_AMOUNT) { placed -> AmountReadLater(placed = placed) }
        bind(LAZY_AMOUNT) { placed ->
            LazyColumn(modifier = Modifier.height(height = LIST_HEIGHT)) {
                item { AmountBox(cents = placed.whole(name = CENTS)) }
            }
        }
        bind(SCROLLED_AMOUNT) { placed -> AmountAfterScrolling(placed = placed) }
        bind(DIALOG_AMOUNT) { placed ->
            Dialog(onDismissRequest = {}) { AmountBox(cents = placed.whole(name = CENTS)) }
        }
        bind(POPUP_AMOUNT) { placed ->
            Popup { AmountBox(cents = placed.whole(name = CENTS)) }
        }
    }

    @Composable
    private fun AmountBox(cents: Long) {
        BasicText(text = cents.toString())
        Box(
            modifier = Modifier
                .size(size = AMOUNT_BOX_SIZE)
                .onPlaced { placedAmounts.incrementAndGet() }
                .drawBehind { drawnAmounts.incrementAndGet() },
        )
    }

    @Composable
    private fun AmountReadLater(placed: PlacedComponentScope) {
        var reading by remember { mutableStateOf(value = false) }
        LaunchedEffect(Unit) { reading = true }
        BasicText(text = READ_LATER_LABEL)
        if (reading) AmountBox(cents = placed.whole(name = CENTS))
    }

    @Composable
    private fun AmountAfterScrolling(placed: PlacedComponentScope) {
        val listState = rememberLazyListState()
        LaunchedEffect(listState) { listState.scrollToItem(index = LAST_ROW) }
        LazyColumn(state = listState, modifier = Modifier.height(height = LIST_HEIGHT)) {
            items(count = LAST_ROW + 1) { row ->
                if (row == LAST_ROW) AmountBox(cents = placed.whole(name = CENTS)) else BasicText(text = "Row $row")
            }
        }
    }

    @Composable
    fun Screen(connection: suspend () -> Connection) {
        SsrScreen(
            screen = SCREEN,
            bindings = bindings,
            catalogVersion = CATALOG_VERSION,
            connection = connection,
            app = APP,
            unavailable = { problem, _ ->
                if (problem is ScreenProblem.Mismatch) BasicText(text = MISMATCH_PREFIX + problem.component)
            },
        )
    }

    fun waitForText(text: String, timeoutMillis: Long = WAIT_MILLIS) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodes(matcher = hasText(text = text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun waitForClosed(connection: FakeConnection) {
        composeRule.waitUntil(timeoutMillis = WAIT_MILLIS) { connection.closed }
    }
}
