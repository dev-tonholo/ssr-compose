package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.protocol.ComponentContract
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private typealias Content = @Composable () -> Unit

class ComponentRegistry {
    private val entries = mutableMapOf<String, (ReferenceNode) -> Content?>()

    fun <P> register(contract: ComponentContract<P>, content: @Composable (props: P) -> Unit) {
        entries[contract.id] = { node ->
            decodeProps(contract, node)?.let { props -> { content(props) } }
        }
    }

    @Composable
    fun Render(node: ReferenceNode) {
        val component = entries[node.componentId]?.invoke(node)
        if (component == null) {
            Text(text = "Unknown component: ${node.componentId}")
        } else {
            component()
        }
    }

    private fun <P> decodeProps(contract: ComponentContract<P>, node: ReferenceNode): P? =
        try {
            Json.decodeFromJsonElement(contract.propsSerializer, node.props)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}
