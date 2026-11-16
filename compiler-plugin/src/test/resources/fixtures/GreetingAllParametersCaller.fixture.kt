package app.caller

import androidx.compose.runtime.Composable
import app.bridge.v1.Greeting

@Composable
fun GreetingAllParametersCaller() {
    var selected = ""
    Greeting(
        title = "Welcome",
        onSelect = { chosen -> selected = chosen },
        subtitle = "Details",
    ) {
        Greeting(title = "Nested", onSelect = { chosen -> selected = chosen })
    }
}
