package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.AndroidModuleFixture
import dev.tonholo.ssrcompose.gradle.testing.AndroidModuleKind
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The font version resource of ADR 0022: written for each Android variant of the module that declares the fonts. */
class AndroidFontVersionTest {
    @TempDir
    lateinit var root: File

    @TempDir
    lateinit var otherRoot: File

    @TempDir
    lateinit var cacheFolder: File

    private fun moduleOf(
        kind: AndroidModuleKind,
        fontsBlock: String = AndroidModuleFixture.fontsBlock(version = VERSION),
    ) = AndroidModuleFixture(directory = root, kind = kind, fontsBlock = fontsBlock)

    @Test
    fun `given an application that declares fonts, when it is built, then its APK holds the font version`() {
        // Arrange
        val app = moduleOf(kind = AndroidModuleKind.Application)

        // Act
        val actual = app.builtEntry(entry = RESOURCE)

        // Assert
        assertEquals(expected = VERSION, actual = actual)
    }

    @Test
    fun `given an application with fonts, when its tasks are listed, then each variant has its version task`() {
        // Arrange
        val app = moduleOf(kind = AndroidModuleKind.Application)

        // Act
        val tasks = app.run("tasks", "--all").output

        // Assert
        listOf("ssrFontVersionDebug", "ssrFontVersionRelease").forEach { task ->
            assertContains(charSequence = tasks, other = task)
        }
    }

    @Test
    fun `given the same application in two folders, when the second writes its version, then it is from the cache`() {
        // Arrange
        val fontsBlock = AndroidModuleFixture.fontsBlock(version = VERSION)
        val kind = AndroidModuleKind.Application
        AndroidModuleFixture(directory = root, kind = kind, fontsBlock = fontsBlock, buildCacheDirectory = cacheFolder)
            .run(DEBUG_VERSION_TASK)
        val second = AndroidModuleFixture(
            directory = otherRoot,
            kind = kind,
            fontsBlock = fontsBlock,
            buildCacheDirectory = cacheFolder,
        )

        // Act
        val result = second.run(DEBUG_VERSION_TASK)

        // Assert
        val outcome = result.task(":$DEBUG_VERSION_TASK")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
    }

    @Test
    fun `given an application with no fonts block, when it is built, then its APK holds no font version`() {
        // Arrange
        val app = moduleOf(kind = AndroidModuleKind.Application, fontsBlock = "")

        // Act
        val actual = app.builtEntry(entry = RESOURCE)

        // Assert
        assertEquals(expected = null, actual = actual)
        assertFalse(actual = app.run("tasks", "--all").output.contains("ssrFontVersion"))
    }

    @Test
    fun `given an application with fonts and a producer block, when it is built, then it has no font version`() {
        // Arrange
        val app = AndroidModuleFixture(
            directory = root,
            kind = AndroidModuleKind.Application,
            fontsBlock = AndroidModuleFixture.fontsBlock(version = VERSION),
            consumerBlock = "producer { }",
        )

        // Act
        val actual = app.builtEntry(entry = RESOURCE)

        // Assert
        assertEquals(expected = null, actual = actual)
        assertFalse(actual = app.run("tasks", "--all").output.contains("ssrFontVersion"))
    }

    @Test
    fun `given an Android library that declares fonts, when it is built, then its AAR holds the font version`() {
        // Arrange
        val library = moduleOf(kind = AndroidModuleKind.Library)

        // Act
        val actual = library.builtEntry(entry = RESOURCE)

        // Assert
        assertEquals(expected = VERSION, actual = actual)
    }

    @Test
    fun `given a multiplatform Android library with fonts, when it is built, then its AAR holds the font version`() {
        // Arrange
        val library = moduleOf(kind = AndroidModuleKind.KotlinMultiplatformLibrary)

        // Act
        val actual = library.builtEntry(entry = RESOURCE)

        // Assert
        assertEquals(expected = VERSION, actual = actual)
    }

    private companion object {
        const val VERSION = "brand-1"
        const val RESOURCE = "ssrcompose/font-version"
        const val DEBUG_VERSION_TASK = "ssrFontVersionDebug"
    }
}
