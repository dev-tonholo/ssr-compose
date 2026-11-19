package dev.tonholo.ssrcompose.compiler.testing.scale

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions
import java.io.File

/** A server module of [files] plain source files next to one screen that calls the bridges of [scenario]. */
internal class LargeModuleScenario(
    workDir: File,
    private val scenario: BridgeScaleScenario,
    files: Int,
) {
    /** The folder that holds every source file of the module except the screen. */
    val sourceFolder: File = workDir.resolve("large-module").apply { mkdirs() }
    private val screenFolder: File = workDir.resolve("large-module-screen").apply { mkdirs() }
    private val plainFiles: List<File> = List(size = files) { index ->
        sourceFolder.resolve("Plain$index.kt").apply {
            writeText(text = ScaleSources.plainFile(fileIndex = index, functions = FUNCTIONS_PER_FILE))
        }
    }

    fun compile(case: LargeModuleCase): JvmCompilationResult {
        val screen = screenFolder.resolve(SCREEN_FILE).apply { writeText(text = screenSource(case = case)) }
        val sources = if (case.isClean) plainFiles + screen else listOf(screen)
        val result = supportFor(case = case).compileFiles(files = sources, options = optionsFor(case = case))
        check(result.exitCode == ExitCode.OK) { result.messages }
        return result
    }

    /** Compiles every file with [heldCatalogs] full catalogs, and times the steps of the plugin on [clock]. */
    fun compileMeasured(heldCatalogs: Int, clock: PluginStepClock): JvmCompilationResult {
        val screen = screenFolder.resolve(SCREEN_FILE).apply { writeText(text = scenario.callerSource) }
        val registrar = MeasuredBridgeRegistrar(
            catalogs = scenario.fullCatalogs(count = heldCatalogs),
            sourceFolder = sourceFolder,
            clock = clock,
        )
        val result = CompileTestSupport(registrars = listOf(registrar, ComposePluginRegistrar()))
            .compileFiles(files = plainFiles + screen)
        check(result.exitCode == ExitCode.OK) { result.messages }
        return result
    }

    private fun screenSource(case: LargeModuleCase): String =
        if (case.heldCatalogs == 0) scenario.standInSource else scenario.callerSource

    private fun supportFor(case: LargeModuleCase): CompileTestSupport =
        if (case.heldCatalogs == 0) {
            CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))
        } else {
            CompileTestSupport()
        }

    private fun optionsFor(case: LargeModuleCase): List<Pair<String, String>> {
        if (case.heldCatalogs == 0) return emptyList()
        val catalogs = scenario.fullCatalogs(count = case.heldCatalogs).toTypedArray()
        return ProducerTestOptions.producerOptions(*catalogs) +
            ProducerTestOptions.sourceFolderOption(folder = sourceFolder)
    }

    private companion object {
        const val FUNCTIONS_PER_FILE = 40
        const val SCREEN_FILE = "Screen.kt"
    }
}
