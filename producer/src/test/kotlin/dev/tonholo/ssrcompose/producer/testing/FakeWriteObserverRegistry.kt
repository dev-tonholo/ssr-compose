package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.snapshots.ObserverHandle

internal class FakeWriteObserverRegistry {
    var registered = 0
        private set
    var disposed = 0
        private set

    val register: ((Any) -> Unit) -> ObserverHandle = {
        registered++
        ObserverHandle { disposed++ }
    }
}
