package fixtures.consumer.renderer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

@ConsumerComposable
@Composable
fun Banner(headline: String) {
    RecordedRender.banner = "banner:$headline"
}
