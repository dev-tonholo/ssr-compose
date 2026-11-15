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
        register("conventionCompose") {
            id = "dev.tonholo.ssrcompose.conventions.compose"
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.ComposeConventionPlugin"
        }
        register("conventionSerialization") {
            id = "dev.tonholo.ssrcompose.conventions.serialization"
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.SerializationConventionPlugin"
        }
        register("conventionPublishing") {
            id = "dev.tonholo.ssrcompose.conventions.publishing"
            implementationClass = "dev.tonholo.ssrcompose.buildlogic.conventions.PublishingConventionPlugin"
        }
    }
}
