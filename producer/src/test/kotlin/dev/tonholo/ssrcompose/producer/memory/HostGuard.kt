package dev.tonholo.ssrcompose.producer.memory

import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val KB_PER_MB = 1024L
private const val WATCH_PAUSE_MS = 500L
private const val HOST_GUARD_EXIT_CODE = 3
private const val SCREENS_BETWEEN_CHECKS = 25

/**
 * Keeps a run from starving its host. It stops opening screens [marginMb] above [floorMb] of available host memory, or
 * at [residentLimitMb], and ends the process when the host falls below [floorMb]. Without `/proc/meminfo`, as on
 * macOS, only the resident limit applies.
 */
internal data class HostGuard(val floorMb: Long, val marginMb: Long, val residentLimitMb: Long) {
    fun startWatching() {
        thread(isDaemon = true, name = "host-memory-guard") {
            while (true) {
                endIfHostBelowFloor()
                Thread.sleep(WATCH_PAUSE_MS)
            }
        }
    }

    fun mayOpenAfter(opened: Int): Boolean {
        if (opened % SCREENS_BETWEEN_CHECKS != 0) return true
        val available = ProcessMemory.hostAvailableKb()
        val hostHasRoom = available == null || available / KB_PER_MB >= floorMb + marginMb
        return hostHasRoom && ProcessMemory.residentKb() / KB_PER_MB < residentLimitMb
    }

    private fun endIfHostBelowFloor() {
        val available = ProcessMemory.hostAvailableKb() ?: return
        if (available / KB_PER_MB < floorMb) {
            println("MEMORY STOPPED: the host has ${available / KB_PER_MB} MB available, below $floorMb MB")
            exitProcess(HOST_GUARD_EXIT_CODE)
        }
    }
}
