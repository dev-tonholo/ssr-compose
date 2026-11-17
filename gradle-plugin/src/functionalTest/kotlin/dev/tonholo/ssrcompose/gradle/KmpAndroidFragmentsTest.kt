package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.KmpAndroidFixtureProject
import dev.tonholo.ssrcompose.gradle.testing.KmpAndroidSources
import dev.tonholo.ssrcompose.gradle.testing.VariantPart
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KmpAndroidFragmentsTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var cleanDir: File

    @Test
    fun `given two flavors, when each variant is compiled, then each has its own fragments folder`() {
        // Arrange
        val project = fixtureIn(folder = projectDir)

        // Act
        project.compile(task = FREE_DEBUG_COMPILE)
        project.compile(task = PAID_DEBUG_COMPILE)

        // Assert
        val free = componentNames(folder = project.file(relativePath = FREE_DEBUG_FOLDER))
        val paid = componentNames(folder = project.file(relativePath = PAID_DEBUG_FOLDER))
        assertEquals(expected = setOf("Home", "Shared", "Panel", "FreeBanner"), actual = free)
        assertEquals(expected = setOf("Home", "Shared", "Panel", "PaidBanner"), actual = paid)
        val metadataFolder = project.file(relativePath = METADATA_FOLDER)
        assertFalse(actual = metadataFolder.exists(), message = "a metadata compilation wrote fragments")
    }

    @Test
    fun `given a marked expect and actual pair, when a variant is compiled, then its part holds it once`() {
        // Arrange
        val project = fixtureIn(folder = projectDir)

        // Act
        project.compile(task = FREE_DEBUG_COMPILE)

        // Assert
        val names = VariantPart.partFrom(folder = project.file(relativePath = FREE_DEBUG_FOLDER)).components
            .map { it.name }
        assertEquals(expected = 1, actual = names.count { it == "Shared" }, message = names.toString())
        assertEquals(expected = 1, actual = names.count { it == "Panel" }, message = names.toString())
    }

    @Test
    fun `given an edit to a flavor file, when compiled again, then only its fragment is written again`() {
        // Arrange
        val project = fixtureIn(folder = projectDir)
        project.compile(task = FREE_DEBUG_COMPILE)
        val folder = project.file(relativePath = FREE_DEBUG_FOLDER)
        val partBeforeEdit = VariantPart.bytesFrom(folder = folder)
        val modifiedBefore = folder.listFiles().orEmpty().associate { it.name to it.lastModified() }
        val path = KmpAndroidSources.FREE_BANNER_PATH
        val edit = KmpAndroidSources.EDITED_FREE_BANNER

        // Act
        project.addFile(relativePath = path, text = edit)
        project.compile(task = FREE_DEBUG_COMPILE)
        val cleanProject = cleanBuildWithEdit(path = path, text = edit)

        // Assert
        val editedPart = VariantPart.bytesFrom(folder = folder)
        val cleanPart = VariantPart.bytesFrom(folder = cleanProject.file(relativePath = FREE_DEBUG_FOLDER))
        val rewritten = folder.listFiles().orEmpty().filter { modifiedBefore[it.name] != it.lastModified() }
        assertNotEquals(illegal = partBeforeEdit.decodeToString(), actual = editedPart.decodeToString())
        assertContentEquals(expected = cleanPart, actual = editedPart)
        assertEquals(expected = listOf(FREE_BANNER_FRAGMENT), actual = rewritten.map { it.name })
        assertTrue(actual = "Home" in componentNames(folder = folder), message = "an untouched file lost its component")
    }

    @Test
    fun `given an edit to a common file, when the variant is compiled again, then the part equals a clean build`() {
        // Arrange
        val project = fixtureIn(folder = projectDir)
        project.compile(task = FREE_DEBUG_COMPILE)
        val partBeforeEdit = VariantPart.bytesFrom(folder = project.file(relativePath = FREE_DEBUG_FOLDER))
        val path = KmpAndroidSources.HOME_PATH
        val edit = KmpAndroidSources.EDITED_HOME

        // Act
        project.addFile(relativePath = path, text = edit)
        project.compile(task = FREE_DEBUG_COMPILE)
        val cleanProject = cleanBuildWithEdit(path = path, text = edit)

        // Assert
        val editedPart = VariantPart.bytesFrom(folder = project.file(relativePath = FREE_DEBUG_FOLDER))
        val cleanPart = VariantPart.bytesFrom(folder = cleanProject.file(relativePath = FREE_DEBUG_FOLDER))
        assertNotEquals(illegal = partBeforeEdit.decodeToString(), actual = editedPart.decodeToString())
        assertContentEquals(expected = cleanPart, actual = editedPart)
    }

    private fun cleanBuildWithEdit(path: String, text: String): KmpAndroidFixtureProject =
        fixtureIn(folder = cleanDir).addFile(relativePath = path, text = text).also { clean ->
            clean.compile(task = FREE_DEBUG_COMPILE)
        }

    private fun fixtureIn(folder: File): KmpAndroidFixtureProject {
        val project = KmpAndroidFixtureProject(directory = folder)
        KmpAndroidSources.ALL.forEach { (path, text) -> project.addFile(relativePath = path, text = text) }
        return project
    }

    private fun componentNames(folder: File): Set<String> =
        VariantPart.fragmentsIn(folder = folder).flatMap { fragment -> fragment.components.map { it.name } }.toSet()

    private companion object {
        const val FREE_DEBUG_COMPILE = "compileFreeDebugKotlinAndroid"
        const val PAID_DEBUG_COMPILE = "compilePaidDebugKotlinAndroid"
        const val FREE_DEBUG_FOLDER = "build/ssr/fragments/android/freeDebug"
        const val PAID_DEBUG_FOLDER = "build/ssr/fragments/android/paidDebug"
        const val METADATA_FOLDER = "build/ssr/fragments/metadata"
        const val FREE_BANNER_FRAGMENT = "fixture..kmp..FreeBanner.kt.fragment.json"
    }
}
