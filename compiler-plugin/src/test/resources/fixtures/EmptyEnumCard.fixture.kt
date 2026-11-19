package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

enum class EmptyChoice

@ConsumerComposable
@Composable
fun EmptyEnumCard(
    choice: EmptyChoice,
) {
}
