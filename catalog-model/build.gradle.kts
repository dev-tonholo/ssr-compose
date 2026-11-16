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

val worstShapeTests = "*CatalogFileWorstShapeMemoryTest"

tasks.test {
    maxHeapSize = "256m"
    filter { excludeTestsMatching(worstShapeTests) }
}

val worstShapeMemoryTest = tasks.register<Test>("worstShapeMemoryTest") {
    description = "Reads the worst 16 MiB catalog under a 512 MiB heap."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    maxHeapSize = "512m"
    filter { includeTestsMatching(worstShapeTests) }
}

tasks.check {
    dependsOn(worstShapeMemoryTest)
}

buildConfig {
    packageName("dev.tonholo.ssrcompose.catalog.options")
    className("CompilerPluginBuildConfig")
    useKotlinOutput { internalVisibility = false }
    buildConfigField("COMPILER_PLUGIN_ID", providers.gradleProperty("ssrcompose.compiler.plugin.id"))
}
