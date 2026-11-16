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

val worstShapeTests = "*WorstShapeMemoryTest"
val worstManifestHeap = "144m"
val worstPartHeap = "320m"
val worstWriterHeap = "72m"

tasks.test {
    maxHeapSize = "256m"
    filter { excludeTestsMatching(worstShapeTests) }
}

fun registerMemoryTest(taskName: String, taskDescription: String, testClass: String, heap: String) =
    tasks.register<Test>(taskName) {
        description = taskDescription
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        maxHeapSize = heap
        forkEvery = 1
        filter { includeTestsMatching(testClass) }
    }

val worstManifestMemoryTest = registerMemoryTest(
    taskName = "worstManifestMemoryTest",
    taskDescription = "Reads the worst 16 MiB manifest under a $worstManifestHeap heap.",
    testClass = "*CatalogManifestWorstShapeMemoryTest",
    heap = worstManifestHeap,
)

val worstPartMemoryTest = registerMemoryTest(
    taskName = "worstPartMemoryTest",
    taskDescription = "Reads the worst 16 MiB part under a $worstPartHeap heap.",
    testClass = "*CatalogPartWorstShapeMemoryTest",
    heap = worstPartHeap,
)

val worstWriterMemoryTest = registerMemoryTest(
    taskName = "worstWriterMemoryTest",
    taskDescription = "Writes the largest 16 MiB part under a $worstWriterHeap heap.",
    testClass = "*CatalogPartWriterWorstShapeMemoryTest",
    heap = worstWriterHeap,
)

tasks.check {
    dependsOn(worstManifestMemoryTest, worstPartMemoryTest, worstWriterMemoryTest)
}

buildConfig {
    packageName("dev.tonholo.ssrcompose.catalog.options")
    className("CompilerPluginBuildConfig")
    useKotlinOutput { internalVisibility = false }
    buildConfigField("COMPILER_PLUGIN_ID", providers.gradleProperty("ssrcompose.compiler.plugin.id"))
}
