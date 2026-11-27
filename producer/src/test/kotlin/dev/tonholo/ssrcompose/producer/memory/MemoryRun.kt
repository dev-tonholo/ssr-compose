package dev.tonholo.ssrcompose.producer.memory

private const val SCREENS_PER_SMALL_RUN = 20
private const val NANOS_PER_MILLI = 1_000_000L

internal const val RESULT_PREFIX = "MEMORY RESULT"

/** One measurement of [screens] screens of [scenario] in this JVM. */
internal class MemoryRun(
    private val scenario: MemoryScenario,
    private val screens: Int,
    private val guard: HostGuard,
    private val collectEvery: Int,
) {
    fun print(text: String) {
        println("MEMORY ${scenario.label} | $text")
    }

    /** Opens, reads the memory, closes, trims, and then repeats opening and closing [cycles] more times. */
    fun measure(cycles: Int) {
        scenario.newSessions().use { it.open(count = SCREENS_PER_SMALL_RUN) { true } }
        val base = ProcessMemory.settled()
        print("base, after $SCREENS_PER_SMALL_RUN screens opened and closed: $base")
        val skia = SkiaObjects.current()
        val sessions = scenario.newSessions()
        val opened = open(sessions = sessions, skia = skia)
        val cost = ScreenCost.between(base = base, live = ProcessMemory.settled(), screens = opened)
        print("per screen: $cost")
        print("after collecting: Skia objects per screen ${skia.addedPerScreen(screens = opened)}")
        if (sessions.framesRendered != null) {
            print("one state change in one other session made these render ${framesForAnotherChange(sessions)} frames")
        }
        closeAndTrim(sessions = sessions, base = base, screens = opened)
        print("after closing: Skia objects per screen ${skia.addedPerScreen(screens = opened)}")
        repeat(cycles) { cycle ->
            val again = scenario.newSessions()
            again.open(count = opened, mayOpenAfter = guard::mayOpenAfter)
            print("cycle ${cycle + 1}:")
            closeAndTrim(sessions = again, base = base, screens = opened)
        }
        println(
            "$RESULT_PREFIX scenario=${scenario.label} screens=$opened heapKb=${cost.heapKb} " +
                "nativeKb=${cost.nativeKb} totalKb=${cost.totalKb}",
        )
    }

    private fun open(sessions: MemorySessions, skia: SkiaObjects): Int {
        val startedAt = System.nanoTime()
        val opened = sessions.open(count = screens) { count ->
            if (collectEvery > 0 && count % collectEvery == 0) System.gc()
            guard.mayOpenAfter(opened = count)
        }
        check(opened > 0) { "No screen could be opened: the host guard refused the first one" }
        val millis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI
        print(
            "before collecting: resident ${ProcessMemory.residentKb()} KB, " +
                "Skia objects per screen ${skia.addedPerScreen(screens = opened)}",
        )
        sessions.framesRendered?.let { frames ->
            print("frames rendered while opening: $frames, %.1f per screen".format(frames.toDouble() / opened))
        }
        print("$opened of $screens screens open after $millis ms: ${ProcessMemory.settled()}")
        return opened
    }

    private fun closeAndTrim(sessions: MemorySessions, base: ProcessMemory, screens: Int) {
        sessions.close()
        val closed = ProcessMemory.settled()
        print("all closed and collected: $closed")
        print("native trim: ${ProcessMemory.trimNativeHeap()}")
        val trimmed = ProcessMemory.settled()
        print("after the native trim: $trimmed")
        print(
            "resident above the base, per screen: ${(closed.residentKb - base.residentKb) / screens} KB closed, " +
                "${(trimmed.residentKb - base.residentKb) / screens} KB trimmed",
        )
    }
}
