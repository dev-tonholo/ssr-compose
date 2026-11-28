package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.testing.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadedFontResolverTest {
    @Test
    fun `given four faces in an order, when their resolver is created, then it has loaded each one, in that order`() {
        // Arrange
        val order = listOf(RobotoFace.Italic, RobotoFace.Regular, RobotoFace.Bold, RobotoFace.Medium)
        val loaded = mutableListOf<RobotoFace>()
        val faces = order.map { face -> faceRecordingItsLoad(face = face, loaded = loaded) }

        // Act
        withSceneLock { loadedFontResolver(faces = faces) }

        // Assert
        assertEquals(expected = order, actual = loaded)
    }

    @Test
    fun `given two families' faces at one weight and style, when their resolver is created, then it loads both`() {
        // Arrange
        val loaded = mutableListOf<RobotoFace>()
        val faces = listOf(RobotoFace.Regular, RobotoFace.Bold).map { file ->
            faceRecordingItsLoad(face = file, loaded = loaded, weight = FontWeight.Normal)
        }

        // Act
        withSceneLock { loadedFontResolver(faces = faces) }

        // Assert
        assertEquals(expected = listOf(RobotoFace.Regular, RobotoFace.Bold), actual = loaded)
    }

    /** [face] as a font that adds itself to [loaded] when Compose asks for its bytes, which it does to load it. */
    private fun faceRecordingItsLoad(
        face: RobotoFace,
        loaded: MutableList<RobotoFace>,
        weight: FontWeight = face.weight,
    ): Font {
        val data = face.readBytes()
        return Font(
            identity = "recorded:${sha256Hex(data = data)}",
            getData = { data.also { loaded += face } },
            weight = weight,
            style = face.style,
        )
    }
}
