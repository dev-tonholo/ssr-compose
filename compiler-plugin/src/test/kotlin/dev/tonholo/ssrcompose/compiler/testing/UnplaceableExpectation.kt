package dev.tonholo.ssrcompose.compiler.testing

internal object UnplaceableExpectation {
    const val MESSAGE: String =
        "The composable 'PaymentSection' cannot be placed from the server: its required slot 'methodPicker' " +
            "receives arguments, and slots with arguments are not supported yet."
}
