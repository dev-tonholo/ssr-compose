package fixtures.consumer.renderer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

object RecordedRender {
    var greeting: String? = null
    var banner: String? = null
}

@ConsumerComposable
@Composable
fun Greeting(title: String) {
    RecordedRender.greeting = "greeting:$title"
}

@Composable
fun Greeting(count: Int) {
    RecordedRender.greeting = "unmarked:$count"
}
