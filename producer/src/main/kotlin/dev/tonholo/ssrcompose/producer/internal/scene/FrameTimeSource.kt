package dev.tonholo.ssrcompose.producer.internal.scene

/**
 * Tells a scene the time of the frame it renders, which is what `withFrameNanos` and every animation read.
 */
internal fun interface FrameTimeSource {
    /** The time of the frame in nanoseconds. It never goes back between two calls. */
    fun nanoTime(): Long

    companion object {
        /** The clock of the machine the producer runs on. */
        val Monotonic: FrameTimeSource = FrameTimeSource { System.nanoTime() }
    }
}
