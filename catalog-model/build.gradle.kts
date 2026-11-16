plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    alias(libs.plugins.buildconfig)
}

dependencies {
    api(projects.core)
    implementation(libs.kotlinx.serialization.json)
}

buildConfig {
    packageName("dev.tonholo.ssrcompose.catalog.options")
    className("CompilerPluginBuildConfig")
    useKotlinOutput { internalVisibility = false }
    buildConfigField("COMPILER_PLUGIN_ID", providers.gradleProperty("ssrcompose.compiler.plugin.id"))
}
