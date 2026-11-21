package dev.tonholo.ssrcompose.producer.testing

import dev.tonholo.ssrcompose.producer.internal.scene.FrameTimeSource

private const val STEP_NANOS = 1_000_000L

/** A clock that advances by one fixed step on every read and counts the reads, which is one per rendered frame. */
internal class FakeFrameTimeSource : FrameTimeSource {
    private var now = 0L

    var reads = 0
        private set

    override fun nanoTime(): Long {
        reads++
        now += STEP_NANOS
        return now
    }
}
