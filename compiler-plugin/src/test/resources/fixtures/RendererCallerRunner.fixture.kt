@file:JvmName("RendererCallerRunner")

package fixtures.consumer.renderer

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import kotlin.coroutines.EmptyCoroutineContext

fun renderGreeting(title: String): String? {
    RecordedRender.greeting = null
    composeWithScope(name = "Greeting", parameter = "title", value = title) { scope ->
        GreetingRenderer(placed = scope)
    }
    return RecordedRender.greeting
}

fun renderBanner(headline: String): String? {
    RecordedRender.banner = null
    composeWithScope(name = "Banner", parameter = "headline", value = headline) { scope ->
        BannerRenderer(placed = scope)
    }
    return RecordedRender.banner
}

private fun composeWithScope(
    name: String,
    parameter: String,
    value: String,
    content: @Composable (PlacedComponentScope) -> Unit,
) {
    val placed = PlacedComponent(
        name = name,
        values = mapOf(parameter to DataValue.Text(value = value)),
    )
    val scope = PlacedComponentScope(placed = placed)
    val recomposer = Recomposer(effectCoroutineContext = EmptyCoroutineContext)
    val composition = Composition(applier = NoNodesApplier, parent = recomposer)
    try {
        composition.setContent { content(scope) }
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}

private object NoNodesApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit

    override fun insertBottomUp(index: Int, instance: Unit) = Unit

    override fun remove(index: Int, count: Int) = Unit

    override fun move(from: Int, to: Int, count: Int) = Unit

    override fun onClear() = Unit
}
