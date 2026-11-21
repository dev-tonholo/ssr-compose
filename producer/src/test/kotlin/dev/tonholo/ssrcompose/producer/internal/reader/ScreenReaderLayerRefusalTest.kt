package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.core.document.Decoration
import dev.tonholo.ssrcompose.core.document.DocumentColor
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.producer.testing.documentOf
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.withoutIds
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SIZE_DP = 10
private const val CORNER_DP = 2
private const val SHADOW_DP = 4
private const val SCALE = 2f
private const val ROTATION_DEGREES = 10f
private const val TRANSLATION_PX = 4f
private const val HALF_ALPHA = 0.5f
private val RED = DocumentColor(argb = 0xFFFF0000u)

class ScreenReaderLayerRefusalTest {
    @Test
    fun `given a scaled box, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).scale(SCALE).background(Color.Red)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a rotated box, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).rotate(ROTATION_DEGREES).background(Color.Red)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box moved by a layer, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp).graphicsLayer(translationX = TRANSLATION_PX).background(Color.Red))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a half transparent box, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).alpha(HALF_ALPHA).background(Color.Red)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box clipped to rounded corners, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp).clip(RoundedCornerShape(CORNER_DP.dp)).background(Color.Red))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with a shadow, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).shadow(SHADOW_DP.dp).background(Color.Red)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with a layer set in a block, when read, then it is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).graphicsLayer {}.background(Color.Red)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a scaled layout around a box, when read, then the box is not sendable for its graphics layer`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.scale(SCALE)) { Box(Modifier.size(SIZE_DP.dp).background(Color.Red)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout with a layer set in a block around a box, when read, then the box is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.graphicsLayer { scaleX = SCALE }) { Box(Modifier.size(SIZE_DP.dp).background(Color.Red)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout with a shadow around a box, when read, then the box is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.shadow(SHADOW_DP.dp)) { Box(Modifier.size(SIZE_DP.dp).background(Color.Red)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout that places a box with a scaling layer, when read, then the box is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(width = placeable.width, height = placeable.height) {
                        placeable.placeWithLayer(x = 0, y = 0) { scaleX = SCALE }
                    }
                },
            ) {
                Box(Modifier.size(SIZE_DP.dp).background(Color.Red))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout that places a box with a rotating layer, when read, then the box is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(width = placeable.width, height = placeable.height) {
                        placeable.placeWithLayer(x = 0, y = 0) { rotationZ = ROTATION_DEGREES }
                    }
                },
            ) {
                Box(Modifier.size(SIZE_DP.dp).background(Color.Red))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a layout clipped to its bounds around a box, when read, then it is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Box(Modifier.clipToBounds()) { Box(Modifier.size(SIZE_DP.dp).background(Color.Red)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a scroll container clipped to its bounds, when read, then it is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.GraphicsLayer)

        // Act
        val actual = readSceneOf {
            Column(Modifier.size(SIZE_DP.dp).clipToBounds().verticalScroll(rememberScrollState())) {
                Box(Modifier.size(SIZE_DP.dp).background(Color.Red))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with a layer that changes nothing, when read, then it is sent at its place`() {
        // Arrange
        val expected = listOf<DocumentNode>(
            DocumentNode.Box(
                id = 0,
                x = 0f,
                y = 0f,
                width = SIZE_DP.toFloat(),
                height = SIZE_DP.toFloat(),
                decoration = Decoration(background = RED, shape = Decoration.Shape.Rectangle),
            ),
        )

        // Act
        val actual = documentOf {
            Box(Modifier.size(SIZE_DP.dp).graphicsLayer().background(Color.Red))
        }.root.children.map { it.withoutIds() }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
