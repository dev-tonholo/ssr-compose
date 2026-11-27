package dev.tonholo.ssrcompose.producer.memory

import java.io.File
import java.lang.management.ManagementFactory
import javax.management.ObjectName

private const val BYTES_PER_KB = 1024L
private const val COLLECTIONS = 10
private const val COLLECTION_PAUSE_MS = 200L
private const val DIAGNOSTIC_BEAN = "com.sun.management:type=DiagnosticCommand"
private val whitespace = Regex(pattern = "\\s+")
private val nativeTrackingTotal = Regex(pattern = "Total: reserved=\\d+KB.*?committed=(\\d+)KB")

/**
 * What the process holds at one moment, in KB. [jvmCommittedKb] is what the JVM's native memory tracking counts, the
 * heap included, and is null without `-XX:NativeMemoryTracking=summary`. Skia's own allocations are not in it.
 */
internal data class ProcessMemory(
    val heapUsedKb: Long,
    val heapCommittedKb: Long,
    val residentKb: Long,
    val jvmCommittedKb: Long?,
) {
    override fun toString(): String =
        "heap used $heapUsedKb KB, heap committed $heapCommittedKb KB, resident $residentKb KB, " +
            "JVM native outside the heap ${jvmCommittedKb?.let { it - heapCommittedKb } ?: "not tracked"} KB"

    companion object {
        /** Collects garbage first, and gives Skiko's cleaner thread time to free what became unreachable. */
        fun settled(): ProcessMemory {
            repeat(COLLECTIONS) {
                System.gc()
                Thread.sleep(COLLECTION_PAUSE_MS)
            }
            val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage
            return ProcessMemory(
                heapUsedKb = heap.used / BYTES_PER_KB,
                heapCommittedKb = heap.committed / BYTES_PER_KB,
                residentKb = residentKb(),
                jvmCommittedKb = jvmCommittedKb(),
            )
        }

        fun residentKb(): Long = statusKb(path = "/proc/self/status", key = "VmRSS:") ?: residentKbFromPs()

        /** What the host can still give out, or null where there is no `/proc/meminfo`, as on macOS. */
        fun hostAvailableKb(): Long? = statusKb(path = "/proc/meminfo", key = "MemAvailable:")

        /** Asks the C library to give freed memory back to the system, or says why the JVM cannot, as on macOS. */
        fun trimNativeHeap(): String = runCatching { diagnosticCommand(operation = "systemTrimNativeHeap") }
            .map { output -> output.trim().replace(oldChar = '\n', newChar = ' ') }
            .getOrElse { error -> "not available: ${error.javaClass.simpleName}" }

        private fun jvmCommittedKb(): Long? = runCatching {
            diagnosticCommand(operation = "vmNativeMemory", arguments = arrayOf("summary"))
        }.getOrNull()?.let { summary -> nativeTrackingTotal.find(summary)?.groupValues?.get(1)?.toLong() }

        private fun diagnosticCommand(operation: String, arguments: Array<String> = emptyArray()): String =
            ManagementFactory.getPlatformMBeanServer().invoke(
                ObjectName(DIAGNOSTIC_BEAN),
                operation,
                arrayOf<Any>(arguments),
                arrayOf(Array<String>::class.java.name),
            ) as String

        private fun statusKb(path: String, key: String): Long? = File(path)
            .takeIf { it.exists() }
            ?.readLines()
            ?.firstOrNull { it.startsWith(key) }
            ?.split(whitespace)
            ?.get(1)
            ?.toLong()

        private fun residentKbFromPs(): Long {
            val pid = ProcessHandle.current().pid().toString()
            val ps = ProcessBuilder("ps", "-o", "rss=", "-p", pid).start()
            return ps.inputStream.bufferedReader().use { it.readText() }.trim().toLong()
        }
    }
}
