package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.testing.measure.MeasurementPlan
import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.catalog.testing.measure.ScaleMeasurement
import dev.tonholo.ssrcompose.catalog.testing.measure.ScaleReport
import dev.tonholo.ssrcompose.compiler.producer.scan.SourceNameScan
import dev.tonholo.ssrcompose.compiler.testing.scale.BridgeScaleScenario
import dev.tonholo.ssrcompose.compiler.testing.scale.LargeModuleCase
import dev.tonholo.ssrcompose.compiler.testing.scale.LargeModuleScenario
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class LargeModuleScaleTest {
    @TempDir
    lateinit var workDir: File

    @Test
    fun `given an edited file of a module, when compiled alone, then its bridges are created from the scan`() {
        // Arrange
        val scenario = bridgeScenario(components = SMALL_COMPONENTS, parts = SMALL_PARTS, called = SMALL_CALLED)
        val module = LargeModuleScenario(workDir = workDir, scenario = scenario, files = SMALL_FILES)

        // Act
        val result = module.compile(case = LargeModuleCase.EditedFileOneCatalog)

        // Assert
        assertEquals(expected = SMALL_CALLED, actual = scenario.createdBridgeCount(result = result))
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given a module of thousands of files, when its names are scanned, then the cost is reported`() {
        // Arrange
        val scenario = bridgeScenario(components = FULL_COMPONENTS, parts = FULL_PARTS, called = FULL_CALLED)
        val module = LargeModuleScenario(workDir = workDir, scenario = scenario, files = LARGE_FILES)
        val scan = SourceNameScan(roots = listOf(module.sourceFolder))
        val oneCatalog = scenario.componentNames
        val tenCatalogs = oneCatalog.flatMapTo(destination = HashSet()) { name ->
            List(size = HELD_CATALOGS) { catalog -> "$name$catalog" }
        }

        // Act
        val reports = listOf(
            ScaleMeasurement.measure(label = "scan $LARGE_FILES files, one catalog", plan = SCAN_PLAN) {
                scan.namedAmong(names = oneCatalog)
            },
            ScaleMeasurement.measure(label = "scan $LARGE_FILES files, ten catalogs", plan = SCAN_PLAN) {
                scan.namedAmong(names = tenCatalogs)
            },
        )

        // Assert
        reports.forEach { report -> println(report.toLine()) }
        assertEquals(expected = emptySet(), actual = scan.namedAmong(names = oneCatalog))
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given a module of thousands of files, when each case is compiled, then the cost is reported`() {
        // Arrange
        val scenario = bridgeScenario(components = FULL_COMPONENTS, parts = FULL_PARTS, called = FULL_CALLED)
        val module = LargeModuleScenario(workDir = workDir, scenario = scenario, files = LARGE_FILES)
        val casesWithCatalog = MEASURED_CASES.filter { case -> case.heldCatalogs > 0 }
        val created = casesWithCatalog.associateWith { case ->
            scenario.createdBridgeCount(result = module.compile(case = case))
        }
        assertEquals(expected = casesWithCatalog.associateWith { FULL_CALLED }, actual = created)

        // Act
        val reports = MEASURED_CASES.partition { case -> case.isClean }.let { (clean, edited) ->
            measureInRotation(module = module, cases = clean, plan = CLEAN_PLAN) +
                measureInRotation(module = module, cases = edited, plan = EDIT_PLAN)
        }

        // Assert
        reports.forEach { report -> println(report.toLine()) }
        assertEquals(expected = EXPECTED_LABELS, actual = reports.map { it.label }.toSet())
    }

    private fun measureInRotation(
        module: LargeModuleScenario,
        cases: List<LargeModuleCase>,
        plan: MeasurementPlan,
    ): List<ScaleReport> {
        val actions = cases.associate { case ->
            val compile: () -> Unit = { module.compile(case = case) }
            "large module ${case.name}" to compile
        }
        return ScaleMeasurement.measureInRotation(actions = actions, plan = plan)
    }

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
        const val HELD_CATALOGS = 10
        const val CALLING_FUNCTIONS = 1
        val MEASURED_CASES = listOf(
            LargeModuleCase.CleanStandIns,
            LargeModuleCase.CleanOneCatalog,
            LargeModuleCase.CleanTenCatalogs,
            LargeModuleCase.EditedFileStandIns,
            LargeModuleCase.EditedFileOneCatalog,
            LargeModuleCase.EditedFileTenCatalogs,
        )
        val EXPECTED_LABELS = setOf(
            "large module CleanStandIns",
            "large module CleanOneCatalog",
            "large module CleanTenCatalogs",
            "large module EditedFileStandIns",
            "large module EditedFileOneCatalog",
            "large module EditedFileTenCatalogs",
        )
        val SCAN_PLAN = MeasurementPlan(warmups = 3, runs = 7)
        val CLEAN_PLAN = MeasurementPlan(warmups = 2, runs = 6)
        val EDIT_PLAN = MeasurementPlan(warmups = 3, runs = 7)
    }
}
