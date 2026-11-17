package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable

typealias ConsumerSlotAlias = @ConsumerComposable @Composable () -> Unit

@ProducerComposable
typealias ProducerSlotAlias = @Composable () -> Unit

typealias AnySlotAlias = @Composable () -> Unit

@ConsumerComposable
@Composable
fun TypealiasSlotContainer(
    rhsAnnotated: ConsumerSlotAlias,
    declarationAnnotated: ProducerSlotAlias,
    useSiteAnnotated: @ConsumerComposable AnySlotAlias,
    unannotated: AnySlotAlias,
) {
}
