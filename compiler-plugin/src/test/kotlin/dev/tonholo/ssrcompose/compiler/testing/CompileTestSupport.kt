package dev.tonholo.ssrcompose.compiler.testing

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.compiler.SsrCommandLineProcessor
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

internal class CompileTestSupport(
    private val registrars: List<CompilerPluginRegistrar> = listOf(
        SsrCompilerPluginRegistrar(),
        ComposePluginRegistrar(),
    ),
) {
    private val runsSsrPlugin = registrars.any { it is SsrCompilerPluginRegistrar }

    /**
     * Compiles the fixture as a source file named [fileName], which is the fixture's name unless a test needs
     * two fixtures to be the same file in two compilations.
     */
    fun compile(
        fixtureName: String,
        fileName: String = fixtureName,
        options: List<Pair<String, String>> = emptyList(),
        classpath: List<File> = emptyList(),
    ): JvmCompilationResult =
        compileSource(
            fileName = "$fileName$FIXTURE_SUFFIX",
            source = readFixture(fixtureName),
            options = options,
            classpath = classpath,
        )

    /**
     * Compiles inline [source] under [fileName].
     */
    fun compileSource(
        fileName: String,
        source: String,
        options: List<Pair<String, String>> = emptyList(),
        classpath: List<File> = emptyList(),
    ): JvmCompilationResult =
        KotlinCompilation().apply {
            classpaths = classpath
            sources = listOf(SourceFile.kotlin(name = fileName, contents = source))
            compilerPluginRegistrars = registrars
            commandLineProcessors = if (runsSsrPlugin) listOf(SsrCommandLineProcessor()) else emptyList()
            pluginOptions = options.map { (name, value) ->
                PluginOption(
                    pluginId = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID,
                    optionName = name,
                    optionValue = value,
                )
            }
            inheritClassPath = true
        }.compile()

    private fun readFixture(fixtureName: String): String {
        val resourcePath = "$FIXTURES_FOLDER/$fixtureName$FIXTURE_SUFFIX"
        val stream = checkNotNull(javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Fixture not found: $resourcePath"
        }
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val FIXTURES_FOLDER = "fixtures"
        const val FIXTURE_SUFFIX = ".fixture.kt"
    }
}
