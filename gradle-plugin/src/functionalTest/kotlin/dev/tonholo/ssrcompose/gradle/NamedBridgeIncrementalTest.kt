package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.NamedBridgeFixture
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamedBridgeIncrementalTest {
    @TempDir
    lateinit var incrementalDir: File

    @TempDir
    lateinit var cleanDir: File

    @Test
    fun `given no call to a component, when the first call is added, then the classes match a clean build`() {
        // Arrange
        val fixture = compiledFixture(sources = BASE_SOURCES)
        val edited = BASE_SOURCES + (BETA_SCREEN to betaScreen(title = "first"))

        // Act
        val output = fixture.applyAndCompile(sources = edited)

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = edited)
    }

    @Test
    fun `given the last call to a component, when it is removed, then the classes match a clean build`() {
        // Arrange
        val sources = BASE_SOURCES + (BETA_SCREEN to betaScreen(title = "first"))
        val fixture = compiledFixture(sources = sources)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))

        // Act
        fixture.deleteSource(relativePath = BETA_SCREEN)
        val output = fixture.compile().output

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertFalse(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = BASE_SOURCES)
    }

    @Test
    fun `given two callers of a component, when one is removed, then the classes match a clean build`() {
        // Arrange
        val sources = BASE_SOURCES + mapOf(
            BETA_SCREEN to betaScreen(title = "first"),
            SECOND_BETA_SCREEN to secondBetaScreen(),
        )
        val fixture = compiledFixture(sources = sources)

        // Act
        fixture.deleteSource(relativePath = SECOND_BETA_SCREEN)
        val output = fixture.compile().output

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = sources - SECOND_BETA_SCREEN)
    }

    @Test
    fun `given a bridge called by one file, when another file is edited, then the classes match a clean build`() {
        // Arrange
        val sources = BASE_SOURCES + (BETA_SCREEN to betaScreen(title = "first"))
        val fixture = compiledFixture(sources = sources)
        val edited = sources + (ALPHA_SCREEN to alphaScreen(title = "second"))

        // Act
        val output = fixture.applyAndCompile(sources = edited)

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = edited)
    }

    @Test
    fun `given a file without a call, when an edit adds the first call, then the classes match a clean build`() {
        // Arrange
        val sources = BASE_SOURCES + (EDITED_FILE to fileWithoutCall())
        val fixture = compiledFixture(sources = sources)
        val edited = sources + (EDITED_FILE to fileCallingBeta())

        // Act
        val output = fixture.applyAndCompile(sources = edited)

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = edited)
    }

    @Test
    fun `given a file with the last call, when an edit removes it, then the classes match a clean build`() {
        // Arrange
        val sources = BASE_SOURCES + (EDITED_FILE to fileCallingBeta())
        val fixture = compiledFixture(sources = sources)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        val edited = sources + (EDITED_FILE to fileWithoutCall())

        // Act
        val output = fixture.applyAndCompile(sources = edited)

        // Assert
        assertStayedIncremental(fixture = fixture, output = output)
        assertFalse(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
        assertMatchesCleanBuild(fixture = fixture, sources = edited)
    }

    private fun compiledFixture(sources: Map<String, String>): NamedBridgeFixture {
        val fixture = NamedBridgeFixture(directory = incrementalDir)
        sources.forEach { (path, text) -> fixture.writeSource(relativePath = path, text = text) }
        fixture.compile()
        untouchedClassTime = fixture.classTime(relativePath = PLAIN_CLASS)
        return fixture
    }

    private fun NamedBridgeFixture.applyAndCompile(sources: Map<String, String>): String {
        sources.forEach { (path, text) -> writeSource(relativePath = path, text = text) }
        return compile().output
    }

    private fun assertStayedIncremental(fixture: NamedBridgeFixture, output: String) {
        assertFalse(actual = output.contains(FixtureSources.FULL_REBUILD_MESSAGE), message = output)
        assertEquals(
            expected = untouchedClassTime,
            actual = fixture.classTime(relativePath = PLAIN_CLASS),
            message = output,
        )
    }

    private fun assertMatchesCleanBuild(fixture: NamedBridgeFixture, sources: Map<String, String>) {
        val clean = NamedBridgeFixture(directory = cleanDir)
        sources.forEach { (path, text) -> clean.writeSource(relativePath = path, text = text) }
        clean.compile()
        assertEquals(expected = clean.classFiles(), actual = fixture.classFiles())
    }

    private var untouchedClassTime: Long = 0L

    private companion object {
        const val ALPHA_SCREEN = "src/main/kotlin/app/screens/AlphaScreen.kt"
        const val BETA_SCREEN = "src/main/kotlin/app/screens/BetaScreen.kt"
        const val SECOND_BETA_SCREEN = "src/main/kotlin/app/screens/SecondBetaScreen.kt"
        const val PLAIN_SOURCE = "src/main/kotlin/app/screens/Plain.kt"
        const val PLAIN_CLASS = "app/screens/PlainKt.class"
        const val EDITED_FILE = "src/main/kotlin/app/screens/Edited.kt"

        // language=kotlin
        const val PLAIN_TEXT = "package app.screens\n\nfun plain(): Int = 1\n"

        val BASE_SOURCES: Map<String, String> = mapOf(
            ALPHA_SCREEN to alphaScreen(title = "first"),
            PLAIN_SOURCE to PLAIN_TEXT,
        )

        fun alphaScreen(title: String): String =
            FixtureSources.bridgeScreenSource(component = NamedBridgeFixture.ALPHA, title = title)

        fun betaScreen(title: String): String =
            FixtureSources.bridgeScreenSource(component = NamedBridgeFixture.BETA, title = title)

        fun fileWithoutCall(): String =
            // language=kotlin
            """
            package app.screens

            fun edited(): Int = 1
            """.trimIndent()

        fun fileCallingBeta(): String =
            // language=kotlin
            """
            package app.screens

            import androidx.compose.runtime.Composable
            import ${FixtureSources.DEFAULT_BRIDGE_PACKAGE}.${NamedBridgeFixture.BETA}

            fun edited(): Int = 1

            @Composable
            fun EditedScreen() {
                ${NamedBridgeFixture.BETA}(title = "edited")
            }
            """.trimIndent()

        fun secondBetaScreen(): String =
            // language=kotlin
            """
            package app.screens

            import androidx.compose.runtime.Composable
            import ${FixtureSources.DEFAULT_BRIDGE_PACKAGE}.${NamedBridgeFixture.BETA}

            @Composable
            fun SecondBetaScreen() {
                ${NamedBridgeFixture.BETA}(title = "second")
            }
            """.trimIndent()
    }
}
