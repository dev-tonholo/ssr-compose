package dev.tonholo.ssrcompose.catalog.testing.measure

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import com.sun.management.ThreadMXBean as AllocationCountingThreadBean

private const val BYTES_PER_MIB = 1024.0 * 1024.0

/**
 * How often a measurement repeats.
 *
 * @property warmups runs that are thrown away so the JVM has compiled the code being timed.
 * @property runs runs that count.
 */
data class MeasurementPlan(val warmups: Int, val runs: Int)

/**
 * The result of one measurement.
 *
 * @property timeMillis wall clock time of each run.
 * @property allocatedMib bytes the measuring thread allocated in each run, from `ThreadMXBean.getThreadAllocatedBytes`.
 * @property peakHeapMib the highest heap use seen in each run, from the peak usage of every heap memory pool, reset
 * after a garbage collection right before the run.
 */
data class ScaleReport(
    val label: String,
    val plan: MeasurementPlan,
    val timeMillis: MeasurementSummary,
    val allocatedMib: MeasurementSummary,
    val peakHeapMib: MeasurementSummary,
) {
    /** One line that the scale task prints and research.md R16 quotes. */
    fun toLine(): String =
        "SCALE | $label | time ms ${timeMillis.toText()} | allocated MiB ${allocatedMib.toText()} | " +
            "peak heap MiB ${peakHeapMib.toText()} | warmups ${plan.warmups} runs ${plan.runs}"

    private fun MeasurementSummary.toText(): String =
        "median ${median.rounded()} min ${min.rounded()} max ${max.rounded()}"

    private fun Double.rounded(): String = "%.1f".format(this)
}

/** Runs an action repeatedly and reports its time and memory. */
object ScaleMeasurement {
    private val memory = ManagementFactory.getMemoryMXBean()
    private val heapPools = ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }
    private val threads = ManagementFactory.getThreadMXBean() as AllocationCountingThreadBean

    fun measure(label: String, plan: MeasurementPlan, action: () -> Unit): ScaleReport {
        repeat(times = plan.warmups) { action() }
        val samples = List(size = plan.runs) { sampleOf(action = action) }
        return reportOf(label = label, plan = plan, samples = samples)
    }

    /**
     * Measures [actions] in rounds, so that no action is the one that always runs first on a cold JVM. Each warmup
     * round runs every action, and each counted round starts with the next action.
     *
     * @return one report for each action, in the order of [actions].
     */
    fun measureInRotation(actions: Map<String, () -> Unit>, plan: MeasurementPlan): List<ScaleReport> {
        val labels = actions.keys.toList()
        repeat(times = plan.warmups) { labels.forEach { label -> actions.getValue(label).invoke() } }
        val samples = labels.associateWith { mutableListOf<Sample>() }
        repeat(times = plan.runs) { round ->
            val start = round % labels.size
            (labels.drop(n = start) + labels.take(n = start)).forEach { label ->
                samples.getValue(label) += sampleOf(action = actions.getValue(label))
            }
        }
        return labels.map { label -> reportOf(label = label, plan = plan, samples = samples.getValue(label)) }
    }

    private fun reportOf(label: String, plan: MeasurementPlan, samples: List<Sample>): ScaleReport =
        ScaleReport(
            label = label,
            plan = plan,
            timeMillis = MeasurementSummary.of(samples = samples.map { it.timeMillis }),
            allocatedMib = MeasurementSummary.of(samples = samples.map { it.allocatedMib }),
            peakHeapMib = MeasurementSummary.of(samples = samples.map { it.peakHeapMib }),
        )

    private fun sampleOf(action: () -> Unit): Sample {
        memory.gc()
        heapPools.forEach { it.resetPeakUsage() }
        val allocatedBefore = threads.currentThreadAllocatedBytes
        val time = measureTime(action)
        return Sample(
            timeMillis = time.toDouble(DurationUnit.MILLISECONDS),
            allocatedMib = (threads.currentThreadAllocatedBytes - allocatedBefore) / BYTES_PER_MIB,
            peakHeapMib = heapPools.sumOf { it.peakUsage.used } / BYTES_PER_MIB,
        )
    }

    private class Sample(val timeMillis: Double, val allocatedMib: Double, val peakHeapMib: Double)
}
