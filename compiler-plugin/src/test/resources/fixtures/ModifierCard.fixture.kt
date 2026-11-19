package fixtures.consumer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

typealias CardModifier = Modifier

@ConsumerComposable
@Composable
fun ModifierCard(
    title: String,
    modifier: Modifier = Modifier,
    secondaryModifier: CardModifier = Modifier,
) {
}
