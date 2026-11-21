package androidx.compose.foundation.text

import androidx.compose.runtime.Composable

@Composable
fun BasicText(label: Int) {
    check(label >= 0)
}

@Composable
fun UnsupportedOverloadCaller() {
    BasicText(label = 1)
}
