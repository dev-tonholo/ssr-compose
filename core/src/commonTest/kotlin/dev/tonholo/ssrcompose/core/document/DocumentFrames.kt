package dev.tonholo.ssrcompose.core.document

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Builds document frames as JSON, so a test can break one field of a valid node. */
internal object DocumentFrames {
    const val ROOT_ID = 1
    const val NODE_SIZE = 10
    private val CORNERS = listOf("topStart", "topEnd", "bottomEnd", "bottomStart")

    fun JsonObject.with(key: String, value: JsonElement): JsonObject = JsonObject(content = this + (key to value))

    fun JsonObject.with(key: String, value: Number): JsonObject = with(key = key, value = JsonPrimitive(value = value))

    fun JsonObject.with(key: String, value: String): JsonObject = with(key = key, value = JsonPrimitive(value = value))

    fun screen(vararg children: JsonObject): ByteArray =
        screenWithRoot(root = box(id = ROOT_ID, children = children.toList()))

    fun screenWithRoot(root: JsonObject): ByteArray = buildJsonObject {
        put(key = "type", value = "Screen")
        put(key = "screen", value = "home")
        put(key = "revision", value = 1)
        put(key = "pending", value = false)
        put(key = "content", element = buildJsonObject { put(key = "root", element = root) })
    }.toString().encodeToByteArray()

    fun box(id: Int, children: List<JsonObject> = emptyList()): JsonObject =
        node(type = "Box", id = id).with(key = "children", value = JsonArray(content = children))

    fun text(id: Int): JsonObject = node(type = "Text", id = id)
        .with(key = "text", value = "Hi")
        .with(key = "style", value = style())

    fun style(): JsonObject = buildJsonObject {
        put(key = "size", value = 14)
        put(key = "weight", value = 400)
        put(key = "color", value = "#FF000000")
    }

    fun scroll(id: Int, children: List<JsonObject> = emptyList()): JsonObject = node(type = "Scroll", id = id)
        .with(key = "orientation", value = "Vertical")
        .with(key = "contentWidth", value = NODE_SIZE)
        .with(key = "contentHeight", value = NODE_SIZE)
        .with(key = "children", value = JsonArray(content = children))

    fun switcher(id: Int, variants: List<List<JsonObject>>): JsonObject = node(type = "Switcher", id = id)
        .with(key = "selected", value = 0)
        .with(key = "largestWidth", value = NODE_SIZE)
        .with(key = "largestHeight", value = NODE_SIZE)
        .with(key = "variants", value = buildJsonArray { variants.forEach { add(element = JsonArray(content = it)) } })

    fun component(id: Int, slots: Map<String, JsonObject> = emptyMap()): JsonObject = node(type = "Component", id = id)
        .with(key = "maxWidth", value = NODE_SIZE)
        .with(key = "maxHeight", value = JsonPrimitive(value = null as Number?))
        .with(key = "name", value = "Card")
        .with(key = "slots", value = JsonObject(content = slots))

    fun slot(nodes: List<JsonObject>): JsonObject = buildJsonObject {
        put(key = "width", value = NODE_SIZE)
        put(key = "height", value = NODE_SIZE)
        put(key = "nodes", element = JsonArray(content = nodes))
    }

    fun localClick(switcher: Int, variant: Int): JsonObject = buildJsonObject {
        put(key = "type", value = "Local")
        put(key = "switcher", value = switcher)
        put(key = "variant", value = variant)
    }

    fun roundedDecoration(radius: Number, borderWidth: Number): JsonObject = buildJsonObject {
        put(
            key = "border",
            element = buildJsonObject {
                put(key = "width", value = borderWidth)
                put(key = "color", value = "#FF000000")
            },
        )
        put(
            key = "shape",
            element = buildJsonObject {
                put(key = "type", value = "Rounded")
                CORNERS.forEach { corner -> put(key = corner, value = radius) }
            },
        )
    }

    private fun node(type: String, id: Int): JsonObject = buildJsonObject {
        put(key = "type", value = type)
        put(key = "id", value = id)
        put(key = "x", value = 0)
        put(key = "y", value = 0)
        put(key = "width", value = NODE_SIZE)
        put(key = "height", value = NODE_SIZE)
    }
}
