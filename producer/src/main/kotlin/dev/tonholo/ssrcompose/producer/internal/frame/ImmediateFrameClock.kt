package dev.tonholo.ssrcompose.producer.internal.frame

import androidx.compose.runtime.MonotonicFrameClock

/**
 * A frame clock for a server, where no screen draws. Every frame is delivered as soon as it is asked for, so a
 * screen costs nothing while it waits for a change.
 *
 * Frames are not spaced out. An effect that asks for a frame in a loop runs without any pause, using all of its
 * dispatcher's time.
 */
internal object ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(System.nanoTime())
}
