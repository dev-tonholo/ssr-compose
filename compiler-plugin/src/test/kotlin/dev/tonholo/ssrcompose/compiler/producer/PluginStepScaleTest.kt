package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.testing.measure.MeasurementSummary
import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.compiler.testing.scale.BridgeScaleScenario
import dev.tonholo.ssrcompose.compiler.testing.scale.LargeModuleScenario
import dev.tonholo.ssrcompose.compiler.testing.scale.PluginStep
import dev.tonholo.ssrcompose.compiler.testing.scale.PluginStepClock
import dev.tonholo.ssrcompose.compiler.testing.scale.PluginStepCost
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginStepScaleTest {
    @TempDir
    lateinit var workDir: File

    @Test
    fun `given a clean compile of a small module, when its steps are timed, then every step ran on the real bridges`() {
        // Arrange
        val scenario = bridgeScenario(components = SMALL_COMPONENTS, parts = SMALL_PARTS, called = SMALL_CALLED)
        val module = LargeModuleScenario(workDir = workDir, scenario = scenario, files = SMALL_FILES)
        val clock = PluginStepClock()

        // Act
        val result = module.compileMeasured(heldCatalogs = 1, clock = clock)

        // Assert
        assertEquals(expected = SMALL_CALLED, actual = scenario.createdBridgeCount(result = result))
        assertEquals(expected = SMALL_FILES, actual = module.filesCallingScaledText(result = result))
        assertEquals(expected = REQUIRED_STEPS, actual = clock.measured.keys)
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given a clean compile of thousands of files, when the steps are timed, then their cost is reported`() {
        // Arrange
        val scenario = bridgeScenario(components = FULL_COMPONENTS, parts = FULL_PARTS, called = FULL_CALLED)
        val module = LargeModuleScenario(workDir = workDir, scenario = scenario, files = LARGE_FILES)
        val created = HELD_CATALOG_COUNTS.associateWith { held ->
            scenario.createdBridgeCount(result = module.compileMeasured(heldCatalogs = held, clock = PluginStepClock()))
        }
        assertEquals(expected = HELD_CATALOG_COUNTS.associateWith { FULL_CALLED }, actual = created)

        // Act
        val samples = HELD_CATALOG_COUNTS.associateWith { mutableListOf<Map<PluginStep, PluginStepCost>>() }
        repeat(times = COUNTED_ROUNDS) { round ->
            val order = if (round % 2 == 0) HELD_CATALOG_COUNTS else HELD_CATALOG_COUNTS.reversed()
            order.forEach { held ->
                val clock = PluginStepClock()
                module.compileMeasured(heldCatalogs = held, clock = clock)
                samples.getValue(held) += clock.measured.toMap()
            }
        }

        // Assert
        samples.forEach { (held, rounds) -> report(held = held, rounds = rounds) }
        assertEquals(expected = HELD_CATALOG_COUNTS.toSet(), actual = samples.keys)
    }

    private fun report(held: Int, rounds: List<Map<PluginStep, PluginStepCost>>) {
        PluginStep.entries.forEach { step ->
            println(lineOf(held = held, label = step.name, costs = rounds.map { it.getValue(step) }))
        }
        val totals = rounds.map { round ->
            PluginStepCost(
                nanos = round.values.sumOf { it.nanos },
                allocatedBytes = round.values.sumOf { it.allocatedBytes },
            )
        }
        println(lineOf(held = held, label = "Total", costs = totals))
    }

    private fun lineOf(held: Int, label: String, costs: List<PluginStepCost>): String {
        val time = MeasurementSummary.of(samples = costs.map { it.nanos / NANOS_PER_MILLI })
        val allocated = MeasurementSummary.of(samples = costs.map { it.allocatedBytes / BYTES_PER_MIB })
        return "SCALE | plugin step | $held catalogs | $label | time ms ${time.toText()} | " +
            "allocated MiB ${allocated.toText()} | rounds ${costs.size}"
    }

    private fun MeasurementSummary.toText(): String =
        "median ${"%.1f".format(median)} min ${"%.1f".format(min)} max ${"%.1f".format(max)}"

    private fun bridgeScenario(components: Int, parts: Int, called: Int) = BridgeScaleScenario(
        workDir = workDir,
        components = components,
        parts = parts,
        called = called,
        callingFunctions = CALLING_FUNCTIONS,
    )

    private companion object {
        const val SMALL_COMPONENTS = 60
        const val SMALL_PARTS = 3
        const val SMALL_CALLED = 2
        const val SMALL_FILES = 5
        const val FULL_COMPONENTS = 20_000
        const val FULL_PARTS = 50
        const val FULL_CALLED = 20
        const val LARGE_FILES = 3_000
        const val CALLING_FUNCTIONS = 1
        const val COUNTED_ROUNDS = 6
        const val NANOS_PER_MILLI = 1_000_000.0
        const val BYTES_PER_MIB = 1_048_576.0
        val HELD_CATALOG_COUNTS = listOf(1, 10)
        val REQUIRED_STEPS = setOf(
            PluginStep.ReadIndexes,
            PluginStep.ScanSources,
            PluginStep.DecodeNamedComponents,
            PluginStep.DeclareBridges,
            PluginStep.CreateBridgeBodies,
            PluginStep.CheckCalls,
            PluginStep.RewriteText,
        )
    }
}
