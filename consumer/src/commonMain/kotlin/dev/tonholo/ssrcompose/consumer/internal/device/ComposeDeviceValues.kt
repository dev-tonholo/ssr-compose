package dev.tonholo.ssrcompose.consumer.internal.device

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.Theme

/**
 * The [DeviceValues] of the composition it is called in: density, window size, theme, language, font scale and
 * layout direction.
 * Its [DeviceValues.current] follows the composition as those values change.
 */
@Composable
internal fun rememberDeviceValues(): DeviceValues {
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val context = DeviceContext(
        density = density.density,
        screenWidthPx = windowSize.width,
        screenHeightPx = windowSize.height,
        theme = if (isSystemInDarkTheme()) Theme.Dark else Theme.Light,
        language = Locale.current.toLanguageTag(),
        fontScale = density.fontScale,
        fontScaleCurve = density.fontScaleCurve(),
        layoutDirection = LocalLayoutDirection.current.toDeviceLayoutDirection(),
    )
    val state = remember { mutableStateOf(context) }
    SideEffect { state.value = context }
    return remember { StateDeviceValues(state = state) }
}

private fun Density.fontScaleCurve(): FontScaleCurve = FontScaleCurve(
    points = FontScaleCurve.SAMPLED_SIZES.map { size -> FontScalePoint(sp = size, dp = size.sp.toDp().value) },
)

private fun ComposeLayoutDirection.toDeviceLayoutDirection(): LayoutDirection = when (this) {
    ComposeLayoutDirection.Ltr -> LayoutDirection.Ltr
    ComposeLayoutDirection.Rtl -> LayoutDirection.Rtl
}

private class StateDeviceValues(private val state: State<DeviceContext>) : DeviceValues {
    override val current: DeviceContext get() = state.value
}
