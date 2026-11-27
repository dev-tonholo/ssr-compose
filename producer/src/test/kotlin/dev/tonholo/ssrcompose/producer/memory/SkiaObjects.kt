package dev.tonholo.ssrcompose.producer.memory

import org.jetbrains.skia.impl.Stats

private const val SMALLEST_SHOWN_PER_SCREEN = 0.5

/**
 * The native objects Skiko held at one moment, by class, from its own count. It counts with
 * `-Dskiko.collect.stat=true`.
 */
internal class SkiaObjects private constructor(private val held: Map<String, Int>) {
    /** The classes with at least one more object for every two of [screens] now than then, most first. */
    fun addedPerScreen(screens: Int): String {
        if (!Stats.enabled) return "not counted"
        val added = current().held
            .mapValues { (name, count) -> (count - (held[name] ?: 0)).toDouble() / screens }
            .filterValues { perScreen -> perScreen >= SMALLEST_SHOWN_PER_SCREEN }
            .entries
            .sortedByDescending { it.value }
        return added.joinToString { (name, perScreen) -> "$name %.1f".format(perScreen) }.ifEmpty { "none" }
    }

    companion object {
        fun current(): SkiaObjects = SkiaObjects(held = HashMap(Stats.allocated))
    }
}
