package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import fixtures.external.ExternalLabel
import fixtures.external.ExternalNullableThemes

@ConsumerComposable
@Composable
fun ExternalAliasCard(
    label: ExternalLabel,
    themes: ExternalNullableThemes = null,
) {
}
