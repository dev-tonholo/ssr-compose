package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.PaymentSection
import app.bridge.unplaceable.Receipt
import app.bridge.v1.Greeting

@Composable
fun OwnBridgeAndUnplaceableCaller() {
    Greeting(title = "Welcome", onSelect = {})
    PaymentSection(title = "Pay") { method ->
        Receipt(title = method)
    }
}
