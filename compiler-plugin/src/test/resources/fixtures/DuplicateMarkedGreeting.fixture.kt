package fixtures.consumer.renderer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

@ConsumerComposable
@Composable
fun Greeting(title: String) {
}

@ConsumerComposable
@Composable
fun Greeting(title: String, subtitle: String) {
}
