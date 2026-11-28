package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Popup
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ScreenSceneFontResolverTest {
    @Test
    fun `given two scenes, when each composes its screen, then both resolve fonts through one resolver`() {
        // Arrange
        var first: FontFamily.Resolver? = null
        var second: FontFamily.Resolver? = null

        // Act
        compose { first = LocalFontFamilyResolver.current }
        compose { second = LocalFontFamilyResolver.current }

        // Assert
        assertSame(expected = assertNotNull(actual = first), actual = second)
    }

    @Test
    fun `given a popup, when another scene composes its screen, then both resolve fonts through one resolver`() {
        // Arrange
        var inPopup: FontFamily.Resolver? = null
        var inScreen: FontFamily.Resolver? = null

        // Act
        compose { Popup { inPopup = LocalFontFamilyResolver.current } }
        compose { inScreen = LocalFontFamilyResolver.current }

        // Assert
        assertSame(expected = assertNotNull(actual = inPopup), actual = inScreen)
    }

    private fun compose(content: @Composable () -> Unit) {
        withRenderedScene(spec = SceneSpec(), content = content, read = {})
    }
}
