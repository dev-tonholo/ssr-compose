package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface ScreenNode

@Serializable
@SerialName("column")
data class ColumnNode(val children: List<ScreenNode>) : ScreenNode

@Serializable
@SerialName("reference")
data class ReferenceNode(val componentId: String, val props: JsonObject) : ScreenNode

@Serializable
@SerialName("fragment")
class FragmentNode(val document: ByteArray) : ScreenNode {
    override fun equals(other: Any?): Boolean =
        other is FragmentNode && document.contentEquals(other.document)

    override fun hashCode(): Int = document.contentHashCode()
}
