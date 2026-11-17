package dev.tonholo.ssrcompose.consumer.internal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.intl.Locale
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Theme

/**
 * The [DeviceValues] of the composition it is called in: density, window size, theme and language.
 * Its [DeviceValues.current] follows the composition as those values change.
 */
@Composable
internal fun rememberDeviceValues(): DeviceValues {
    val windowSize = LocalWindowInfo.current.containerSize
    val context = DeviceContext(
        density = LocalDensity.current.density,
        screenWidthPx = windowSize.width,
        screenHeightPx = windowSize.height,
        theme = if (isSystemInDarkTheme()) Theme.Dark else Theme.Light,
        language = Locale.current.toLanguageTag(),
    )
    val state = remember { mutableStateOf(context) }
    SideEffect { state.value = context }
    return remember { StateDeviceValues(state = state) }
}

private class StateDeviceValues(private val state: State<DeviceContext>) : DeviceValues {
    override val current: DeviceContext get() = state.value
}
