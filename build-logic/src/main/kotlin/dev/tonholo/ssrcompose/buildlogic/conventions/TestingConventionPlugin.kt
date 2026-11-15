package dev.tonholo.ssrcompose.buildlogic.conventions

import dev.tonholo.ssrcompose.buildlogic.ANDROID_HOST_TEST_SOURCE_SET
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertGradleExtension

private const val TEST_IMPLEMENTATION_CONFIGURATION = "testImplementation"

private val POWER_ASSERT_FUNCTIONS = listOf(
    "kotlin.assert",
    "kotlin.test.assertEquals",
    "kotlin.test.assertTrue",
    "kotlin.test.assertFalse",
    "kotlin.test.assertNotNull",
)

/**
 * Sets up testing: power-assert, and the kotlin.test, coroutines test and Turbine dependencies for Kotlin JVM and
 * Kotlin Multiplatform modules. Android host tests also get `kotlin-test-junit`.
 */
internal abstract class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.kotlin.power.assert.get().pluginId)
        configurePowerAssert()

        pluginManager.withPlugin(libs.plugins.kotlin.jvm.get().pluginId) { configureJvmTests() }
        pluginManager.withPlugin(libs.plugins.kotlin.multiplatform.get().pluginId) { configureMultiplatformTests() }
    }
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun Project.configurePowerAssert() {
    extensions.configure<PowerAssertGradleExtension> {
        functions.set(POWER_ASSERT_FUNCTIONS)
    }
}

private fun Project.configureJvmTests() {
    dependencies {
        add(TEST_IMPLEMENTATION_CONFIGURATION, libs.kotlin.test)
        add(TEST_IMPLEMENTATION_CONFIGURATION, libs.kotlinx.coroutines.test)
        add(TEST_IMPLEMENTATION_CONFIGURATION, libs.turbine)
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

private fun Project.configureMultiplatformTests() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        sourceSets.matching { it.name == ANDROID_HOST_TEST_SOURCE_SET }.configureEach {
            dependencies {
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}
