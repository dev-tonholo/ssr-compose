package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontVersionsIndex
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.PublishedFonts
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The server's font steps of contracts/build-tooling.md, section Fonts, in a plain JVM server module. */
class FontServerTasksTest {
    @TempDir
    lateinit var root: File

    @TempDir
    lateinit var otherRoot: File

    @TempDir
    lateinit var cacheFolder: File

    private fun serverIn(
        folder: File,
        fontsBlock: String = PublishedFonts.serverBlock(version = VERSION),
        buildCache: File? = null,
    ): FixtureProject = FixtureProject(directory = folder.resolve("server"), buildCacheDirectory = buildCache)
        .configure(ssrComposeBlock = FixtureSources.producerBlock() + "\n" + fontsBlock)

    @Test
    fun `given a held font bundle, when it is synced and verified, then it is saved, pinned and verified`() {
        // Arrange
        val published = PublishedFonts(appFolder = root.resolve("app")).publish(version = VERSION)
        val server = serverIn(folder = root)

        // Act
        val sync = server.run(task = SYNC, CONFIGURATION_CACHE)
        val verify = server.run(task = VERIFY, CONFIGURATION_CACHE)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = sync.task(":$SYNC")?.outcome)
        assertEquals(expected = TaskOutcome.SUCCESS, actual = verify.task(":$VERIFY")?.outcome)
        val saved = root.resolve("server/ssr/fonts/$VERSION")
        val savedManifest = saved.resolve(FONTS_JSON).readBytes()
        assertContentEquals(expected = published.resolve(FONTS_JSON).readBytes(), actual = savedManifest)
        val lock = root.resolve("server/ssr/fonts.lock").readText()
        assertContains(charSequence = lock, other = "../app/ssr/fonts/$VERSION/fonts.json")
    }

    @Test
    fun `given a saved bundle and a republished one, when synced with update, then the saved bundle is replaced`() {
        // Arrange
        val app = PublishedFonts(appFolder = root.resolve("app"))
        app.publish(version = VERSION)
        val server = serverIn(folder = root)
        server.run(task = SYNC)
        val republished = app.publish(version = VERSION, text = "changed")

        // Act
        val kept = server.run(task = SYNC)
        val keptManifest = root.resolve("server/ssr/fonts/$VERSION/$FONTS_JSON").readBytes()
        server.run(task = SYNC, UPDATE)

        // Assert
        assertContains(charSequence = kept.output, other = UPDATE)
        assertFalse(actual = keptManifest.contentEquals(republished.resolve(FONTS_JSON).readBytes()))
        val replaced = root.resolve("server/ssr/fonts/$VERSION/$FONTS_JSON").readBytes()
        assertContentEquals(expected = republished.resolve(FONTS_JSON).readBytes(), actual = replaced)
    }

    @Test
    fun `given a tampered saved font file, when ssrFontVerify runs, then the build fails naming the file`() {
        // Arrange
        PublishedFonts(appFolder = root.resolve("app")).publish(version = VERSION)
        val server = serverIn(folder = root)
        server.run(task = SYNC)
        root.resolve("server/ssr/fonts/$VERSION/$BOLD_FILE").writeText("tampered")

        // Act
        val result = server.runAndFail(task = VERIFY)

        // Assert
        assertContains(charSequence = result.output, other = BOLD_FILE)
    }

    @Test
    fun `given a saved font file that is a link, when resources are processed, then the build fails naming the link`() {
        // Arrange
        PublishedFonts(appFolder = root.resolve("app")).publish(version = VERSION)
        val server = serverIn(folder = root)
        server.run(task = SYNC)
        val saved = root.resolve("server/ssr/fonts/$VERSION/$BOLD_FILE")
        val outside = root.resolve("outside.ttf").apply { writeBytes(saved.readBytes()) }
        saved.delete()
        Files.createSymbolicLink(saved.toPath(), outside.toPath())

        // Act
        val result = server.runAndFail(task = PROCESS_RESOURCES)

        // Assert
        assertContains(charSequence = result.output, other = BOLD_FILE)
        assertContains(charSequence = result.output, other = "symbolic link")
        assertFalse(actual = root.resolve("server/build/resources/main/ssrcompose/fonts/$VERSION").exists())
    }

    @Test
    fun `given a server module, when check is planned, then it runs ssrFontVerify and never ssrFontSync`() {
        // Arrange
        val server = serverIn(folder = root)

        // Act
        val plan = server.run(task = "check", DRY_RUN).output

        // Assert
        assertContains(charSequence = plan, other = ":$VERIFY ")
        assertFalse(actual = plan.contains(":$SYNC "), message = plan)
    }

    @Test
    fun `given a synced bundle, when resources are processed, then it is where the producer loads it`() {
        // Arrange
        val published = PublishedFonts(appFolder = root.resolve("app")).publish(version = VERSION)
        val server = serverIn(folder = root)
        server.run(task = SYNC)

        // Act
        val result = server.run(task = PROCESS_RESOURCES, CONFIGURATION_CACHE)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$RESOURCES")?.outcome)
        val resources = root.resolve("server/build/resources/main")
        assertContentEquals(
            expected = published.resolve(BOLD_FILE).readBytes(),
            actual = resources.resolve("ssrcompose/fonts/$VERSION/$BOLD_FILE").readBytes(),
        )
        val index = FontVersionsIndex.read(bytes = resources.resolve(INDEX).readBytes())
        assertEquals(expected = listOf(FontBundleVersion(value = VERSION)), actual = index)
    }

    @Test
    fun `given the same server in two folders, when the second copies fonts, then they come from the cache`() {
        // Arrange
        listOf(root, otherRoot).forEach { folder ->
            PublishedFonts(appFolder = folder.resolve("app")).publish(version = VERSION)
            serverIn(folder = folder, buildCache = cacheFolder).run(task = SYNC)
        }
        serverIn(folder = root, buildCache = cacheFolder).run(task = RESOURCES)

        // Act
        val result = serverIn(folder = otherRoot, buildCache = cacheFolder).run(task = RESOURCES)

        // Assert
        val outcome = result.task(":$RESOURCES")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
        val index = otherRoot.resolve("server/build/ssr/font-resources/$INDEX").readText()
        val holdsPath = index.contains(otherRoot.absolutePath) || index.contains(root.absolutePath)
        assertFalse(actual = holdsPath, message = index)
    }

    @Test
    fun `given a server with no font bundle, when the font steps run, then each does nothing`() {
        // Arrange
        val server = serverIn(folder = root, fontsBlock = "")

        // Act
        val result = server.run(task = SYNC, VERIFY, RESOURCES)

        // Assert
        val outcomes = listOf(SYNC, VERIFY).map { result.task(":$it")?.outcome }
        assertEquals(expected = List(size = 2) { TaskOutcome.SKIPPED }, actual = outcomes)
        assertFalse(actual = root.resolve("server/ssr/fonts.lock").exists())
    }

    @Test
    fun `given built font resources and then no held bundle, when resources are processed, then none are left`() {
        // Arrange
        PublishedFonts(appFolder = root.resolve("app")).publish(version = VERSION)
        serverIn(folder = root).run(task = SYNC)
        serverIn(folder = root).run(task = PROCESS_RESOURCES)

        // Act
        serverIn(folder = root, fontsBlock = "").run(task = PROCESS_RESOURCES)

        // Assert
        val resources = root.resolve("server/build/resources/main/ssrcompose")
        assertFalse(actual = resources.resolve("fonts").exists(), message = "an old font bundle is left")
        assertFalse(actual = resources.resolve("font-versions.json").exists(), message = "the old index is left")
    }

    private companion object {
        const val VERSION = "1"
        const val SYNC = "ssrFontSync"
        const val VERIFY = "ssrFontVerify"
        const val RESOURCES = "ssrFontResources"
        const val PROCESS_RESOURCES = "processResources"
        const val FONTS_JSON = "fonts.json"
        const val BOLD_FILE = "brand/700-normal.ttf"
        const val INDEX = "ssrcompose/font-versions.json"
        const val UPDATE = "--update"
        const val DRY_RUN = "--dry-run"
        const val CONFIGURATION_CACHE = "--configuration-cache"
    }
}
