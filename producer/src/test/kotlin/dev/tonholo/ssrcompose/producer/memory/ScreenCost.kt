package dev.tonholo.ssrcompose.producer.memory

/**
 * What one open screen costs, in KB. [nativeKb] is the growth of resident memory per screen, which with a heap touched
 * up front is memory outside the heap, and [jvmNativeKb] is the part of it that the JVM itself tracks.
 */
internal data class ScreenCost(val heapKb: Long, val nativeKb: Long, val jvmNativeKb: Long?) {
    /** The live heap and the native memory of one screen, with no headroom for the collector. */
    val totalKb: Long get() = heapKb + nativeKb

    override fun toString(): String =
        "live heap $heapKb KB, native $nativeKb KB (of which the JVM tracks ${jvmNativeKb ?: "nothing"} KB), " +
            "together $totalKb KB"

    companion object {
        fun between(base: ProcessMemory, live: ProcessMemory, screens: Int): ScreenCost = ScreenCost(
            heapKb = (live.heapUsedKb - base.heapUsedKb) / screens,
            nativeKb = (live.residentKb - base.residentKb) / screens,
            jvmNativeKb = live.jvmOutsideHeapKb()?.let { after ->
                base.jvmOutsideHeapKb()?.let { before -> (after - before) / screens }
            },
        )

        private fun ProcessMemory.jvmOutsideHeapKb(): Long? = jvmCommittedKb?.let { it - heapCommittedKb }
    }
}
