package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.tonholo.ssrcompose.protocol.ComponentContract
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Composable
fun <P> Reference(contract: ComponentContract<P>, props: P) {
    val encodedProps = Json.encodeToJsonElement(contract.propsSerializer, props).jsonObject
    ComposeNode<MutableNode.Reference, TreeApplier>(
        factory = { MutableNode.Reference(componentId = contract.id, props = encodedProps) },
        update = {},
    )
}
