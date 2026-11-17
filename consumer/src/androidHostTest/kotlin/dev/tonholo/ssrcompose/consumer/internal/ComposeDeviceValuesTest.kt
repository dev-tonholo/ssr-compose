package dev.tonholo.ssrcompose.consumer.internal

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import dev.tonholo.ssrcompose.consumer.testing.composeAndRecompose
import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Theme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val DENSITY = 2.5f
private const val WIDTH_PX = 1080
private const val HEIGHT_PX = 2400
private const val BRAZILIAN_PORTUGUESE = "pt-BR"
private const val CANADIAN_FRENCH = "fr-CA"
private const val NEW_DENSITY = 3f
private const val NEW_WIDTH_PX = 2400
private const val NEW_HEIGHT_PX = 1080

private class FakeWindowInfo(override val containerSize: IntSize) : WindowInfo {
    override val isWindowFocused: Boolean = true
}

private fun deviceContext(
    nightMode: Int = Configuration.UI_MODE_NIGHT_NO,
    windowSize: IntSize = IntSize(width = WIDTH_PX, height = HEIGHT_PX),
    density: Float = DENSITY,
): DeviceContext {
    var context: DeviceContext? = null
    val configuration = Configuration().apply { uiMode = nightMode }
    composeOnce {
        CompositionLocalProvider(
            LocalDensity provides Density(density = density),
            LocalWindowInfo provides FakeWindowInfo(containerSize = windowSize),
            LocalConfiguration provides configuration,
        ) {
            context = rememberDeviceValues().current
        }
    }
    return assertNotNull(context)
}

class ComposeDeviceValuesTest {
    @Test
    fun `given a density and a window size, when the device values are read, then the context carries them`() {
        // Arrange
        val windowSize = IntSize(width = WIDTH_PX, height = HEIGHT_PX)

        // Act
        val actual = deviceContext(windowSize = windowSize, density = DENSITY)

        // Assert
        assertEquals(expected = DENSITY, actual = actual.density)
        assertEquals(expected = WIDTH_PX, actual = actual.screenWidthPx)
        assertEquals(expected = HEIGHT_PX, actual = actual.screenHeightPx)
    }

    @Test
    fun `given the system night mode, when the device values are read, then the theme follows it`() {
        // Arrange
        val themesByNightMode = listOf(
            Configuration.UI_MODE_NIGHT_YES to Theme.Dark,
            Configuration.UI_MODE_NIGHT_NO to Theme.Light,
        )

        themesByNightMode.forEach { (nightMode, expected) ->
            // Act
            val actual = deviceContext(nightMode = nightMode)

            // Assert
            assertEquals(expected = expected, actual = actual.theme)
        }
    }

    @Test
    fun `given the device language, when the device values are read, then the language tag is that language`() {
        listOf(BRAZILIAN_PORTUGUESE, CANADIAN_FRENCH).forEach { tag ->
            // Arrange
            val previous = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag(tag))

            // Act
            val actual = try {
                deviceContext()
            } finally {
                Locale.setDefault(previous)
            }

            // Assert
            assertEquals(expected = tag, actual = actual.language)
        }
    }

    @Test
    fun `given a density and a window size that change, when the composition recomposes, then the values follow`() {
        // Arrange
        val density = mutableStateOf(DENSITY)
        val windowSize = mutableStateOf(IntSize(width = WIDTH_PX, height = HEIGHT_PX))
        val configuration = Configuration()
        var values: DeviceValues? = null

        composeAndRecompose(
            content = {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = density.value),
                    LocalWindowInfo provides FakeWindowInfo(containerSize = windowSize.value),
                    LocalConfiguration provides configuration,
                ) {
                    values = rememberDeviceValues()
                }
            },
        ) { recompose ->
            // Act
            density.value = NEW_DENSITY
            windowSize.value = IntSize(width = NEW_WIDTH_PX, height = NEW_HEIGHT_PX)
            recompose()

            // Assert
            val actual = assertNotNull(values).current
            assertEquals(expected = NEW_DENSITY, actual = actual.density)
            assertEquals(expected = NEW_WIDTH_PX, actual = actual.screenWidthPx)
            assertEquals(expected = NEW_HEIGHT_PX, actual = actual.screenHeightPx)
        }
    }

    @Test
    fun `given a system night mode that changes, when the composition recomposes, then the theme follows`() {
        // Arrange
        val configuration = mutableStateOf(Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO })
        val windowSize = IntSize(width = WIDTH_PX, height = HEIGHT_PX)
        var values: DeviceValues? = null

        composeAndRecompose(
            content = {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = DENSITY),
                    LocalWindowInfo provides FakeWindowInfo(containerSize = windowSize),
                    LocalConfiguration provides configuration.value,
                ) {
                    values = rememberDeviceValues()
                }
            },
        ) { recompose ->
            // Act
            configuration.value = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_YES }
            recompose()

            // Assert
            assertEquals(expected = Theme.Dark, actual = assertNotNull(values).current.theme)
        }
    }
}
