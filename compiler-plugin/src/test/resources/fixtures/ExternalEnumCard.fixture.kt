package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import fixtures.external.ExternalTheme

@ConsumerComposable
@Composable
fun ExternalEnumCard(
    theme: ExternalTheme,
    fallbackThemes: List<ExternalTheme> = emptyList(),
) {
}
