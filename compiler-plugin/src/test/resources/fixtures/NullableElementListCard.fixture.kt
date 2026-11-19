package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

@ConsumerComposable
@Composable
fun NullableElementListCard(
    labels: List<String?>,
) {
}
