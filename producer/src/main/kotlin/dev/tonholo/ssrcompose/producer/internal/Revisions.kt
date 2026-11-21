package dev.tonholo.ssrcompose.producer.internal

import java.util.concurrent.atomic.AtomicLong

/**
 * Numbers the documents a session sends, from 1, rising by one with each. Share one across a session's screens.
 */
internal class Revisions {
    private val last = AtomicLong()

    fun next(): Long = last.incrementAndGet()
}
