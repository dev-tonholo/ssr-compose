package dev.tonholo.ssrcompose.gradle.testing

import org.gradle.testkit.runner.BuildResult
import java.io.File

/**
 * A build of two modules: `:bridge` holds one catalog and creates its bridge, and `:screens` calls `Alpha` from
 * `AlphaScreen.kt` and `Beta` from `BetaScreen.kt`. The catalog also holds `Unused`, which nobody calls.
 */
internal class BridgeModulesFixture(private val directory: File) {
    private val project = FixtureProject(directory = directory)

    init {
        project
            .addFile(relativePath = "settings.gradle.kts", text = FixtureSettings.script() + SETTINGS_MODULES)
            .addFile(relativePath = "build.gradle.kts", text = ROOT_BUILD)
            .addFile(relativePath = "bridge/build.gradle.kts", text = BRIDGE_BUILD)
            .addFile(relativePath = "bridge/src/main/kotlin/app/anchor/Anchor.kt", text = ANCHOR_SOURCE)
            .addFile(relativePath = INDEX_PATH, text = indexText(changedComponent = null))
            .addFile(relativePath = "screens/build.gradle.kts", text = SCREENS_BUILD)
            .addFile(relativePath = "$SCREENS_SOURCES/AlphaScreen.kt", text = screenSource(component = "Alpha"))
            .addFile(relativePath = "$SCREENS_SOURCES/BetaScreen.kt", text = screenSource(component = "Beta"))
    }

    fun compile(): BuildResult = project.run(task = COMPILE_SCREENS_TASK, "--info")

    /** Gives [component] one more optional parameter in the index of the catalog. */
    fun changeComponent(component: String) {
        project.addFile(relativePath = INDEX_PATH, text = indexText(changedComponent = component))
    }

    /** The time each compiled class of `:screens` was last written, by file name. */
    fun screenClassTimes(): Map<String, Long> =
        directory.resolve(SCREENS_CLASSES).walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .associate { it.name to it.lastModified() }

    private fun indexText(changedComponent: String?): String =
        COMPONENTS.joinToString(separator = "\n", postfix = "\n") { component ->
            val extra = if (component == changedComponent) "\nparam extra text optional" else ""
            "component $component\nparam title text required$extra"
        }

    private fun screenSource(component: String): String =
        FixtureSources.bridgeScreenSource(component = component, title = "text")

    private companion object {
        const val COMPILE_SCREENS_TASK = ":screens:compileKotlin"
        const val INDEX_PATH = "bridge/build/ssr/index/2.index"
        const val SCREENS_SOURCES = "screens/src/main/kotlin/app/screens"
        const val SCREENS_CLASSES = "screens/build/classes/kotlin/main/app/screens"
        val COMPONENTS = listOf("Alpha", "Beta", "Unused")

        // language=kotlin
        const val SETTINGS_MODULES = """

include(":bridge", ":screens")
"""

        // language=kotlin
        const val ROOT_BUILD = """
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ssrcompose) apply false
}
"""

        // language=kotlin
        const val BRIDGE_BUILD = """
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ssrcompose)
}

dependencies {
    api("dev.tonholo.ssrcompose:producer")
}

ssrCompose {
    producer {
        catalog(version = "2") {
            location = "https://example.com/app/catalogs/2/manifest.json"
            bridgePackage = "app.bridge.v2"
        }
    }
}
"""

        // language=kotlin
        const val SCREENS_BUILD = """
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":bridge"))
}
"""

        // language=kotlin
        const val ANCHOR_SOURCE = """
package app.anchor

internal const val ANCHOR: Int = 1
"""
    }
}
