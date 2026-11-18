package dev.tonholo.ssrcompose.gradle.testing

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import org.gradle.testkit.runner.BuildResult
import java.io.File

/**
 * One module compiled with the named-only bridge prototype: `AlphaScreen.kt` calls `Alpha`, `BetaScreen.kt` calls
 * `Beta`, `Plain.kt` calls no bridge, and the prototype declares a bridge only for the names in the files the compiler
 * is given.
 */
internal class NamedOnlyProbeFixture(private val directory: File) {
    private val project = FixtureProject(directory = directory)

    init {
        val probeJar = requireNotNull(System.getProperty(PROBE_JAR_PROPERTY)) {
            "The $PROBE_JAR_PROPERTY system property is not set"
        }
        val catalog = CatalogOptionValue(
            version = FixtureSources.DEFAULT_VERSION,
            indexPath = directory.resolve(INDEX_PATH).invariantSeparatorsPath,
            bridgePackage = FixtureSources.DEFAULT_BRIDGE_PACKAGE,
        )
        val buildScript = BUILD_TEMPLATE
            .replace(oldValue = PROBE_JAR_PLACEHOLDER, newValue = File(probeJar).invariantSeparatorsPath)
            .replace(oldValue = CATALOG_PLACEHOLDER, newValue = catalog.encode())
        project
            .addFile(relativePath = "build.gradle.kts", text = buildScript)
            .addFile(relativePath = INDEX_PATH, bytes = FixtureIndexes.withComponents(names = listOf("Alpha", "Beta")))
            .addFile(relativePath = ALPHA_SCREEN_PATH, text = screenSource(component = "Alpha", title = "first"))
            .addFile(relativePath = BETA_SCREEN_PATH, text = screenSource(component = "Beta", title = "first"))
            .addFile(relativePath = PLAIN_SOURCE_PATH, text = PLAIN_SOURCE)
    }

    fun compile(): BuildResult = project.run(task = COMPILE_TASK, "--info")

    /** The time the compiled `BetaScreen` class was last written. */
    fun betaScreenClassTime(): Long = directory.resolve(BETA_SCREEN_CLASS).lastModified()

    /** The time the compiled class of `Plain.kt`, which calls no bridge, was last written. */
    fun plainClassTime(): Long = directory.resolve(PLAIN_CLASS).lastModified()

    /** Edits only `AlphaScreen.kt`, so an incremental compile is given that one file. */
    fun editAlphaScreen() {
        project.addFile(relativePath = ALPHA_SCREEN_PATH, text = screenSource(component = "Alpha", title = "second"))
    }

    /** True when a class in the bridge package declares the bridge function of [component]. */
    fun bridgeClassesDeclare(component: String): Boolean =
        directory.resolve(BRIDGE_CLASSES).walkTopDown()
            .filter { it.isFile && it.extension == CLASS_EXTENSION }
            .any { file -> disassembled(classFile = file).contains(" void $component(") }

    /** True when the bytecode of `BetaScreen` calls the bridge function of [component] in the bridge class. */
    fun betaScreenCallsBridge(component: String): Boolean =
        disassembled(classFile = directory.resolve(BETA_SCREEN_CLASS)).lineSequence().any { line ->
            line.contains("invokestatic") && line.contains("Method $BRIDGE_CLASS_NAME.$component:")
        }

    private fun disassembled(classFile: File): String {
        val javap = File(System.getProperty(JAVA_HOME_PROPERTY), JAVAP_PATH).path
        val process = ProcessBuilder(javap, "-c", "-p", classFile.path).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output
    }

    private fun screenSource(component: String, title: String): String =
        FixtureSources.bridgeScreenSource(component = component, title = title)

    private companion object {
        const val PROBE_JAR_PROPERTY = "ssrcompose.namedOnlyProbeJar"
        const val PROBE_JAR_PLACEHOLDER = "PROBE_JAR"
        const val CATALOG_PLACEHOLDER = "CATALOG_VALUE"
        const val COMPILE_TASK = "compileKotlin"
        const val INDEX_PATH = "catalog/2.index"
        const val ALPHA_SCREEN_PATH = "src/main/kotlin/app/screens/AlphaScreen.kt"
        const val BETA_SCREEN_PATH = "src/main/kotlin/app/screens/BetaScreen.kt"
        const val PLAIN_SOURCE_PATH = "src/main/kotlin/app/screens/Plain.kt"
        const val BRIDGE_CLASSES = "build/classes/kotlin/main/app/bridge/v2"
        const val BRIDGE_CLASS_NAME = "app/bridge/v2/Bridge0Kt"
        const val BETA_SCREEN_CLASS = "build/classes/kotlin/main/app/screens/BetaScreenKt.class"
        const val PLAIN_CLASS = "build/classes/kotlin/main/app/screens/PlainKt.class"
        const val CLASS_EXTENSION = "class"
        const val JAVA_HOME_PROPERTY = "java.home"
        const val JAVAP_PATH = "bin/javap"

        // language=kotlin
        const val PLAIN_SOURCE = "package app.screens\n\nfun plain(): Int = 1\n"

        // language=kotlin
        const val BUILD_TEMPLATE = """
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation("dev.tonholo.ssrcompose:producer")
    kotlinCompilerPluginClasspathMain(files("PROBE_JAR"))
    kotlinCompilerPluginClasspathMain("dev.tonholo.ssrcompose:catalog-model")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:dev.tonholo.ssrcompose.scale.source-scan:catalog=CATALOG_VALUE",
            "-Xcompiler-plugin-order=dev.tonholo.ssrcompose.scale.source-scan>androidx.compose.compiler.plugins.kotlin",
        )
    }
}
"""
    }
}
