package dev.tonholo.ssrcompose.gradle.testing

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.util.zip.ZipInputStream

/** An Android module of one [kind] that applies the plugin as an app module, with or without a `fonts { }` block. */
internal class AndroidModuleFixture(
    private val directory: File,
    private val kind: AndroidModuleKind,
    fontsBlock: String,
    buildCacheDirectory: File? = null,
    consumerBlock: String = CONSUMER_BLOCK,
) {
    private val cacheArguments = listOfNotNull(BUILD_CACHE.takeIf { buildCacheDirectory != null })

    init {
        val settings = FixtureSettings.script(buildCacheDirectory = buildCacheDirectory)
        write(relativePath = "settings.gradle.kts", text = settings)
        write(relativePath = "gradle.properties", text = GRADLE_PROPERTIES)
        // language=kotlin
        val ssrComposeBlock = "ssrCompose {\n    $consumerBlock\n$fontsBlock\n}\n"
        write(relativePath = "build.gradle.kts", text = kind.buildScript + "\n\n" + ssrComposeBlock)
        write(relativePath = "src/main/AndroidManifest.xml", text = MANIFEST)
        write(relativePath = FONT_FILE, text = "regular")
    }

    fun run(vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(directory)
            .withArguments(arguments.toList() + cacheArguments + CONFIGURATION_CACHE + STACKTRACE)
            .build()

    /** Builds the module's archive, an APK or an AAR, and reads [entry] from it or from a jar inside it. */
    fun builtEntry(entry: String): String? {
        run(kind.assembleTask)
        val archive = directory.resolve("build/outputs").walkTopDown().first { it.extension == kind.archiveExtension }
        return archive.inputStream().use { stream -> entryIn(zip = ZipInputStream(stream), entry = entry) }
    }

    private fun entryIn(zip: ZipInputStream, entry: String): String? {
        var found: String? = null
        var next = zip.nextEntry
        while (next != null && found == null) {
            found = when {
                next.name == entry -> zip.readBytes().decodeToString()
                next.name.endsWith(JAR_EXTENSION) -> entryIn(zip = nestedZip(bytes = zip.readBytes()), entry = entry)
                else -> null
            }
            next = zip.nextEntry
        }
        return found
    }

    private fun nestedZip(bytes: ByteArray) = ZipInputStream(bytes.inputStream())

    private fun write(relativePath: String, text: String) {
        directory.resolve(relativePath).apply { parentFile.mkdirs() }.writeText(text = text)
    }

    companion object {
        const val FONT_FILE = "fonts/inter_regular.ttf"
        private const val STACKTRACE = "--stacktrace"
        private const val CONFIGURATION_CACHE = "--configuration-cache"
        private const val BUILD_CACHE = "--build-cache"
        private const val JAR_EXTENSION = ".jar"

        // language=kotlin
        private const val CONSUMER_BLOCK = """consumer { catalogVersion = "1" }"""

        // language=properties
        private val GRADLE_PROPERTIES = """
            android.useAndroidX=true
            org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1g
        """.trimIndent()

        // language=xml
        private const val MANIFEST = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" />"""

        /** A `fonts { }` block with one regular font of the family `brand`, at [version]. */
        fun fontsBlock(version: String): String =
            // language=kotlin
            """
            fonts {
                version = "$version"
                family("brand") {
                    font(file = "$FONT_FILE", weight = 400)
                }
            }
            """.trimIndent()
    }
}
