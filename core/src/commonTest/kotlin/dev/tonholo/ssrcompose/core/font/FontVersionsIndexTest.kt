package dev.tonholo.ssrcompose.core.font

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** The index of the font versions the server build put in the producer's resources. */
class FontVersionsIndexTest {
    private fun versions(vararg labels: String): List<FontBundleVersion> = labels.map { FontBundleVersion(value = it) }

    private fun indexOf(versions: JsonElement): String =
        buildJsonObject { put(key = "versions", element = versions) }.toString()

    private fun labels(vararg labels: String): JsonElement =
        buildJsonArray { labels.forEach { add(element = JsonPrimitive(value = it)) } }

    private fun failureOf(text: String) = runCatching { FontVersionsIndex.read(bytes = text.encodeToByteArray()) }
        .exceptionOrNull()?.let { failure -> failure::class }

    @Test
    fun `given versions in any order, when the index is written, then the bytes are the sorted golden form`() {
        // Arrange
        val unsorted = versions("2", "10", "1")

        // Act
        val actual = FontVersionsIndex.write(versions = unsorted).decodeToString()

        // Assert
        assertEquals(expected = GOLDEN, actual = actual)
    }

    @Test
    fun `given a written index, when it is read, then it gives the same versions`() {
        // Arrange
        val bytes = FontVersionsIndex.write(versions = versions("1", "2"))

        // Act
        val actual = FontVersionsIndex.read(bytes = bytes)

        // Assert
        assertEquals(expected = versions("1", "2"), actual = actual)
    }

    @Test
    fun `given a version listed twice, when the index is written, then it is written once and can be read`() {
        // Arrange
        val repeated = versions("2", "1", "2")

        // Act
        val actual = FontVersionsIndex.read(bytes = FontVersionsIndex.write(versions = repeated))

        // Assert
        assertEquals(expected = versions("1", "2"), actual = actual)
    }

    @Test
    fun `given an index that repeats a version or names a bad label, when read, then it is refused`() {
        // Arrange
        val texts = listOf(
            indexOf(versions = labels("1", "1")),
            indexOf(versions = labels("../1")),
            indexOf(versions = JsonPrimitive(value = "1")),
            "not json",
        )

        // Act
        val actual = texts.map { text -> failureOf(text = text) }

        // Assert
        assertEquals(expected = texts.map { FontBundleException::class }, actual = actual)
    }

    @Test
    fun `given an index over its size cap, when read, then it is refused before parsing`() {
        // Arrange
        val bytes = FontVersionsIndex.write(versions = versions("1"))

        // Act
        val actual = runCatching { FontVersionsIndex.read(bytes = bytes, sizeCap = bytes.size - 1) }
            .exceptionOrNull()?.let { failure -> failure::class }

        // Assert
        assertEquals(expected = FontBundleException::class, actual = actual)
    }

    private companion object {
        // language=json
        const val GOLDEN = """{
  "versions": [
    "1",
    "10",
    "2"
  ]
}
"""
    }
}
