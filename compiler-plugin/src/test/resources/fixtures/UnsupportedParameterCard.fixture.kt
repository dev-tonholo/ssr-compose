package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

class Money(val cents: Long)

@ConsumerComposable
@Composable
fun PriceCard(
    title: String,
    price: Money,
) {
}
