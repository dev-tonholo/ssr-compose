package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.Receipt

@Composable
fun PlaceableNextToUnplaceableCaller() {
    Receipt(title = "Thank you")
}
