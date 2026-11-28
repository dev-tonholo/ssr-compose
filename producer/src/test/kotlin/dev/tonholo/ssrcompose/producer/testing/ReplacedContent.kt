package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.mutableIntStateOf
import java.lang.ref.WeakReference

private const val COLLECTIONS = 20
private const val COLLECTION_PAUSE_MS = 20L

/** What one generation of replaced content reads. Whatever still holds that content holds this too. */
internal class ReplacedContent(val generation: Int) {
    val shiftPx = mutableIntStateOf(value = 0)
}

/** Makes [ReplacedContent] and keeps only weak references, so it holds none of it alive itself. */
internal class ReplacedContentTracker {
    private val made = mutableListOf<WeakReference<ReplacedContent>>()

    val madeCount: Int get() = made.size

    fun make(generation: Int): ReplacedContent =
        ReplacedContent(generation = generation).also { made += WeakReference(it) }

    /** The generations still reachable, collecting garbage until none is older than [oldestExpected] or giving up. */
    fun reachableGenerations(oldestExpected: Int): List<Int> {
        repeat(COLLECTIONS) {
            System.gc()
            val reachable = reachableNow()
            if (reachable.all { it >= oldestExpected }) return reachable
            Thread.sleep(COLLECTION_PAUSE_MS)
        }
        return reachableNow()
    }

    private fun reachableNow(): List<Int> = made.mapNotNull { it.get()?.generation }
}
