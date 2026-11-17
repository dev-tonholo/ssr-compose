package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.PaymentSection
import app.bridge.unplaceable.Receipt

@Composable
fun UnplaceableCaller() {
    PaymentSection(title = "Pay") { method ->
        Receipt(title = method)
    }
}
