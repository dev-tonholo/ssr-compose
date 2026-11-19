package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.gradle.testing.FixtureIndexes
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag(SCALE_TAG)
class CatalogRebuildScopeTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `given a compiled module, when only its index changes, then the whole module is rebuilt`() {
        // Arrange
        val fixture = compiledSingleModule()
        fixture.addFile(
            relativePath = FixtureSources.indexPath(),
            bytes = FixtureIndexes.withoutComponents(marker = CHANGED_INDEX),
        )

        // Act
        val output = fixture.run(task = COMPILE_TASK, INFO_FLAG).output

        // Assert
        assertTrue(actual = output.contains(FixtureSources.FULL_REBUILD_MESSAGE), message = output)
    }

    @Test
    fun `given a compiled module, when only a source file changes, then the module is not rebuilt in full`() {
        // Arrange
        val fixture = compiledSingleModule()
        val untouchedClass = projectDir.resolve(MARKED_CLASS_PATH)
        val untouchedClassTime = untouchedClass.lastModified()
        fixture.addFile(relativePath = OTHER_SOURCE_PATH, text = CHANGED_OTHER_SOURCE)

        // Act
        val output = fixture.run(task = COMPILE_TASK, INFO_FLAG).output

        // Assert
        assertFalse(actual = output.contains(FixtureSources.FULL_REBUILD_MESSAGE), message = output)
        assertTrue(actual = untouchedClass.lastModified() == untouchedClassTime, message = output)
    }

    private fun compiledSingleModule(): FixtureProject {
        val fixture = FixtureProject(directory = projectDir)
            .configure(ssrComposeBlock = FixtureSources.producerBlock())
            .addFile(relativePath = FixtureSources.MARKED_SOURCE_PATH, text = FixtureSources.MARKED_SOURCE)
            .addFile(relativePath = OTHER_SOURCE_PATH, text = OTHER_SOURCE)
            .addFile(
                relativePath = FixtureSources.indexPath(),
                bytes = FixtureIndexes.withoutComponents(marker = FIRST_INDEX),
            )
        fixture.compile()
        return fixture
    }

    private companion object {
        const val COMPILE_TASK = "compileKotlin"
        const val INFO_FLAG = "--info"
        const val OTHER_SOURCE_PATH = "src/main/kotlin/app/Other.kt"
        const val MARKED_CLASS_PATH = "build/classes/kotlin/main/MarkedKt.class"
        const val FIRST_INDEX = "FirstContent"
        const val CHANGED_INDEX = "SecondContent"

        // language=kotlin
        const val OTHER_SOURCE = "package app\n\nfun other(): Int = 1\n"

        // language=kotlin
        const val CHANGED_OTHER_SOURCE = "package app\n\nfun other(): Int = 2\n"
    }
}
