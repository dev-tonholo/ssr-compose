package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable

@ConsumerComposable
@Composable
fun ConflictingSlotContainer(
    content: @ConsumerComposable @ProducerComposable @Composable () -> Unit,
) {
}
