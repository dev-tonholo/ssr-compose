package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The app's font steps of contracts/build-tooling.md, section Fonts: bundle, check and dump. */
class FontAppTasksTest {
    @TempDir
    lateinit var root: File

    @TempDir
    lateinit var otherRoot: File

    @TempDir
    lateinit var cacheFolder: File

    private fun appIn(folder: File, fontsBlock: String = fontsBlock(), buildCache: File? = null): FixtureProject =
        FixtureProject(directory = folder, buildCacheDirectory = buildCache)
            .configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK + "\n" + fontsBlock)
            .addFile(relativePath = REGULAR_SOURCE, text = "regular")
            .addFile(relativePath = BOLD_SOURCE, text = "bold")

    @Test
    fun `given declared fonts and no saved bundle, when checked, then it fails and says to run ssrFontDump`() {
        // Arrange
        val app = appIn(folder = root)

        // Act
        val result = app.runAndFail(task = CHECK)

        // Assert
        assertContains(charSequence = result.output, other = DUMP)
    }

    @Test
    fun `given declared fonts, when dumped then checked, then the bundle is saved and the check passes`() {
        // Arrange
        val app = appIn(folder = root)

        // Act
        app.run(task = DUMP, CONFIGURATION_CACHE)
        val result = app.run(task = CHECK, CONFIGURATION_CACHE)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$CHECK")?.outcome)
        val savedRegular = root.resolve("ssr/fonts/$VERSION/brand/400-normal.ttf").readText()
        assertEquals(expected = "regular", actual = savedRegular)
        assertTrue(actual = root.resolve("ssr/fonts/$VERSION/fonts.json").isFile, message = "fonts.json is not saved")
    }

    @Test
    fun `given two files swapped between weights with the same version, when checked, then it fails`() {
        // Arrange
        appIn(folder = root).run(task = DUMP)
        val swappedBlock = fontsBlock(regularFile = BOLD_SOURCE, boldFile = REGULAR_SOURCE)
        val swapped = appIn(folder = root, fontsBlock = swappedBlock)

        // Act
        val result = swapped.runAndFail(task = CHECK)

        // Assert
        assertContains(charSequence = result.output, other = "brand/400-normal.ttf")
    }

    @Test
    fun `given a changed weight with the same file and version, when checked, then it fails naming the new file`() {
        // Arrange
        appIn(folder = root).run(task = DUMP)
        val reweighted = appIn(folder = root, fontsBlock = fontsBlock(regularWeight = MEDIUM))

        // Act
        val result = reweighted.runAndFail(task = CHECK)

        // Assert
        assertContains(charSequence = result.output, other = "brand/$MEDIUM-normal.ttf")
    }

    @Test
    fun `given an app module, when check is planned, then it runs ssrFontCheck and never ssrFontDump`() {
        // Arrange
        val app = appIn(folder = root)

        // Act
        val plan = app.run(task = "check", DRY_RUN).output

        // Assert
        assertContains(charSequence = plan, other = ":$CHECK ")
        assertFalse(actual = plan.contains(":$DUMP "), message = plan)
    }

    @Test
    fun `given the same app in two folders, when the second bundles its fonts, then the bundle comes from the cache`() {
        // Arrange
        appIn(folder = root, buildCache = cacheFolder).run(task = BUNDLE)

        // Act
        val result = appIn(folder = otherRoot, buildCache = cacheFolder).run(task = BUNDLE)

        // Assert
        val outcome = result.task(":$BUNDLE")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
    }

    @Test
    fun `given an app with no fonts block, when the font steps run, then each does nothing`() {
        // Arrange
        val app = appIn(folder = root, fontsBlock = "")

        // Act
        val result = app.run(task = BUNDLE, CHECK, DUMP)

        // Assert
        val outcomes = listOf(BUNDLE, CHECK, DUMP).map { result.task(":$it")?.outcome }
        assertEquals(expected = List(size = 3) { TaskOutcome.SKIPPED }, actual = outcomes)
        assertFalse(actual = root.resolve("ssr/fonts").exists())
    }

    private fun fontsBlock(
        regularFile: String = REGULAR_SOURCE,
        boldFile: String = BOLD_SOURCE,
        regularWeight: Int = REGULAR,
    ): String =
        // language=kotlin
        """
        fonts {
            version = "$VERSION"
            family("brand") {
                font(file = "$regularFile", weight = $regularWeight)
                font(file = "$boldFile", weight = 700)
            }
        }
        """.trimIndent()

    private companion object {
        const val VERSION = "1"
        const val REGULAR = 400
        const val MEDIUM = 500
        const val BUNDLE = "ssrFontBundle"
        const val CHECK = "ssrFontCheck"
        const val DUMP = "ssrFontDump"
        const val REGULAR_SOURCE = "src/main/res/font/inter_regular.ttf"
        const val BOLD_SOURCE = "src/main/res/font/inter_bold.ttf"
        const val DRY_RUN = "--dry-run"
        const val CONFIGURATION_CACHE = "--configuration-cache"
    }
}
