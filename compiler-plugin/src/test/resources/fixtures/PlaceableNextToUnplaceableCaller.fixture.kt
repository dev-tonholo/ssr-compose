package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.Receipt

const val COMPONENT_NAMED_BUT_NOT_CALLED = "PaymentSection"

@Composable
fun PlaceableNextToUnplaceableCaller() {
    Receipt(title = "Thank you")
}
