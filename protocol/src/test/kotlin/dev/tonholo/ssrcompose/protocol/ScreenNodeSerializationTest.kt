package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenNodeSerializationTest {

    private val json = Json

    @Test
    @Suppress("MaxLineLength")
    fun `given a tree with a column, a reference and a fragment, when it is encoded to JSON and decoded, then the result equals the original`() {
        // Arrange
        val original: ScreenNode = ColumnNode(
            children = listOf(
                ReferenceNode(
                    componentId = "greeting-card",
                    props = JsonObject(mapOf("title" to JsonPrimitive("Hi"))),
                ),
                FragmentNode(document = byteArrayOf(1, 2, 3)),
            ),
        )

        // Act
        val decoded = json.decodeFromString<ScreenNode>(json.encodeToString(original))

        // Assert
        assertEquals(expected = original, actual = decoded)
    }

    @Test
    fun `given two fragments with the same bytes, when compared, then they are equal`() {
        // Arrange
        val first = FragmentNode(document = byteArrayOf(4, 5, 6))
        val second = FragmentNode(document = byteArrayOf(4, 5, 6))

        // Act
        val areEqual = first == second

        // Assert
        assertEquals(expected = true, actual = areEqual)
        assertEquals(expected = first.hashCode(), actual = second.hashCode())
    }
}
