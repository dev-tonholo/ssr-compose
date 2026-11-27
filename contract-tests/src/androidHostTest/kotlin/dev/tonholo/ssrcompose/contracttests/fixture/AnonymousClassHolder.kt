package dev.tonholo.ssrcompose.contracttests.fixture

internal object AnonymousClassHolder {
    val anonymous: Runnable = object : Runnable {
        override fun run() = Unit
    }
}
