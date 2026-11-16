package dev.tonholo.ssrcompose.compiler.testing

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.compiler.SsrCommandLineProcessor
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

internal class CompileTestSupport(
    private val registrars: List<CompilerPluginRegistrar> = listOf(
        SsrCompilerPluginRegistrar(),
        ComposePluginRegistrar(),
    ),
) {
    fun compile(
        fixtureName: String,
        options: List<Pair<String, String>> = emptyList(),
    ): JvmCompilationResult =
        KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin(name = "$fixtureName$FIXTURE_SUFFIX", contents = readFixture(fixtureName)),
            )
            compilerPluginRegistrars = registrars
            commandLineProcessors = listOf(SsrCommandLineProcessor())
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
