package dev.tonholo.ssrcompose.compiler.testing

import androidx.compose.compiler.plugins.kotlin.ComposeCommandLineProcessor
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
import org.jetbrains.kotlin.config.JvmTarget

internal class CompileTestSupport(
    private val registrars: List<CompilerPluginRegistrar> = listOf(
        SsrCompilerPluginRegistrar(),
        ComposePluginRegistrar(),
    ),
) {
    private val runsSsrPlugin = registrars.any { it is SsrCompilerPluginRegistrar }

    /**
     * Compiles the fixture as a source file named [fileName], which is the fixture's name unless a test needs
     * two fixtures to be the same file in two compilations. The fixture can read the internals of [friendPaths].
     */
    fun compile(
        fixtureName: String,
        fileName: String = fixtureName,
        options: List<Pair<String, String>> = emptyList(),
        classpath: List<File> = emptyList(),
        friendPaths: List<File> = emptyList(),
    ): JvmCompilationResult =
        compileSources(
            sources = listOf(sourceFile(fixtureName = fixtureName, fileName = fileName)),
            options = options,
            classpath = classpath,
            friendPaths = friendPaths,
        )

    /** Compiles several fixtures together in one compilation. */
    fun compile(
        fixtures: List<String>,
        options: List<Pair<String, String>> = emptyList(),
        classpath: List<File> = emptyList(),
        fileNames: Map<String, String> = emptyMap(),
    ): JvmCompilationResult =
        compileSources(
            sources = fixtures.map { fixtureName ->
                sourceFile(fixtureName = fixtureName, fileName = fileNames[fixtureName] ?: fixtureName)
            },
            options = options,
            classpath = classpath,
        )

    /**
     * Compiles inline [source] under [fileName]. [composeOptions] go to the Compose compiler plugin.
     */
    fun compileSource(
        fileName: String,
        source: String,
        options: List<Pair<String, String>> = emptyList(),
        classpath: List<File> = emptyList(),
        composeOptions: List<Pair<String, String>> = emptyList(),
        friendPaths: List<File> = emptyList(),
        moduleName: String? = null,
    ): JvmCompilationResult =
        compileSources(
            sources = listOf(SourceFile.kotlin(name = fileName, contents = source)),
            options = options,
            classpath = classpath,
            friendPaths = friendPaths,
            composeOptions = composeOptions,
            moduleName = moduleName,
        )

    /** Compiles the Kotlin [files] from disk in one compilation. */
    fun compileFiles(files: List<File>, options: List<Pair<String, String>> = emptyList()): JvmCompilationResult =
        compileSources(
            sources = files.map { file -> SourceFile.fromPath(path = file) },
            options = options,
            classpath = emptyList(),
        )

    private fun sourceFile(fixtureName: String, fileName: String): SourceFile {
        val name = if (fileName.endsWith(KT_EXTENSION)) fileName else "$fileName$FIXTURE_SUFFIX"
        return SourceFile.kotlin(name = name, contents = readFixture(fixtureName = fixtureName))
    }

    private fun compileSources(
        sources: List<SourceFile>,
        options: List<Pair<String, String>>,
        classpath: List<File>,
        friendPaths: List<File> = emptyList(),
        composeOptions: List<Pair<String, String>> = emptyList(),
        moduleName: String? = null,
    ): JvmCompilationResult =
        KotlinCompilation().apply {
            val composeProcessor = ComposeCommandLineProcessor()
            this.moduleName = moduleName
            jvmTarget = JvmTarget.JVM_17.description
            classpaths = classpath
            this.friendPaths = friendPaths
            this.sources = sources
            compilerPluginRegistrars = registrars
            commandLineProcessors = listOfNotNull(SsrCommandLineProcessor().takeIf { runsSsrPlugin }, composeProcessor)
            pluginOptions = options.map { (name, value) ->
                PluginOption(
                    pluginId = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID,
                    optionName = name,
                    optionValue = value,
                )
            } + composeOptions.map { (name, value) ->
                PluginOption(pluginId = composeProcessor.pluginId, optionName = name, optionValue = value)
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
        const val KT_EXTENSION = ".kt"
    }
}
