package dev.tonholo.ssrcompose.producer.memory

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Tag

private const val SCALE_TAG = "scale"
private const val REPORT_MAIN_CLASS = "dev.tonholo.ssrcompose.producer.memory.MemoryReportKt"
private const val SCREENS_PROPERTY = "ssrcompose.memory.screens"
private const val DEFAULT_SCREENS = 300
private const val SCREEN_WIDTH_PX = 1080L
private const val SCREEN_HEIGHT_PX = 2340L
private const val BYTES_PER_PIXEL = 4L
private const val BYTES_PER_KB = 1024L
private val runTimeLimitBase = 5.minutes
private val runTimeLimitPerScreen = 3.seconds

private val measurementFlags = listOf(
    "-Xms3g",
    "-Xmx3g",
    "-XX:+AlwaysPreTouch",
    "-XX:NativeMemoryTracking=summary",
    "-Djava.awt.headless=true",
    "-Dskiko.collect.stat=true",
)

private data class ScenarioResult(val openedScreens: Int, val totalKbPerScreen: Long)

@Tag(SCALE_TAG)
class LiveScreenMemoryScaleTest {
    @Test
    fun `given each scenario, when measured, then all screens open and one read once costs less than its raster`() {
        // Arrange
        val screens = System.getProperty(SCREENS_PROPERTY)?.toInt() ?: DEFAULT_SCREENS
        val rasterKb = SCREEN_WIDTH_PX * SCREEN_HEIGHT_PX * BYTES_PER_PIXEL / BYTES_PER_KB

        // Act
        val results = MemoryScenario.entries.associateWith { scenario ->
            measure(scenario = scenario, screens = screens)
        }

        // Assert
        assertEquals(
            expected = MemoryScenario.entries.associateWith { screens },
            actual = results.mapValues { (_, result) -> result.openedScreens },
        )
        val read = results.getValue(MemoryScenario.ReadHosts).totalKbPerScreen
        assertTrue(actual = read < rasterKb, message = "a screen costs $read KB, its raster alone is $rasterKb KB")
    }

    private fun measure(scenario: MemoryScenario, screens: Int): ScenarioResult {
        val result = runReport(scenario = scenario, screens = screens).single { it.startsWith(RESULT_PREFIX) }
        return ScenarioResult(
            openedScreens = result.valueOf(key = "screens").toInt(),
            totalKbPerScreen = result.valueOf(key = "totalKb").toLong(),
        )
    }

    private fun runReport(scenario: MemoryScenario, screens: Int): List<String> {
        val java = File(System.getProperty("java.home"), "bin/java").path
        val command = listOf(java) + measurementFlags +
            listOf("-cp", System.getProperty("java.class.path"), REPORT_MAIN_CLASS, scenario.label, "$screens")
        val log = File.createTempFile("ssrcompose-memory-${scenario.label}-", ".log").apply { deleteOnExit() }
        val process = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
        val timeLimit = runTimeLimitBase + runTimeLimitPerScreen * screens
        val ended = process.waitFor(timeLimit.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        if (!ended) process.destroyForcibly().waitFor()
        val output = log.readLines()
        output.filter { it.startsWith("MEMORY") }.forEach(::println)
        check(ended && process.exitValue() == 0) {
            val how = if (ended) "ended with ${process.exitValue()}" else "was stopped after $timeLimit"
            "${scenario.label} $how: ${output.joinToString(separator = "\n")}"
        }
        return output
    }

    private fun String.valueOf(key: String): String = substringAfter("$key=").substringBefore(" ")
}
