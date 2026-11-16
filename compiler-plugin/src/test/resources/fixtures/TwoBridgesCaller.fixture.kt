package app.caller

import androidx.compose.runtime.Composable
import app.bridge.v1.Greeting as FirstGreeting
import app.bridge.v2.Greeting as SecondGreeting

@Composable
fun TwoBridgesCaller() {
    FirstGreeting(title = "First", onSelect = {}, subtitle = "Details")
    SecondGreeting(title = "Second", onSelect = {}, badge = "New")
}
