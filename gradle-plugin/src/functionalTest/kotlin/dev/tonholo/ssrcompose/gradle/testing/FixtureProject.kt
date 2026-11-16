package dev.tonholo.ssrcompose.gradle.testing

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

internal class FixtureProject(
    private val directory: File,
    private val buildCacheDirectory: File? = null,
) {
    init {
        val settings = FixtureSettings.script(buildCacheDirectory = buildCacheDirectory)
        write(relativePath = "settings.gradle.kts", text = settings)
    }

    fun configure(
        ssrComposeBlock: String,
        extraPlugins: List<String> = emptyList(),
        extraBuildScript: String = "",
    ): FixtureProject {
        write(
            relativePath = "build.gradle.kts",
            text = buildScript(
                ssrComposeBlock = ssrComposeBlock,
                extraPlugins = extraPlugins,
                extraBuildScript = extraBuildScript,
            ),
        )
        return this
    }

    fun addFile(relativePath: String, text: String): FixtureProject {
        write(relativePath = relativePath, text = text)
        return this
    }

    fun readCompilerSetup(): CompilerSetup {
        val output = run(task = PRINT_TASK).output
        return CompilerSetup(
            pluginOptions = output.linesTagged(tag = PLUGIN_OPTION_TAG),
            freeCompilerArguments = output.linesTagged(tag = FREE_ARGUMENT_TAG),
            inputProperties = output.linesTagged(tag = INPUT_PROPERTY_TAG),
            pluginDependencies = output.linesTagged(tag = DEPENDENCY_TAG),
            implementationDependencies = output.linesTagged(tag = IMPLEMENTATION_TAG),
        )
    }

    fun compile(): BuildResult = run(task = COMPILE_TASK)

    fun run(task: String, vararg extraArgs: String): BuildResult =
        runner(task = task, extraArgs = extraArgs.toList()).build()

    fun runAndFail(task: String): BuildResult = runner(task = task).buildAndFail()

    private fun runner(task: String, extraArgs: List<String> = emptyList()): GradleRunner =
        GradleRunner.create()
            .withProjectDir(directory)
            .withArguments(
                listOfNotNull(
                    task,
                    "--stacktrace",
                    BUILD_CACHE_FLAG.takeIf { buildCacheDirectory != null },
                ) + extraArgs,
            )

    private fun write(relativePath: String, text: String) {
        val file = File(directory, relativePath)
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    private fun String.linesTagged(tag: String): List<String> =
        lines()
            .filter { line -> line.startsWith("$tag$SEPARATOR") }
            .map { line -> line.removePrefix("$tag$SEPARATOR") }

    private fun buildScript(
        ssrComposeBlock: String,
        extraPlugins: List<String>,
        extraBuildScript: String,
    ): String {
        val pluginLines = extraPlugins.joinToString(separator = "\n    ")
        return BUILD_SCRIPT_TEMPLATE
            .replace(oldValue = EXTRA_PLUGINS_PLACEHOLDER, newValue = pluginLines)
            .replace(oldValue = SSR_COMPOSE_BLOCK_PLACEHOLDER, newValue = ssrComposeBlock)
            .replace(oldValue = EXTRA_BUILD_SCRIPT_PLACEHOLDER, newValue = extraBuildScript)
    }

    private companion object {
        const val EXTRA_PLUGINS_PLACEHOLDER = "EXTRA_PLUGINS"
        const val SSR_COMPOSE_BLOCK_PLACEHOLDER = "SSR_COMPOSE_BLOCK"
        const val EXTRA_BUILD_SCRIPT_PLACEHOLDER = "EXTRA_BUILD_SCRIPT"
        const val PRINT_TASK = "printCompilerSetup"
        const val COMPILE_TASK = "compileKotlin"
        const val BUILD_CACHE_FLAG = "--build-cache"
        const val SEPARATOR = "|"
        const val PLUGIN_OPTION_TAG = "PLUGIN_OPTION"
        const val FREE_ARGUMENT_TAG = "FREE_ARGUMENT"
        const val INPUT_PROPERTY_TAG = "INPUT_PROPERTY"
        const val DEPENDENCY_TAG = "DEPENDENCY"
        const val IMPLEMENTATION_TAG = "IMPLEMENTATION"

        val BUILD_SCRIPT_TEMPLATE = """
            @file:OptIn(org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi::class)

            import org.gradle.api.artifacts.Dependency
            import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext
            import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

            plugins {
                alias(libs.plugins.kotlin.jvm)
                alias(libs.plugins.ssrcompose)
                $EXTRA_PLUGINS_PLACEHOLDER
            }

            ssrCompose {
                $SSR_COMPOSE_BLOCK_PLACEHOLDER
            }

            $EXTRA_BUILD_SCRIPT_PLACEHOLDER

            tasks.register("$PRINT_TASK") {
                val compile = tasks.named<KotlinCompile>("compileKotlin")
                val pluginClasspath = configurations.named("kotlinCompilerPluginClasspathMain")
                val implementation = configurations.named("implementation")
                doLast {
                    val task = compile.get()
                    val arguments = task.createCompilerArguments(CreateCompilerArgumentsContext.default)
                    arguments.pluginOptions.orEmpty().forEach { println("$PLUGIN_OPTION_TAG$SEPARATOR" + it) }
                    task.compilerOptions.freeCompilerArgs.get().forEach { println("$FREE_ARGUMENT_TAG$SEPARATOR" + it) }
                    task.inputs.properties.forEach { entry ->
                        println("$INPUT_PROPERTY_TAG$SEPARATOR" + entry.key + "=" + entry.value)
                    }
                    fun notation(dependency: Dependency) =
                        dependency.group + ":" + dependency.name + ":" + dependency.version
                    pluginClasspath.get().dependencies
                        .forEach { println("$DEPENDENCY_TAG$SEPARATOR" + notation(it)) }
                    implementation.get().dependencies
                        .forEach { println("$IMPLEMENTATION_TAG$SEPARATOR" + notation(it)) }
                }
            }
        """.trimIndent()
    }
}
