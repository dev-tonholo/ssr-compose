import java.util.Properties

plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.serialization)
}

val layoutlibRuntime = configurations.create("layoutlibRuntime") {
    isCanBeConsumed = false
    isTransitive = false
}

val layoutlibHostsFile = layout.projectDirectory
    .dir("src/main/resources/dev/tonholo/ssrcompose/producer/text/layoutlib")
    .file("layoutlib-hosts.properties")

val hostSystem: Provider<String> =
    providers.systemProperty("os.name").zip(providers.systemProperty("os.arch")) { os, arch -> "$os/$arch" }

val hostLayoutlibRuntimeClassifier: Provider<String> =
    providers.fileContents(layoutlibHostsFile).asText.zip(hostSystem) { table, host ->
        val hosts = Properties().apply { load(table.reader()) }
        hosts.getProperty(host)
            ?: error("layoutlib has no runtime set up for $host, only for ${hosts.stringPropertyNames().sorted()}")
    }

dependencies {
    implementation(libs.layoutlib)
    implementation(libs.layoutlib.api)
    runtimeOnly(libs.layoutlib.resources)

    layoutlibRuntime.dependencies.addLater(
        libs.layoutlib.runtime.zip(hostLayoutlibRuntimeClassifier) { runtime, hostClassifier ->
            project.dependencies.create(runtime).also { dependency ->
                (dependency as ExternalModuleDependency).artifact { classifier = hostClassifier }
            }
        },
    )

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.roboto.fonts) { isTransitive = false }
    testImplementation(libs.open.sans.fonts) { isTransitive = false }
}

val extractLayoutlibRuntime = tasks.register<Sync>("extractLayoutlibRuntime") {
    from(zipTree(layoutlibRuntime.elements.map { it.single() }))
    into(layout.buildDirectory.dir("layoutlib-runtime"))
}

class LayoutlibRuntimeArgument(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val directory: Provider<File>,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf("-Dssrcompose.layoutlib.runtime=${directory.get().absolutePath}")
}

tasks.test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    jvmArgumentProviders.add(LayoutlibRuntimeArgument(extractLayoutlibRuntime.map { it.destinationDir }))
}
