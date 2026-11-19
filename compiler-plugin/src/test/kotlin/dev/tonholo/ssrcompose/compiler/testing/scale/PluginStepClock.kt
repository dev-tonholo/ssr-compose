package dev.tonholo.ssrcompose.compiler.testing.scale

import java.lang.management.ManagementFactory
import java.util.EnumMap
import com.sun.management.ThreadMXBean as AllocationCountingThreadBean

/** The work the compiler plugin adds to a compile. */
internal enum class PluginStep {
    ReadIndexes,
    ScanSources,
    DecodeNamedComponents,
    DeclareBridges,
    CreateBridgeBodies,
    CheckCalls,
}

/** What one step cost in one compile. */
internal data class PluginStepCost(val nanos: Long, val allocatedBytes: Long)

/**
 * Adds up the time and the allocation of each [PluginStep] in one compile. Work nested in a step counts in that step
 * too, so a total can be higher than the real cost and never lower.
 */
internal class PluginStepClock {
    private val threads = ManagementFactory.getThreadMXBean() as AllocationCountingThreadBean
    private val costs = EnumMap<PluginStep, PluginStepCost>(PluginStep::class.java)
    private val started = EnumMap<PluginStep, PluginStepCost>(PluginStep::class.java)

    /** The steps that ran, with what each one cost. */
    val measured: Map<PluginStep, PluginStepCost> get() = costs

    fun start(step: PluginStep) {
        started[step] = now()
    }

    fun stop(step: PluginStep) {
        add(step = step, begin = checkNotNull(started.remove(step)) { "The step $step was not started" })
    }

    fun <T> measure(step: PluginStep, action: () -> T): T {
        val begin = now()
        try {
            return action()
        } finally {
            add(step = step, begin = begin)
        }
    }

    private fun add(step: PluginStep, begin: PluginStepCost) {
        val end = now()
        val spent = costs[step] ?: PluginStepCost(nanos = 0L, allocatedBytes = 0L)
        costs[step] = PluginStepCost(
            nanos = spent.nanos + end.nanos - begin.nanos,
            allocatedBytes = spent.allocatedBytes + end.allocatedBytes - begin.allocatedBytes,
        )
    }

    private fun now() = PluginStepCost(nanos = System.nanoTime(), allocatedBytes = threads.currentThreadAllocatedBytes)
}
