package dev.tonholo.ssrcompose.gradle.testing

import org.gradle.testkit.runner.BuildResult
import java.io.File

/** A server module that holds one catalog of [COMPONENTS] and creates the bridges its sources name. */
internal class NamedBridgeFixture(
    private val directory: File,
    buildCacheDirectory: File? = null,
    extraBuildScript: String = "",
) {
    private val project = FixtureProject(directory = directory, buildCacheDirectory = buildCacheDirectory)
        .configure(
            ssrComposeBlock = FixtureSources.producerBlock(),
            extraPlugins = listOf(COMPOSE_PLUGIN),
            extraBuildScript = PRODUCER_DEPENDENCY + extraBuildScript,
        )
        .addFile(
            relativePath = FixtureSources.indexPath(),
            bytes = FixtureIndexes.withComponents(names = COMPONENTS),
        )

    fun writeSource(relativePath: String, text: String): NamedBridgeFixture {
        project.addFile(relativePath = relativePath, text = text)
        return this
    }

    fun deleteSource(relativePath: String) {
        check(directory.resolve(relativePath).delete()) { relativePath }
    }

    fun compile(): BuildResult = project.run(COMPILE_TASK, INFO_FLAG)

    /** Every class file of the module by its path, with its bytes as text so two maps can be compared. */
    fun classFiles(): Map<String, String> {
        val root = directory.resolve(CLASSES_FOLDER)
        return root.walkTopDown()
            .filter { it.isFile && it.extension == CLASS_EXTENSION }
            .associate { it.relativeTo(root).invariantSeparatorsPath to it.readBytes().contentToString() }
    }

    /** The time the class file at [relativePath], under the classes folder, was last written. */
    fun classTime(relativePath: String): Long = directory.resolve("$CLASSES_FOLDER/$relativePath").lastModified()

    /** True when a class in the bridge package declares the bridge function of [component]. */
    fun declaresBridge(component: String): Boolean =
        directory.resolve("$CLASSES_FOLDER/$BRIDGE_FOLDER").walkTopDown()
            .filter { it.isFile && it.extension == CLASS_EXTENSION }
            .any { file -> disassembled(classFile = file).contains(" void $component(") }

    /** The class files of the module that hold the absolute path of any of [folders]. */
    fun classFilesHoldingPathOf(folders: List<File>): List<String> {
        val paths = folders.map { folder -> folder.absolutePath }
        return classFiles().keys.filter { name ->
            val text = directory.resolve("$CLASSES_FOLDER/$name").readBytes().toString(Charsets.ISO_8859_1)
            paths.any { path -> text.contains(path) }
        }
    }

    private fun disassembled(classFile: File): String {
        val javap = File(System.getProperty(JAVA_HOME_PROPERTY), JAVAP_PATH).path
        val process = ProcessBuilder(javap, "-p", classFile.path).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output
    }

    companion object {
        const val ALPHA = "Alpha"
        const val BETA = "Beta"
        private val COMPONENTS = listOf(ALPHA, BETA)
        private const val COMPILE_TASK = "compileKotlin"
        private const val INFO_FLAG = "--info"
        private const val COMPOSE_PLUGIN = "alias(libs.plugins.kotlin.compose)"
        private const val CLASSES_FOLDER = "build/classes/kotlin/main"
        private const val BRIDGE_FOLDER = "app/bridge/v2"
        private const val CLASS_EXTENSION = "class"
        private const val JAVA_HOME_PROPERTY = "java.home"
        private const val JAVAP_PATH = "bin/javap"

        // language=kotlin
        private const val PRODUCER_DEPENDENCY = """
            dependencies {
                implementation("dev.tonholo.ssrcompose:producer")
            }
        """
    }
}
