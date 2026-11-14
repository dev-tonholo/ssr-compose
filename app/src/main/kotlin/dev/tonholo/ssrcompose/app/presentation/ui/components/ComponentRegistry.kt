package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ComponentContract
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private typealias Content = @Composable (onAction: (ActionId) -> Unit) -> Unit

class ComponentRegistry {
    private val entries = mutableMapOf<String, (ReferenceNode) -> Content?>()

    fun <P> register(
        contract: ComponentContract<P>,
        content: @Composable (props: P, onAction: (ActionId) -> Unit) -> Unit,
    ) {
        entries[contract.id] = { node ->
            decodeProps(contract, node)?.let { props -> { onAction -> content(props, onAction) } }
        }
    }

    @Composable
    fun Render(node: ReferenceNode, onAction: (ActionId) -> Unit) {
        val component = entries[node.componentId]?.invoke(node)
        if (component == null) {
            Text(text = "Unknown component: ${node.componentId}")
        } else {
            component(onAction)
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
