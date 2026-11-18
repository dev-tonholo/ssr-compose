@file:JvmName("BridgeCompositionRunner")

package app.caller

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import app.bridge.v1.Greeting as FirstGreeting
import app.bridge.v2.Greeting as SecondGreeting
import app.bridge.v4.Banner
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.internal.ScreenApplier
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

private const val MOST_FRAMES_TO_RECOMPOSE = 1_000L

private fun <T> composeOnce(content: @Composable () -> Unit, observe: (PlacedNode.Root) -> T): T {
    val root = PlacedNode.Root()
    val recomposer = Recomposer(EmptyCoroutineContext)
    val composition = Composition(ScreenApplier(root), recomposer)
    try {
        composition.setContent(content)
        return observe(root)
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}

private fun describedAfter(content: @Composable () -> Unit): String =
    composeOnce(content = content, observe = { root -> root.describe() })

private fun PlacedNode.describe(): String {
    val inner = children.joinToString(separator = ", ", prefix = "[", postfix = "]") { it.describe() }
    return when (this) {
        is PlacedNode.Root -> inner
        is PlacedNode.Component -> "$name@$catalogVersion$values$inner"
        is PlacedNode.Slot -> "slot $name$inner"
    }
}

fun everyParameter(): String = describedAfter {
    FirstGreeting(title = "Welcome", onSelect = {}, subtitle = "Details") {
        FirstGreeting(title = "Nested", onSelect = {})
    }
}

fun twoBridges(): String = describedAfter {
    FirstGreeting(title = "Welcome", onSelect = {})
    SecondGreeting(title = "Welcome", onSelect = {}, badge = "New")
}

fun optionalLeftOut(): String = describedAfter {
    FirstGreeting(title = "Welcome", onSelect = {})
}

fun defaultedTitleLeftOut(): String = describedAfter {
    Banner()
}

fun defaultedTitleGiven(): String = describedAfter {
    Banner(title = "Hello")
}

fun actionArgument(): String {
    var received = ""
    composeOnce(
        content = { FirstGreeting(title = "Welcome", onSelect = { selected -> received = selected }) },
        observe = { root ->
            val greeting = root.children.single() as PlacedNode.Component
            greeting.actions.getValue("onSelect").invoke("clicked")
        },
    )
    return received
}

fun recomposedNode(): String = runBlocking {
    val root = PlacedNode.Root()
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + clock)
    launch(clock) { recomposer.runRecomposeAndApplyChanges() }
    yield()
    val composition = Composition(ScreenApplier(root), recomposer)
    try {
        val title = mutableStateOf("Welcome")
        composition.setContent { FirstGreeting(title = title.value, onSelect = {}) }
        title.value = "Updated"
        Snapshot.sendApplyNotifications()
        var frame = 0L
        while (frame < MOST_FRAMES_TO_RECOMPOSE && !root.describe().contains("Updated")) {
            clock.sendFrame(frame++)
            yield()
        }
        root.describe()
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}
