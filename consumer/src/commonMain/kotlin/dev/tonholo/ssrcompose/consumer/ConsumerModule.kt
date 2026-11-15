package dev.tonholo.ssrcompose.consumer

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable

class ConsumerModule {
    val name: String = "consumer"

    @Composable
    internal fun Label() {
        BasicText(text = name)
    }
}
