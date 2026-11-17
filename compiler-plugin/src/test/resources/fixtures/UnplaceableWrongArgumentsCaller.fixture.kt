package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.PaymentSection

@Composable
fun UnplaceableWrongArgumentsCaller() {
    PaymentSection(title = 1)
}
