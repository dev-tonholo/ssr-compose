package dev.tonholo.ssrcompose.producer.memory

import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

private const val DEFAULT_FLOOR_MB = 6000L
private const val DEFAULT_MARGIN_MB = 500L
private const val DEFAULT_RESIDENT_LIMIT_MB = 22000L
private const val ARGUMENT_COUNT = 2

private val allocatorVariables =
    listOf("MALLOC_ARENA_MAX", "MALLOC_TRIM_THRESHOLD_", "MALLOC_MMAP_THRESHOLD_", "LD_PRELOAD")

/**
 * Holds `<screens>` screens of one `<scenario>` open and prints what each one costs in memory. Run it with the heap
 * fixed and touched up front (`-Xms` equal to `-Xmx`, `-XX:+AlwaysPreTouch`), so that resident memory that grows while
 * screens open is native memory, and with `-XX:NativeMemoryTracking=summary` to tell the JVM's own share apart.
 *
 * `-Dskiko.collect.stat=true` adds Skiko's counts of native objects. `-Dmemory.collectEvery=N` collects garbage every
 * N screens while opening. `-Dmemory.cycles=N` then opens and closes the screens N more times, to tell a leak from
 * memory the allocator keeps. `-Dmemory.hostFloorMb`, `-Dmemory.hostMarginMb` and `-Dmemory.residentLimitMb` set the
 * [HostGuard].
 */
fun main(arguments: Array<String>) {
    require(arguments.size == ARGUMENT_COUNT) { "Usage: MemoryReportKt <scenario> <screens>" }
    val (label, screens) = arguments
    val guard = HostGuard(
        floorMb = longProperty(name = "memory.hostFloorMb", default = DEFAULT_FLOOR_MB),
        marginMb = longProperty(name = "memory.hostMarginMb", default = DEFAULT_MARGIN_MB),
        residentLimitMb = longProperty(name = "memory.residentLimitMb", default = DEFAULT_RESIDENT_LIMIT_MB),
    )
    guard.startWatching()
    val run = MemoryRun(
        scenario = MemoryScenario.labelled(label = label),
        screens = screens.toInt(),
        guard = guard,
        collectEvery = longProperty(name = "memory.collectEvery", default = 0).toInt(),
    )
    printEnvironment(run = run)
    run.measure(cycles = longProperty(name = "memory.cycles", default = 0).toInt())
    exitProcess(0)
}

private fun printEnvironment(run: MemoryRun) {
    val allocator = allocatorVariables.joinToString { name -> "$name=${System.getenv(name) ?: "unset"}" }
    run.print(
        "os ${System.getProperty("os.name")} ${System.getProperty("os.arch")}, " +
            "java ${System.getProperty("java.version")}, cpus ${Runtime.getRuntime().availableProcessors()}",
    )
    run.print("allocator: $allocator")
    run.print("jvm flags: ${ManagementFactory.getRuntimeMXBean().inputArguments}")
}

private fun longProperty(name: String, default: Long): Long = System.getProperty(name)?.toLong() ?: default
