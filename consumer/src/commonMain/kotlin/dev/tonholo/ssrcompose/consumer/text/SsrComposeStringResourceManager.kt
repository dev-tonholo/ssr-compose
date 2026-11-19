package dev.tonholo.ssrcompose.consumer.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.tonholo.ssrcompose.consumer.internal.text.EnglishStringResourceManager

/**
 * Chooses the text the library shows for each [SsrComposeStringResource].
 *
 * An implementation must return a text for every key and must not throw: a failure during composition stops the
 * app's recomposer. Keep the instance stable between compositions, for example with `remember`.
 */
public interface SsrComposeStringResourceManager {
    /**
     * Returns the text for [resource].
     *
     * @param arguments the values the text of [resource] needs. No key takes any today.
     */
    @Composable
    public fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String
}

/**
 * The manager the library reads its texts from. Its default gives the English text. An app that replaces only some
 * texts reads the current manager before it provides its own, and falls back to it for the other keys.
 */
public val LocalSsrComposeStringResourceManager: ProvidableCompositionLocal<SsrComposeStringResourceManager> =
    staticCompositionLocalOf<SsrComposeStringResourceManager> { EnglishStringResourceManager }
