package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.testing.skiaClassesAllocatedBy
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

private const val SKIA_TYPEFACE = "Typeface"

/** Needs a JVM of its own: a Roboto text shown earlier would hide a face the producer did not load up front. */
class RobotoFacesLoadedFirstScreenTest {
    @Test
    fun `given only a screen with no text so far, when a screen shows each Roboto face, then it makes no typeface`() {
        // Arrange
        withRenderedScene(content = {}, read = {})

        // Act
        val madeByTheScreen = skiaClassesAllocatedBy { withRenderedScene(content = { EachRobotoFace() }, read = {}) }
        val madeByALoad = skiaClassesAllocatedBy {
            withSceneLock { loadedFontResolver(faces = bundledRobotoFaces()) }
        }

        // Assert
        assertContains(iterable = madeByALoad, element = SKIA_TYPEFACE)
        assertFalse(actual = SKIA_TYPEFACE in madeByTheScreen)
    }

    @Composable
    private fun EachRobotoFace() {
        Column {
            RobotoFace.entries.forEach { face ->
                BasicText(
                    text = face.name,
                    style = TextStyle(
                        fontFamily = bundledRobotoFamily(),
                        fontWeight = face.weight,
                        fontStyle = face.style,
                    ),
                )
            }
        }
    }
}
