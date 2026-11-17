package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable

@ConsumerComposable
@Composable
fun SlotContainer(
    content: @ConsumerComposable @Composable () -> Unit,
    producerContent: @ProducerComposable @Composable () -> Unit,
    anyContent: @Composable () -> Unit = {},
) {
}
