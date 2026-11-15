import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

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
            id = "dev.tonholo.ssrcompose.conventions.kotlin-jvm"
            implementationClass = "dev.tonholo.ssrcompose.conventions.KotlinJvmConventionPlugin"
        }
        register("conventionKmpLibrary") {
            id = "dev.tonholo.ssrcompose.conventions.kmp-library"
            implementationClass = "dev.tonholo.ssrcompose.conventions.KmpLibraryConventionPlugin"
        }
        register("conventionCompose") {
            id = "dev.tonholo.ssrcompose.conventions.compose"
            implementationClass = "dev.tonholo.ssrcompose.conventions.ComposeConventionPlugin"
        }
        register("conventionSerialization") {
            id = "dev.tonholo.ssrcompose.conventions.serialization"
            implementationClass = "dev.tonholo.ssrcompose.conventions.SerializationConventionPlugin"
        }
        register("conventionTesting") {
            id = "dev.tonholo.ssrcompose.conventions.testing"
            implementationClass = "dev.tonholo.ssrcompose.conventions.TestingConventionPlugin"
        }
        register("conventionDetekt") {
            id = "dev.tonholo.ssrcompose.conventions.detekt"
            implementationClass = "dev.tonholo.ssrcompose.conventions.DetektConventionPlugin"
        }
        register("conventionPublishing") {
            id = "dev.tonholo.ssrcompose.conventions.publishing"
            implementationClass = "dev.tonholo.ssrcompose.conventions.PublishingConventionPlugin"
        }
    }
}

private fun plugin(provider: Provider<PluginDependency>): Provider<String> = provider.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}"
}
