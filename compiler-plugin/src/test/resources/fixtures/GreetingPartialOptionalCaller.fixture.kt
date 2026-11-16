package app.caller

import androidx.compose.runtime.Composable
import app.bridge.v1.Greeting

@Composable
fun GreetingPartialOptionalCaller() {
    Greeting(title = "Welcome", onSelect = {}, subtitle = "Details")
}
