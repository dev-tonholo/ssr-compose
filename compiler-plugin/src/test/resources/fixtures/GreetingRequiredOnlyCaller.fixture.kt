package app.caller

import androidx.compose.runtime.Composable
import app.bridge.v1.Greeting

@Composable
fun GreetingRequiredOnlyCaller() {
    Greeting(title = "Welcome", onSelect = {})
}
