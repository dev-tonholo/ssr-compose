package dev.tonholo.ssrcompose.consumer.testing

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

/** Shows [content] as if the system theme were [nightMode], one of the `Configuration.UI_MODE_NIGHT_*` values. */
@Composable
fun InSystemTheme(nightMode: Int, content: @Composable () -> Unit) {
    val configuration = Configuration(LocalConfiguration.current).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
    }
    CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
}
