package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.snapshots.ObserverHandle

/** Counts the observers registered through [register] and the handles disposed. */
internal class FakeObserverRegistry<Observer> {
    var registered = 0
        private set
    var disposed = 0
        private set

    val register: (Observer) -> ObserverHandle = {
        registered++
        ObserverHandle { disposed++ }
    }
}
