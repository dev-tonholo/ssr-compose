package dev.tonholo.ssrcompose.consumer.internal.device

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.consumer.testing.composeAndRecompose
import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.Theme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val DENSITY = 2.5f
private const val FONT_SCALE = 1f
private const val LARGE_FONT_SCALE = 1.3f
private const val NON_LINEAR_OFFSET_DP = 4f
private const val WIDTH_PX = 1080
private const val HEIGHT_PX = 2400
private const val BRAZILIAN_PORTUGUESE = "pt-BR"
private const val CANADIAN_FRENCH = "fr-CA"
private const val NEW_DENSITY = 3f
private const val NEW_WIDTH_PX = 2400
private const val NEW_HEIGHT_PX = 1080

private class NonLinearDensity(override val density: Float) : Density {
    override val fontScale: Float = LARGE_FONT_SCALE

    override fun TextUnit.toDp(): Dp = (value + NON_LINEAR_OFFSET_DP).dp
}

private class FakeWindowInfo(override val containerSize: IntSize) : WindowInfo {
    override val isWindowFocused: Boolean = true
}

private fun deviceContext(
    nightMode: Int = Configuration.UI_MODE_NIGHT_NO,
    windowSize: IntSize = IntSize(width = WIDTH_PX, height = HEIGHT_PX),
    density: Density = Density(density = DENSITY, fontScale = FONT_SCALE),
    direction: ComposeLayoutDirection = ComposeLayoutDirection.Ltr,
): DeviceContext {
    var context: DeviceContext? = null
    val configuration = Configuration().apply { uiMode = nightMode }
    composeOnce {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalWindowInfo provides FakeWindowInfo(containerSize = windowSize),
            LocalConfiguration provides configuration,
            LocalLayoutDirection provides direction,
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
        val actual = deviceContext(windowSize = windowSize)

        // Assert
        assertEquals(expected = DENSITY, actual = actual.density)
        assertEquals(expected = WIDTH_PX, actual = actual.screenWidthPx)
        assertEquals(expected = HEIGHT_PX, actual = actual.screenHeightPx)
    }

    @Test
    fun `given a font scale and a right-to-left layout, when the values are read, then the context carries them`() {
        // Arrange
        val direction = ComposeLayoutDirection.Rtl
        val density = Density(density = DENSITY, fontScale = LARGE_FONT_SCALE)

        // Act
        val actual = deviceContext(density = density, direction = direction)

        // Assert
        assertEquals(expected = LARGE_FONT_SCALE, actual = actual.fontScale)
        assertEquals(expected = LayoutDirection.Rtl, actual = actual.layoutDirection)
    }

    @Test
    fun `given a density that scales text non-linearly, when the values are read, then the curve follows it`() {
        // Arrange
        val density = NonLinearDensity(density = DENSITY)
        val expected = EXPECTED_SAMPLED_SIZES.map { sp ->
            FontScalePoint(sp = sp, dp = sp + NON_LINEAR_OFFSET_DP)
        }

        // Act
        val actual = deviceContext(density = density)

        // Assert
        assertEquals(expected = expected, actual = actual.fontScaleCurve.points)
    }

    @Test
    fun `given a left-to-right layout, when the device values are read, then the context says left to right`() {
        // Arrange
        val direction = ComposeLayoutDirection.Ltr

        // Act
        val actual = deviceContext(direction = direction)

        // Assert
        assertEquals(expected = LayoutDirection.Ltr, actual = actual.layoutDirection)
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
                    LocalLayoutDirection provides ComposeLayoutDirection.Ltr,
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
                    LocalLayoutDirection provides ComposeLayoutDirection.Ltr,
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

    private companion object {
        val EXPECTED_SAMPLED_SIZES = listOf(8f, 10f, 12f, 14f, 18f, 20f, 24f, 30f, 100f)
    }
}
