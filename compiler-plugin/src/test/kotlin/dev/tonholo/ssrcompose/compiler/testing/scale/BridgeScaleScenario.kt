package dev.tonholo.ssrcompose.compiler.testing.scale

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.testing.ParameterPalette
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalog
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalogShape
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions
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
    private val fullIndex by lazy { writeIndex(name = "full", components = allComponents) }
    private val calledIndex by lazy { writeIndex(name = "called", components = calledComponents) }
    private val changedIndex by lazy { writeIndex(name = "changed", components = withOneUncalledChanged()) }

    fun compile(case: CompileCase): JvmCompilationResult {
        val result = when (case) {
            CompileCase.StandIns ->
                compileWithComposeOnly(source = ScaleSources.standInCaller(components = calledComponents))

            CompileCase.CalledComponentsCatalog ->
                compileBridges(catalogs = catalogsOf(index = calledIndex, count = 1))

            CompileCase.OneFullCatalog -> compileSpread(catalogs = catalogsOf(index = fullIndex, count = 1))
            CompileCase.TenFullCatalogs ->
                compileSpread(catalogs = catalogsOf(index = fullIndex, count = HELD_CATALOGS))

            CompileCase.OneFullCatalogNamedOnly ->
                compileNamedOnly(catalogs = catalogsOf(index = fullIndex, count = 1))

            CompileCase.TenFullCatalogsNamedOnly ->
                compileNamedOnly(catalogs = catalogsOf(index = fullIndex, count = HELD_CATALOGS))

            CompileCase.ManyCallsWithoutPlugin -> compileWithComposeOnly(source = manyCallsSource())
            CompileCase.ManyCallsWithCheckMode -> CompileTestSupport().compileSource(
                fileName = SOURCE_FILE,
                source = manyCallsSource(),
                options = ProducerTestOptions.checkOptions(),
            )
        }
        return result.requireSuccess()
    }

    /** Compiles one full catalog with the plugin as it is, which puts every bridge function in one class. */
    fun compileFullCatalogInOneClass(): JvmCompilationResult =
        compileBridges(catalogs = catalogsOf(index = fullIndex, count = 1))

    /** Compiles against a catalog where one component the screen does not call has one more parameter. */
    fun compileWithOneUncalledComponentChanged(): JvmCompilationResult =
        compileBridges(catalogs = catalogsOf(index = changedIndex, count = 1)).requireSuccess()

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

    private fun compileSpread(catalogs: List<CatalogOptionValue>): JvmCompilationResult =
        compileExperiment(catalogs = catalogs, creation = BridgeCreation(namedIn = null, files = BRIDGE_FILES))

    private fun compileNamedOnly(catalogs: List<CatalogOptionValue>): JvmCompilationResult =
        compileExperiment(catalogs = catalogs, creation = BridgeCreation(namedIn = bridgeCallerSource, files = 1))

    private fun compileExperiment(
        catalogs: List<CatalogOptionValue>,
        creation: BridgeCreation,
    ): JvmCompilationResult =
        CompileTestSupport(
            registrars = listOf(
                ExperimentBridgeRegistrar(catalogs = catalogs, creation = creation),
                ComposePluginRegistrar(),
            ),
        ).compileSource(fileName = SOURCE_FILE, source = bridgeCallerSource)

    private fun compileWithComposeOnly(source: String): JvmCompilationResult =
        CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))
            .compileSource(fileName = SOURCE_FILE, source = source)

    private fun catalogsOf(index: File, count: Int): List<CatalogOptionValue> =
        List(size = count) { catalog ->
            CatalogOptionValue(
                version = "$VERSION_PREFIX$catalog",
                indexPath = index.absolutePath,
                bridgePackage = packageOf(catalog = catalog),
            )
        }

    private fun writeIndex(name: String, components: List<ComponentDescriptor>): File =
        workDir.resolve("$name.index").apply { writeText(text = ThrowawayIndexText.of(components = components)) }

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
        const val BRIDGE_FILES = 64
        const val FACADE_SUFFIX = "Kt.class"
        const val CALLER_FOLDER = "scale/caller"
        const val BRIDGE_FOLDER = "scale/bridge"
        const val CLASS_EXTENSION = "class"
        const val ADDED_PARAMETER = "addedLater"
    }
}
