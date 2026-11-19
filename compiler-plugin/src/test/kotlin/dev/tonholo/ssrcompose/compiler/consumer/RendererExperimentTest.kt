package dev.tonholo.ssrcompose.compiler.consumer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RendererExperimentTest {
    private val support: CompileTestSupport = CompileTestSupport()
    private val callerSupport: CompileTestSupport = CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))
    private val consumerOptions: List<Pair<String, String>> = listOf(
        CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue,
    )

    @Test
    fun `given a marked composable with a text parameter, when compiled, then its renderer and caller compile`() {
        // Arrange
        val fixtures: List<String> = listOf(
            STAND_IN_FIXTURE,
            GREETING_FIXTURE,
            BANNER_FIXTURE,
            RUNNER_FIXTURE,
        )

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given a compiled renderer, when composed with a text value, then the composable gets that text`() {
        // Arrange
        val compiled = support.compile(
            fixtures = listOf(STAND_IN_FIXTURE, GREETING_FIXTURE, BANNER_FIXTURE, RUNNER_FIXTURE),
            options = consumerOptions,
        )
        assertEquals(expected = ExitCode.OK, actual = compiled.exitCode, message = compiled.messages)

        // Act
        val actual: String? = invokeRunner(
            results = listOf(compiled),
            methodName = RENDER_GREETING,
            argument = INPUT_TITLE,
        )

        // Assert
        assertEquals(expected = EXPECTED_GREETING, actual = actual)
    }

    @Test
    fun `given two files with renderers, when one is edited and recompiled, then the other renderer still runs`() {
        // Arrange
        val initial = support.compile(
            fixtures = listOf(STAND_IN_FIXTURE, GREETING_FIXTURE, BANNER_FIXTURE),
            options = consumerOptions,
        )
        assertEquals(expected = ExitCode.OK, actual = initial.exitCode, message = initial.messages)

        // Act
        val edited = support.compile(
            fixtureName = EDITED_BANNER_FIXTURE,
            fileName = BANNER_FIXTURE,
            options = consumerOptions,
            classpath = listOf(initial.outputDirectory),
        )
        val runner = callerSupport.compile(
            fixtureName = RUNNER_FIXTURE,
            classpath = listOf(edited.outputDirectory, initial.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = edited.exitCode, message = edited.messages)
        assertEquals(expected = ExitCode.OK, actual = runner.exitCode, message = runner.messages)
        val results: List<JvmCompilationResult> = listOf(runner, edited, initial)
        val actualBanner: String? = invokeRunner(
            results = results,
            methodName = RENDER_BANNER,
            argument = INPUT_HEADLINE,
        )
        val actualGreeting: String? = invokeRunner(
            results = results,
            methodName = RENDER_GREETING,
            argument = INPUT_TITLE,
        )
        assertEquals(expected = EXPECTED_EDITED_BANNER, actual = actualBanner)
        assertEquals(expected = EXPECTED_GREETING, actual = actualGreeting)
    }

    @Test
    fun `given a marked composable next to an unmarked overload, when composed, then the marked one runs`() {
        // Arrange
        val compiled = support.compile(
            fixtures = listOf(
                STAND_IN_FIXTURE,
                OVERLOADED_UNMARKED_GREETING_FIXTURE,
                BANNER_FIXTURE,
                RUNNER_FIXTURE,
            ),
            options = consumerOptions,
        )
        assertEquals(expected = ExitCode.OK, actual = compiled.exitCode, message = compiled.messages)

        // Act
        val actual: String? = invokeRunner(
            results = listOf(compiled),
            methodName = RENDER_GREETING,
            argument = INPUT_TITLE,
        )

        // Assert
        assertEquals(expected = EXPECTED_GREETING, actual = actual)
    }

    @Test
    fun `given two marked composables with the same name in one package, when compiled, then both declarations fail`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, DUPLICATE_MARKED_GREETING_FIXTURE)
        val expectedSuffixes: List<String> = listOf(
            FIRST_DUPLICATE_DIAGNOSTIC,
            SECOND_DUPLICATE_DIAGNOSTIC,
        )

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = expectedSuffixes.size, actual = lines.size, message = result.messages)
        expectedSuffixes.zip(lines).forEach { (expectedSuffix, line) ->
            assertTrue(actual = line.endsWith(suffix = expectedSuffix), message = result.messages)
        }
    }

    @Test
    fun `given a marked extension composable, when compiled, then it fails at the declaration`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, EXTENSION_MARKED_COMPOSABLE_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = 1, actual = lines.size, message = result.messages)
        assertTrue(actual = lines.single().endsWith(suffix = EXTENSION_RECEIVER_DIAGNOSTIC), message = result.messages)
    }

    @Test
    fun `given a marked composable with context parameters, when compiled, then it fails at the declaration`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, CONTEXT_MARKED_COMPOSABLE_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = 1, actual = lines.size, message = result.messages)
        assertTrue(actual = lines.single().endsWith(suffix = CONTEXT_PARAMETERS_DIAGNOSTIC), message = result.messages)
    }

    @Test
    fun `given a marked composable with type parameters, when compiled, then it fails at the declaration`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, GENERIC_MARKED_COMPOSABLE_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = 1, actual = lines.size, message = result.messages)
        assertTrue(actual = lines.single().endsWith(suffix = TYPE_PARAMETERS_DIAGNOSTIC), message = result.messages)
    }

    @Test
    fun `given a private marked composable, when compiled, then it fails at the declaration`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, PRIVATE_MARKED_COMPOSABLE_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = 1, actual = lines.size, message = result.messages)
        assertTrue(actual = lines.single().endsWith(suffix = PRIVATE_DIAGNOSTIC), message = result.messages)
    }

    @Test
    fun `given a marked composable with an Int parameter, when a caller names its renderer, then it is unresolved`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, INT_RENDERER_CALLER_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(
            actual = result.messages.contains(other = UNRESOLVED_COUNTER_RENDERER_MESSAGE),
            message = result.messages,
        )
    }

    @Test
    fun `given a marked composable with a slot parameter, when a caller names its renderer, then it is unresolved`() {
        // Arrange
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, SLOT_RENDERER_CALLER_FIXTURE)

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(
            actual = result.messages.contains(other = UNRESOLVED_SECTION_RENDERER_MESSAGE),
            message = result.messages,
        )
    }

    @Test
    fun `given a user file named GreetingRenderer in the same package, when compiled, then both compile`() {
        // Arrange
        val fixtures: List<String> = listOf(
            STAND_IN_FIXTURE,
            GREETING_FIXTURE,
            BANNER_FIXTURE,
            USER_GREETING_RENDERER_FIXTURE,
            RUNNER_FIXTURE,
        )
        val fileNames: Map<String, String> = mapOf(
            USER_GREETING_RENDERER_FIXTURE to USER_GREETING_RENDERER_FILE_NAME,
        )

        // Act
        val result = support.compile(fixtures = fixtures, options = consumerOptions, fileNames = fileNames)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given Compose registered before the consumer plugin, when compiled, then it fails with the guard message`() {
        // Arrange
        val wrongOrder = CompileTestSupport(
            registrars = listOf(ComposePluginRegistrar(), SsrCompilerPluginRegistrar()),
        )
        val fixtures: List<String> = listOf(STAND_IN_FIXTURE, GREETING_FIXTURE)

        // Act
        val result = wrongOrder.compile(fixtures = fixtures, options = consumerOptions)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(other = ORDER_GUARD_MESSAGE), message = result.messages)
    }

    private fun invokeRunner(results: List<JvmCompilationResult>, methodName: String, argument: String): String? {
        val urls = results.map { result -> result.outputDirectory.toURI().toURL() }.toTypedArray()
        return URLClassLoader(urls, javaClass.classLoader).use { loader ->
            val method = loader.loadClass(RUNNER_CLASS).getMethod(methodName, String::class.java)
            method.invoke(null, argument) as? String
        }
    }

    private fun JvmCompilationResult.errorLines(): List<String> =
        messages.lines().filter { line -> line.startsWith(prefix = ERROR_PREFIX) }

    private companion object {
        const val STAND_IN_FIXTURE = "PlacedComponentScopeStandIn"
        const val GREETING_FIXTURE = "RenderableGreeting"
        const val BANNER_FIXTURE = "RenderableBanner"
        const val EDITED_BANNER_FIXTURE = "EditedRenderableBanner"
        const val OVERLOADED_UNMARKED_GREETING_FIXTURE = "OverloadedWithUnmarkedGreeting"
        const val DUPLICATE_MARKED_GREETING_FIXTURE = "DuplicateMarkedGreeting"
        const val EXTENSION_MARKED_COMPOSABLE_FIXTURE = "ExtensionMarkedComposable"
        const val CONTEXT_MARKED_COMPOSABLE_FIXTURE = "ContextMarkedComposable"
        const val INT_RENDERER_CALLER_FIXTURE = "IntRendererCaller"
        const val SLOT_RENDERER_CALLER_FIXTURE = "SlotRendererCaller"
        const val USER_GREETING_RENDERER_FIXTURE = "UserGreetingRenderer"
        const val USER_GREETING_RENDERER_FILE_NAME = "GreetingRenderer.kt"
        const val RUNNER_FIXTURE = "RendererCallerRunner"
        const val RUNNER_CLASS = "fixtures.consumer.renderer.RendererCallerRunner"
        const val RENDER_GREETING = "renderGreeting"
        const val RENDER_BANNER = "renderBanner"
        const val INPUT_TITLE = "Welcome aboard"
        const val EXPECTED_GREETING = "greeting:Welcome aboard"
        const val INPUT_HEADLINE = "Spring Sale"
        const val EXPECTED_EDITED_BANNER = "updated-banner:Spring Sale"
        const val DUPLICATE_MARKED_BODY =
            "The marked composable 'Greeting' is declared more than once in package " +
                "'fixtures.consumer.renderer'. Marked composables in the same package cannot be overloaded."
        const val FIRST_DUPLICATE_DIAGNOSTIC = "DuplicateMarkedGreeting.fixture.kt:6:1 $DUPLICATE_MARKED_BODY"
        const val SECOND_DUPLICATE_DIAGNOSTIC = "DuplicateMarkedGreeting.fixture.kt:11:1 $DUPLICATE_MARKED_BODY"
        const val EXTENSION_RECEIVER_DIAGNOSTIC =
            "ExtensionMarkedComposable.fixture.kt:6:1 The marked composable 'Tag' has an extension receiver. " +
                "Only top-level composables without an extension receiver or type parameters can be marked."
        const val CONTEXT_PARAMETERS_DIAGNOSTIC =
            "ContextMarkedComposable.fixture.kt:6:1 The marked composable 'Tag' has context parameters. " +
                "Only top-level composables without an extension receiver or context parameters can be marked."
        const val GENERIC_MARKED_COMPOSABLE_FIXTURE = "GenericMarkedComposable"
        const val PRIVATE_MARKED_COMPOSABLE_FIXTURE = "PrivateMarkedComposable"
        const val TYPE_PARAMETERS_DIAGNOSTIC =
            "GenericMarkedComposable.fixture.kt:6:1 The marked composable 'Tag' has type parameters. " +
                "Only top-level composables without type parameters can be marked."
        const val PRIVATE_DIAGNOSTIC =
            "PrivateMarkedComposable.fixture.kt:6:1 The marked composable 'Tag' is private. " +
                "Only internal or public composables can be marked."
        const val UNRESOLVED_COUNTER_RENDERER_MESSAGE = "Unresolved reference 'CounterRenderer'"
        const val UNRESOLVED_SECTION_RENDERER_MESSAGE = "Unresolved reference 'SectionRenderer'"
        const val ORDER_GUARD_MESSAGE = "must run before the Compose compiler plugin"
        const val ERROR_PREFIX = "e: "
    }
}
