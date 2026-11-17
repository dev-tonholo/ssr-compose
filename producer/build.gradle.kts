import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.compose)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

dependencies {
    api(projects.core)
    api(libs.compose.runtime)
    kotlinCompilerPluginClasspathTest(projects.compilerPlugin)
}

val bridgePluginId = providers.gradleProperty("ssrcompose.compiler.plugin.id")
val composePluginId = "androidx.compose.compiler.plugins.kotlin"
val throwawayIndexFolder = rootProject.layout.projectDirectory.dir("compiler-plugin/src/test/resources/throwaway-index")
val firstBridgeIndex = throwawayIndexFolder.file("first-catalog.throwaway-index")
val secondBridgeIndex = throwawayIndexFolder.file("second-catalog.throwaway-index")
val bridgePackagePrefix = "dev.tonholo.ssrcompose.producer.experiment"

tasks.named<KotlinCompile>("compileTestKotlin") {
    inputs.file(firstBridgeIndex).withPropertyName("firstBridgeIndex").withPathSensitivity(PathSensitivity.NONE)
    inputs.file(secondBridgeIndex).withPropertyName("secondBridgeIndex").withPathSensitivity(PathSensitivity.NONE)
    compilerOptions.freeCompilerArgs.addAll(
        bridgePluginId.map { id ->
            val firstCatalog = "1|${firstBridgeIndex.asFile.absolutePath}|$bridgePackagePrefix.first"
            val secondCatalog = "2|${secondBridgeIndex.asFile.absolutePath}|$bridgePackagePrefix.second"
            listOf(
                "-P", "plugin:$id:mode=producer",
                "-P", "plugin:$id:catalog=$firstCatalog",
                "-P", "plugin:$id:catalog=$secondCatalog",
                "-Xcompiler-plugin-order=$id>$composePluginId",
            )
        },
    )
}
