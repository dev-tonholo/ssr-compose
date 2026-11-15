@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import dev.tonholo.ssrcompose.buildlogic.ANDROID_HOST_TEST_SOURCE_SET
import dev.tonholo.ssrcompose.buildlogic.libs
import dev.tonholo.ssrcompose.buildlogic.library
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.plugin.power-assert")
}

powerAssert {
    functions.set(
        listOf(
            "kotlin.assert",
            "kotlin.test.assertEquals",
            "kotlin.test.assertTrue",
            "kotlin.test.assertFalse",
            "kotlin.test.assertNotNull",
        ),
    )
}

val testLibraries = listOf("kotlin-test", "kotlinx-coroutines-test", "turbine").map(libs::library)

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    dependencies {
        testLibraries.forEach { testLibrary -> add("testImplementation", testLibrary) }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets.commonTest.dependencies {
            testLibraries.forEach { testLibrary -> implementation(testLibrary) }
        }
        sourceSets.matching { it.name == ANDROID_HOST_TEST_SOURCE_SET }.configureEach {
            dependencies {
                implementation(libs.library(alias = "kotlin-test-junit"))
            }
        }
    }
}
