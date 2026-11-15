package dev.tonholo.ssrcompose.buildlogic.conventions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.tonholo.ssrcompose.buildlogic.PROJECT_GROUP
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Sets up a Kotlin Multiplatform library with an Android target: the Kotlin Multiplatform and Android Kotlin
 * Multiplatform library plugins, the testing and detekt conventions, the JVM target for every JVM target, and the
 * Android namespace, SDK versions and host tests. A module adds the targets it needs, such as `jvm()`.
 */
internal abstract class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.kotlin.multiplatform.get().pluginId)
        pluginManager.apply(libs.plugins.android.kmp.library.get().pluginId)
        pluginManager.apply(TestingConventionPlugin::class.java)
        pluginManager.apply(DetektConventionPlugin::class.java)

        val androidNamespace = "$PROJECT_GROUP.${name.replace(oldValue = "-", newValue = "")}"

        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinJvmTarget>().configureEach {
                compilerOptions.jvmTarget.set(libs.jvmTarget)
            }
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = androidNamespace
                compileSdk = libs.compileSdk
                minSdk = libs.minSdk
                compilerOptions.jvmTarget.set(libs.jvmTarget)
                withHostTest {}
            }
        }
    }
}
