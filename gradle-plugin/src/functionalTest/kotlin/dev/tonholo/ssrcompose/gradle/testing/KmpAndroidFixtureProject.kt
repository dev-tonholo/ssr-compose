package dev.tonholo.ssrcompose.gradle.testing

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

/**
 * A Kotlin Multiplatform module with an Android target and two product flavors, `free` and `paid`. AGP 9 refuses
 * flavors next to the Kotlin Multiplatform plugin unless its built-in Kotlin and new DSL are both turned off.
 */
internal class KmpAndroidFixtureProject(private val directory: File) {
    init {
        write(relativePath = "settings.gradle.kts", text = FixtureSettings.script())
        write(relativePath = "build.gradle.kts", text = BUILD_SCRIPT)
        write(relativePath = "gradle.properties", text = GRADLE_PROPERTIES)
    }

    fun addFile(relativePath: String, text: String): KmpAndroidFixtureProject {
        write(relativePath = relativePath, text = text)
        return this
    }

    fun file(relativePath: String): File = File(directory, relativePath)

    fun compile(task: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(directory)
            .withArguments(task, "--stacktrace")
            .build()

    private fun write(relativePath: String, text: String) {
        val file = file(relativePath = relativePath)
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    private companion object {
        // language=properties
        val GRADLE_PROPERTIES = """
            android.builtInKotlin=false
            android.newDsl=false
            android.useAndroidX=true
            org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g
        """.trimIndent()

        // language=kotlin
        val BUILD_SCRIPT = """
            plugins {
                alias(libs.plugins.kotlin.multiplatform)
                alias(libs.plugins.android.library)
                alias(libs.plugins.kotlin.compose)
                alias(libs.plugins.ssrcompose)
            }

            android {
                namespace = "fixture.kmp"
                compileSdk = libs.versions.compile.sdk.get().toInt()
                flavorDimensions += "tier"
                productFlavors {
                    create("free") { dimension = "tier" }
                    create("paid") { dimension = "tier" }
                }
            }

            kotlin {
                androidTarget()
                sourceSets.commonMain.dependencies {
                    implementation(libs.compose.runtime)
                }
            }

            ssrCompose {
                consumer { catalogVersion = "1" }
            }
        """.trimIndent()
    }
}
