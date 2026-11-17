package app.caller

import androidx.compose.runtime.Composable
import app.bridge.unplaceable.PaymentSection

@Composable
fun UnplaceableReferenceCaller() {
    val reference = ::PaymentSection
    println(reference)
}
