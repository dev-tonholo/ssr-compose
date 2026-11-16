package app.caller

import androidx.compose.runtime.Composable
import app.bridge.v1.Greeting as FirstGreeting

@Composable
fun TwoBridgesWrongParameterCaller() {
    FirstGreeting(title = "First", onSelect = {}, badge = "New")
}
