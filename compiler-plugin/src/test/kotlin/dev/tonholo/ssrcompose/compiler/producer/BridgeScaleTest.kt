package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleWriter
import dev.tonholo.ssrcompose.catalog.index.CatalogIndex
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexReader
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.testing.ParameterPalette
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalog
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalogShape
import dev.tonholo.ssrcompose.catalog.testing.measure.MeasurementPlan
import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.catalog.testing.measure.ScaleMeasurement
import dev.tonholo.ssrcompose.compiler.testing.scale.BridgeScaleScenario
import dev.tonholo.ssrcompose.compiler.testing.scale.CompileCase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BridgeScaleTest {
    @TempDir
    lateinit var workDir: File

    @Test
    fun `given code that names two components, when compiled, then only those two get a function`() {
        // Arrange
        val scenario = smallScenario()

        // Act
        val result = scenario.compile(case = CompileCase.OneFullCatalog)

        // Assert
        assertEquals(expected = SMALL_CALLED, actual = scenario.createdBridgeCount(result = result))
    }

    @Test
    fun `given a change to a component nobody calls, when compiled again, then the caller classes stay the same`() {
        // Arrange
        val scenario = smallScenario()
        val before = scenario.callerClasses(result = scenario.compile(case = CompileCase.OneFullCatalog))

        // Act
        val after = scenario.callerClasses(result = scenario.compileWithOneUncalledComponentChanged())

        // Assert
        assertEquals(expected = before.keys, actual = after.keys)
        before.forEach { (name, bytes) ->
            assertContentEquals(expected = bytes, actual = after.getValue(name), message = name)
        }
    }

    @Test
    fun `given a change to a component nobody calls, when compiled again, then the bridge classes stay the same`() {
        // Arrange
        val scenario = smallScenario()
        val before = scenario.bridgeClasses(result = scenario.compile(case = CompileCase.OneFullCatalog))

        // Act
        val after = scenario.bridgeClasses(result = scenario.compileWithOneUncalledComponentChanged())

        // Assert
        assertEquals(expected = before.keys, actual = after.keys)
        assertTrue(actual = before.isNotEmpty())
        before.forEach { (name, bytes) ->
            assertContentEquals(expected = bytes, actual = after.getValue(name), message = name)
        }
    }

    @Test
    fun `given every compile case at a small size, when compiled, then each one succeeds`() {
        // Arrange
        val scenario = smallScenario()

        // Act
        val results = CompileCase.entries.associateWith { case -> scenario.compile(case = case).exitCode }

        // Assert
        assertEquals(expected = CompileCase.entries.associateWith { ExitCode.OK }, actual = results)
    }

    @Test
    fun `given a small catalog, when its index is built and read, then it finds every component`() {
        // Arrange
        val catalog = SyntheticCatalog.generate(shape = fullPaletteShape(components = SMALL_COMPONENTS))

        // Act
        val index = CatalogIndexReader.read(bytes = CatalogIndexWriter.write(catalog = catalog))

        // Assert
        assertEquals(expected = SMALL_COMPONENTS, actual = index.componentNames.size)
        assertEquals(expected = descriptorsOf(catalog), actual = decodedComponents(index))
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given the full catalog, when each index format is built and read, then its cost is reported`() {
        // Arrange
        val catalog = SyntheticCatalog.generate(shape = fullPaletteShape(components = FULL_COMPONENTS))
        val files = CatalogBundleWriter.write(catalog = catalog)

        // Act
        val indexBytes = measureIndex(catalog = catalog) {
            CatalogBundleReader.read(manifest = files.manifest, readPart = files.parts::getValue)
        }

        // Assert
        val index = CatalogIndexReader.read(bytes = indexBytes)
        assertEquals(expected = FULL_COMPONENTS, actual = index.componentNames.size)
        assertEquals(expected = descriptorsOf(catalog), actual = decodedComponents(index))
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given the full catalog, when the cheap cases are compiled in rotation, then their cost is reported`() {
        // Arrange
        val scenario = fullScenario()
        val created = scenario.createdBridgeCount(result = scenario.compile(case = CompileCase.OneFullCatalog))
        val actions = CompileCase.entries
            .associate { case ->
                val compile: () -> Unit = { scenario.compile(case = case) }
                "compile ${case.name}" to compile
            }

        // Act
        val reports = ScaleMeasurement.measureInRotation(actions = actions, plan = ROTATION_PLAN)

        // Assert
        reports.forEach { report -> println(report.toLine()) }
        assertEquals(expected = FULL_CALLED, actual = created)
        assertEquals(expected = actions.keys.toList(), actual = reports.map { it.label })
    }

    private fun measureIndex(catalog: Catalog, readBundle: () -> Catalog): ByteArray {
        val bytes = CatalogIndexWriter.write(catalog = readBundle())
        val allNames = CatalogIndexReader.read(bytes = bytes).componentNames
        val calledNames = allNames.take(FULL_CALLED)
        var calledParameters = 0L
        var allParameters = 0L
        fun openIndex() = CatalogIndexReader.read(bytes = bytes)
        val reports = listOf(
            ScaleMeasurement.measure(label = "index build from bundle", plan = FORMAT_PLAN) {
                CatalogIndexWriter.write(catalog = readBundle())
            },
            ScaleMeasurement.measure(label = "index open and read 20", plan = FORMAT_PLAN) {
                calledParameters += decodedParameters(index = openIndex(), names = calledNames)
            },
            ScaleMeasurement.measure(label = "index open and read all", plan = FORMAT_PLAN) {
                allParameters += decodedParameters(index = openIndex(), names = allNames)
            },
        )
        reports.forEach { report -> println(report.toLine()) }
        println("SCALE | index bytes | ${bytes.size}")
        assertEquals(expected = parametersRead(catalog, calledNames), actual = calledParameters)
        assertEquals(expected = parametersRead(catalog, allNames), actual = allParameters)
        return bytes
    }

    private fun decodedParameters(index: CatalogIndex, names: List<String>): Long =
        names.sumOf { name -> index.component(name = name)?.parameters?.size?.toLong() ?: 0L }

    private fun parametersRead(catalog: Catalog, names: List<String>): Long {
        val descriptors = descriptorsOf(catalog)
        val rounds = FORMAT_PLAN.warmups + FORMAT_PLAN.runs
        return rounds * names.sumOf { name -> descriptors.getValue(name).parameters.size.toLong() }
    }

    private fun descriptorsOf(catalog: Catalog): Map<String, ComponentDescriptor> =
        catalog.parts.flatMap { it.components }.associateBy { it.name }

    private fun decodedComponents(index: CatalogIndex): Map<String, ComponentDescriptor?> =
        index.componentNames.associateWith { name -> index.component(name = name) }

    private fun fullScenario() = BridgeScaleScenario(
        workDir = workDir,
        components = FULL_COMPONENTS,
        parts = FULL_PARTS,
        called = FULL_CALLED,
        callingFunctions = FULL_CALLING_FUNCTIONS,
    )

    private fun smallScenario() = BridgeScaleScenario(
        workDir = workDir,
        components = SMALL_COMPONENTS,
        parts = SMALL_PARTS,
        called = SMALL_CALLED,
        callingFunctions = SMALL_CALLING_FUNCTIONS,
    )

    private fun fullPaletteShape(components: Int) = SyntheticCatalogShape(
        components = components,
        parts = FULL_PARTS,
        seed = SEED,
        palette = ParameterPalette.Full,
    )

    private companion object {
        const val SEED = 20_000L
        const val SMALL_COMPONENTS = 60
        const val SMALL_PARTS = 3
        const val SMALL_CALLED = 2
        const val SMALL_CALLING_FUNCTIONS = 4
        const val FULL_COMPONENTS = 20_000
        const val FULL_PARTS = 50
        const val FULL_CALLED = 20
        const val FULL_CALLING_FUNCTIONS = 400
        val FORMAT_PLAN = MeasurementPlan(warmups = 5, runs = 7)
        val ROTATION_PLAN = MeasurementPlan(warmups = 5, runs = 12)
    }
}
