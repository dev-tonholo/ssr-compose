import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

fun DependencyHandler.plugin(plugin: Provider<PluginDependency>): Provider<String> =
    plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation(plugin(libs.plugins.kotlin.jvm))
    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.kotlin.compose))
    implementation(plugin(libs.plugins.kotlin.serialization))
    implementation(plugin(libs.plugins.kotlin.power.assert))
    implementation(plugin(libs.plugins.android.kmp.library))
    implementation(plugin(libs.plugins.detekt))
}

gradlePlugin {
    plugins {
        register("conventionKotlinJvm") {
            id = libs.plugins.ssrcompose.conventions.kotlin.jvm.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.KotlinJvmConventionPlugin"
        }
        register("conventionKmpLibrary") {
            id = libs.plugins.ssrcompose.conventions.kmp.library.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.KmpLibraryConventionPlugin"
        }
        register("conventionCompose") {
            id = libs.plugins.ssrcompose.conventions.compose.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.ComposeConventionPlugin"
        }
        register("conventionSerialization") {
            id = libs.plugins.ssrcompose.conventions.serialization.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.SerializationConventionPlugin"
        }
        register("conventionTesting") {
            id = libs.plugins.ssrcompose.conventions.testing.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.TestingConventionPlugin"
        }
        register("conventionDetekt") {
            id = libs.plugins.ssrcompose.conventions.detekt.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.DetektConventionPlugin"
        }
        register("conventionPublishing") {
            id = libs.plugins.ssrcompose.conventions.publishing.get().pluginId
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.PublishingConventionPlugin"
        }
    }
}
