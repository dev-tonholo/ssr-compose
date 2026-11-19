package dev.tonholo.ssrcompose.compiler.testing.scale

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexWriter
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.testing.ParameterPalette
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalog
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalogShape
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.File

/**
 * A server module that calls [called] of the [components] bridge functions, compiled in each [CompileCase]. The
 * cases that measure `mode=check` compile [callingFunctions] functions of 25 calls each instead.
 */
internal class BridgeScaleScenario(
    private val workDir: File,
    components: Int,
    parts: Int,
    called: Int,
    private val callingFunctions: Int,
) {
    private val allComponents: List<ComponentDescriptor> = SyntheticCatalog
        .generate(
            shape = SyntheticCatalogShape(
                components = components,
                parts = parts,
                seed = SEED,
                palette = ParameterPalette.BridgeExperiment,
            ),
        )
        .parts
        .flatMap { it.components }
        .sortedBy { it.name }
    private val calledComponents = allComponents.take(called)
    private val bridgeCallerSource =
        ScaleSources.bridgeCaller(components = calledComponents, bridgePackage = packageOf(catalog = 0))
    private val fullIndex = HeldComponents(name = "full") { allComponents }
    private val calledIndex = HeldComponents(name = "called") { calledComponents }
    private val changedIndex = HeldComponents(name = "changed") { withOneUncalledChanged() }
    private val writtenIndexes = HashMap<String, File>()

    fun compile(case: CompileCase): JvmCompilationResult {
        val result = when (case) {
            CompileCase.StandIns ->
                compileWithComposeOnly(source = ScaleSources.standInCaller(components = calledComponents))

            CompileCase.CalledComponentsCatalog ->
                compileBridges(catalogs = catalogsOf(index = calledIndex, count = 1))

            CompileCase.OneFullCatalog -> compileBridges(catalogs = catalogsOf(index = fullIndex, count = 1))
            CompileCase.TenFullCatalogs ->
                compileBridges(catalogs = catalogsOf(index = fullIndex, count = HELD_CATALOGS))

            CompileCase.ManyCallsWithoutPlugin -> compileWithComposeOnly(source = manyCallsSource())
            CompileCase.ManyCallsWithCheckMode -> CompileTestSupport().compileSource(
                fileName = SOURCE_FILE,
                source = manyCallsSource(),
                options = ProducerTestOptions.checkOptions(),
            )
        }
        return result.requireSuccess()
    }

    /** Compiles against a catalog where one component the screen does not call has one more parameter. */
    fun compileWithOneUncalledComponentChanged(): JvmCompilationResult =
        compileBridges(catalogs = catalogsOf(index = changedIndex, count = 1)).requireSuccess()

    /** The screen that calls the bridges of the first held catalog. */
    val callerSource: String get() = bridgeCallerSource

    /** The same screen with hand-written stand-ins for the bridges. */
    val standInSource: String get() = ScaleSources.standInCaller(components = calledComponents)

    /** The name of every component of the catalog. */
    val componentNames: Set<String> get() = allComponents.mapTo(destination = HashSet()) { it.name }

    /** The options of [count] held catalogs of every component. */
    fun fullCatalogs(count: Int): List<CatalogOptionValue> = catalogsOf(index = fullIndex, count = count)

    /** How many components of the catalog have a function in the first bridge package of [result]. */
    fun createdBridgeCount(result: JvmCompilationResult): Int {
        val bridgePackage = packageOf(catalog = 0)
        val names = allComponents.mapTo(destination = HashSet()) { it.name }
        return result.outputDirectory.resolve(bridgePackage.replace('.', '/'))
            .listFiles { file -> file.name.endsWith(FACADE_SUFFIX) }
            .orEmpty()
            .flatMap { file ->
                result.classLoader.loadClass("$bridgePackage.${file.nameWithoutExtension}").declaredMethods.toList()
            }
            .map { it.name }
            .filter { it in names }
            .toSet()
            .size
    }

    fun callerClasses(result: JvmCompilationResult): Map<String, ByteArray> =
        classesUnder(result = result, folder = CALLER_FOLDER)

    fun bridgeClasses(result: JvmCompilationResult): Map<String, ByteArray> =
        classesUnder(result = result, folder = BRIDGE_FOLDER)

    private fun classesUnder(result: JvmCompilationResult, folder: String): Map<String, ByteArray> {
        val root = result.outputDirectory.resolve(folder)
        return root.walkTopDown()
            .filter { it.isFile && it.extension == CLASS_EXTENSION }
            .associate { it.relativeTo(root).invariantSeparatorsPath to it.readBytes() }
    }

    private fun compileBridges(catalogs: List<CatalogOptionValue>): JvmCompilationResult =
        CompileTestSupport().compileSource(
            fileName = SOURCE_FILE,
            source = bridgeCallerSource,
            options = ProducerTestOptions.producerOptions(*catalogs.toTypedArray()),
        )

    private fun compileWithComposeOnly(source: String): JvmCompilationResult =
        CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))
            .compileSource(fileName = SOURCE_FILE, source = source)

    private fun catalogsOf(index: HeldComponents, count: Int): List<CatalogOptionValue> =
        List(size = count) { catalog ->
            val version = "$VERSION_PREFIX$catalog"
            CatalogOptionValue(
                version = version,
                indexPath = indexFileOf(held = index, version = version).absolutePath,
                bridgePackage = packageOf(catalog = catalog),
            )
        }

    private fun indexFileOf(held: HeldComponents, version: String): File =
        writtenIndexes.getOrPut(key = "${held.name}-$version") {
            workDir.resolve("${held.name}-$version.index").apply {
                val components = held.components.toTypedArray()
                val catalog = TestCatalogs.catalogOf(version = version, components = components)
                writeBytes(array = CatalogIndexWriter.write(catalog = catalog))
            }
        }

    private class HeldComponents(val name: String, components: () -> List<ComponentDescriptor>) {
        val components: List<ComponentDescriptor> by lazy(components)
    }

    private fun withOneUncalledChanged(): List<ComponentDescriptor> {
        val changed = allComponents.last()
        val added = Parameter.Value(
            name = ADDED_PARAMETER,
            kind = ValueKind.Text,
            required = false,
            nullable = true,
        )
        return allComponents.dropLast(n = 1) + changed.copy(parameters = changed.parameters + added)
    }

    private fun manyCallsSource(): String =
        ScaleSources.manyCalls(functions = callingFunctions, calls = CALLS_PER_FUNCTION)

    private fun JvmCompilationResult.requireSuccess(): JvmCompilationResult {
        check(exitCode == ExitCode.OK) { messages }
        return this
    }

    private fun packageOf(catalog: Int): String = "scale.bridge.$VERSION_PREFIX$catalog"

    private companion object {
        const val SEED = 20_000L
        const val HELD_CATALOGS = 10
        const val CALLS_PER_FUNCTION = 25
        const val VERSION_PREFIX = "v"
        const val SOURCE_FILE = "Screen.kt"
        const val FACADE_SUFFIX = "Kt.class"
        const val CALLER_FOLDER = "scale/caller"
        const val BRIDGE_FOLDER = "scale/bridge"
        const val CLASS_EXTENSION = "class"
        const val ADDED_PARAMETER = "addedLater"
    }
}
