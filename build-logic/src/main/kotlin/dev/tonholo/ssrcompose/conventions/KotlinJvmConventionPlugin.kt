package dev.tonholo.ssrcompose.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Sets up a Kotlin JVM module: the Kotlin JVM plugin, the testing and detekt conventions, and the JVM target from the
 * version catalog for both Kotlin and Java.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.kotlin.jvm.get().pluginId)
        pluginManager.apply(TestingConventionPlugin::class.java)
        pluginManager.apply(DetektConventionPlugin::class.java)

        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(libs.jvmTarget)
            }
        }
        tasks.withType<JavaCompile>().configureEach {
            options.release.set(libs.javaRelease)
        }
    }
}
