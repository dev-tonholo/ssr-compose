package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.document.Click
import dev.tonholo.ssrcompose.core.document.Decoration
import dev.tonholo.ssrcompose.core.document.DocumentColor
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.NodeRole
import dev.tonholo.ssrcompose.core.document.ScreenDocument
import dev.tonholo.ssrcompose.core.document.SlotContent
import dev.tonholo.ssrcompose.core.document.TextStyleSpec
import dev.tonholo.ssrcompose.core.message.internal.DocumentScreen
import dev.tonholo.ssrcompose.core.message.internal.DocumentScreenCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DocumentScreenCodecTest {
    private fun decode(text: String): DocumentScreen = DocumentScreenCodec.decode(frame = text.encodeToByteArray())

    private fun screenHolding(node: DocumentNode) = DocumentScreen(
        screen = "home",
        revision = 4,
        pending = false,
        content = ScreenDocument(root = viewportBox(children = listOf(node))),
    )

    private fun viewportBox(children: List<DocumentNode>) = DocumentNode.Box(
        id = 1,
        x = 0f,
        y = 0f,
        width = 411.4f,
        height = 843.4f,
        children = children,
    )

    private fun screenFrameHolding(node: JsonElement): String = buildJsonObject {
        put(key = "type", value = "Screen")
        put(key = "screen", value = "home")
        put(key = "revision", value = 4)
        put(key = "pending", value = false)
        put(key = "content", element = buildJsonObject { put(key = "root", element = viewportBoxFrame(node = node)) })
    }.toString()

    private fun viewportBoxFrame(node: JsonElement): JsonObject = buildJsonObject {
        put(key = "type", value = "Box")
        put(key = "id", value = 1)
        put(key = "x", value = 0f)
        put(key = "y", value = 0f)
        put(key = "width", value = 411.4f)
        put(key = "height", value = 843.4f)
        put(key = "children", element = buildJsonArray { add(element = node) })
    }

    private fun style(size: Float, weight: Int, color: DocumentColor) =
        TextStyleSpec(size = size, weight = weight, color = color)

    @Test
    fun `given R20 example 1, when it is decoded, then it is the screen with no app component it describes`() {
        // Arrange
        val frame = EXAMPLE_1
        val expected = DocumentScreen(
            screen = "home",
            revision = 1,
            pending = false,
            content = ScreenDocument(root = viewportBox(children = listOf(exampleOneCard(), exampleOneText()))),
        )

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given R20 example 2, when it is decoded, then it is the screen with one measured app component`() {
        // Arrange
        val frame = EXAMPLE_2
        val greeting = DocumentNode.Component(
            id = 7,
            x = 16f,
            y = 16f,
            width = 379.4f,
            height = 120f,
            maxWidth = 379.4f,
            maxHeight = null,
            name = "Greeting",
            values = mapOf("title" to DataValue.Text(value = "Hi")),
        )
        val below = DocumentNode.Text(
            id = 8,
            x = 16f,
            y = 152f,
            width = 379.4f,
            height = 20f,
            text = "Below the app's card",
            style = style(size = 14f, weight = 400, color = DocumentColor(argb = 0xFF000000u)),
        )
        val expected = DocumentScreen(
            screen = "home",
            revision = 2,
            pending = false,
            content = ScreenDocument(root = viewportBox(children = listOf(greeting, below))),
        )

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given R20 example 3's Section node inside a screen, when it is decoded, then its slot holds server content`() {
        // Arrange
        val frame = screenFrameHolding(node = Json.parseToJsonElement(string = EXAMPLE_3_NODE))
        val expected = screenHolding(node = exampleThreeSection())

        // Act
        val actual = decode(text = frame)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given each R20 example screen, when it is encoded then decoded, then it is equal`() {
        // Arrange
        val screens = listOf(EXAMPLE_1, EXAMPLE_2).map { text -> decode(text = text) } +
            screenHolding(node = exampleThreeSection())

        // Act
        val actual = screens.map { screen ->
            DocumentScreenCodec.decode(frame = DocumentScreenCodec.encode(screen = screen))
        }

        // Assert
        assertEquals(expected = screens, actual = actual)
    }

    @Test
    fun `given colors whose first hex digit is 0, when encoded then decoded, then each is equal`() {
        // Arrange
        val screens = listOf(TRANSPARENT, LOW_ALPHA).map { argb ->
            val style = style(size = 14f, weight = 400, color = DocumentColor(argb = argb))
            screenHolding(node = exampleOneText().copy(style = style))
        }

        // Act
        val actual = screens.map { screen ->
            DocumentScreenCodec.decode(frame = DocumentScreenCodec.encode(screen = screen))
        }

        // Assert
        assertEquals(expected = screens, actual = actual)
    }

    private fun exampleOneCard() = DocumentNode.Box(
        id = 3,
        x = 16f,
        y = 16f,
        width = 379.4f,
        height = 96f,
        decoration = Decoration(
            background = DocumentColor(argb = 0xFF6200EEu),
            shape = Decoration.Shape.Rounded(topStart = 12f, topEnd = 12f, bottomEnd = 12f, bottomStart = 12f),
        ),
        click = Click.Remote(action = ActionId(value = "a1"), label = "Add one", role = NodeRole.Button),
        children = listOf(
            DocumentNode.Text(
                id = 4,
                x = 16f,
                y = 16f,
                width = 200f,
                height = 24f,
                text = "Tapped 2 times",
                style = style(size = 16f, weight = 500, color = DocumentColor(argb = 0xFFFFFFFFu)),
            ),
        ),
    )

    private fun exampleOneText() = DocumentNode.Text(
        id = 5,
        x = 16f,
        y = 128f,
        width = 379.4f,
        height = 40f,
        text = "Served by the producer",
        style = style(size = 14f, weight = 400, color = DocumentColor(argb = 0xDE000000u)),
        maxLines = 2,
        overflow = DocumentNode.Text.Overflow.Ellipsis,
    )

    private fun exampleThreeSection(): DocumentNode.Component {
        val serverText = DocumentNode.Text(
            id = 11,
            x = 0f,
            y = 0f,
            width = 347.4f,
            height = 20f,
            text = "Rendered by the server, inside the app's Section",
            style = style(size = 14f, weight = 400, color = DocumentColor(argb = 0xFF000000u)),
        )
        val payButton = DocumentNode.Component(
            id = 12,
            x = 0f,
            y = 24f,
            width = 120f,
            height = 40f,
            maxWidth = 347.4f,
            maxHeight = null,
            name = "PayButton",
            callbacks = mapOf("onClick" to ActionId(value = "a2")),
        )
        return DocumentNode.Component(
            id = 9,
            x = 16f,
            y = 16f,
            width = 379.4f,
            height = 160f,
            maxWidth = 379.4f,
            maxHeight = null,
            name = "Section",
            values = mapOf("title" to DataValue.Text(value = "Today")),
            slots = mapOf(
                "content" to SlotContent(width = 347.4f, height = 64f, nodes = listOf(serverText, payButton)),
            ),
        )
    }

    private companion object {
        const val TRANSPARENT = 0x00000000u
        const val LOW_ALPHA = 0x0A123456u

        // language=json
        const val EXAMPLE_1 = """{"type":"Screen","screen":"home","revision":1,"pending":false,"content":{"root":{"type":"Box","id":1,"x":0.0,"y":0.0,"width":411.4,"height":843.4,"children":[
  {"type":"Box","id":3,"x":16.0,"y":16.0,"width":379.4,"height":96.0,"decoration":{"background":"#FF6200EE","shape":{"type":"Rounded","topStart":12.0,"topEnd":12.0,"bottomEnd":12.0,"bottomStart":12.0}},"click":{"type":"Remote","action":"a1","label":"Add one","role":"Button"},"children":[
    {"type":"Text","id":4,"x":16.0,"y":16.0,"width":200.0,"height":24.0,"text":"Tapped 2 times","style":{"size":16.0,"weight":500,"color":"#FFFFFFFF"}}
  ]},
  {"type":"Text","id":5,"x":16.0,"y":128.0,"width":379.4,"height":40.0,"text":"Served by the producer","style":{"size":14.0,"weight":400,"color":"#DE000000"},"maxLines":2,"overflow":"Ellipsis"}
]}}}"""

        // language=json
        const val EXAMPLE_2 = """{"type":"Screen","screen":"home","revision":2,"pending":false,"content":{"root":{"type":"Box","id":1,"x":0.0,"y":0.0,"width":411.4,"height":843.4,"children":[
  {"type":"Component","id":7,"x":16.0,"y":16.0,"width":379.4,"height":120.0,"maxWidth":379.4,"maxHeight":null,"name":"Greeting","values":{"title":{"type":"Text","value":"Hi"}}},
  {"type":"Text","id":8,"x":16.0,"y":152.0,"width":379.4,"height":20.0,"text":"Below the app's card","style":{"size":14.0,"weight":400,"color":"#FF000000"}}
]}}}"""

        // language=json
        const val EXAMPLE_3_NODE = """{"type":"Component","id":9,"x":16.0,"y":16.0,"width":379.4,"height":160.0,"maxWidth":379.4,"maxHeight":null,"name":"Section","values":{"title":{"type":"Text","value":"Today"}},"slots":{"content":{"width":347.4,"height":64.0,"nodes":[
  {"type":"Text","id":11,"x":0.0,"y":0.0,"width":347.4,"height":20.0,"text":"Rendered by the server, inside the app's Section","style":{"size":14.0,"weight":400,"color":"#FF000000"}},
  {"type":"Component","id":12,"x":0.0,"y":24.0,"width":120.0,"height":40.0,"maxWidth":347.4,"maxHeight":null,"name":"PayButton","callbacks":{"onClick":"a2"}}
]}}}"""
    }
}
