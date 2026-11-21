package dev.tonholo.ssrcompose.compiler.testing.text

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.io.File
import kotlin.test.assertEquals

private const val RUNNER_FIXTURE = "TextRewriteRunner"
private const val RUNNER_CLASS = "app.text.TextRewriteRunner"
private const val RUNNER_FUNCTION = "probe"
private const val PRODUCER_CLASS = "dev.tonholo.ssrcompose.producer.ConsumerComponentKt"
private const val TEXT_SEPARATOR = ";"
private const val FIELD_SEPARATOR = ","
private const val NO_MARK = "none"

/** What the runner fixture read from one text of a drawn screen: its height in pixels and its mark's size in sp. */
internal data class TextProbe(val heightPx: Int, val markSizeSp: Float?)

/** The producer's own classes, which a fixture reads the internals of as a friend module. */
internal fun producerClasses(): File =
    File(Class.forName(PRODUCER_CLASS).protectionDomain.codeSource.location.toURI())

/** Compiles the runner fixture with the plugin [options], and fails the test when the compile fails. */
internal fun compileTextRunner(options: List<Pair<String, String>>): JvmCompilationResult =
    CompileTestSupport().compile(
        fixtureName = RUNNER_FIXTURE,
        options = options,
        friendPaths = listOf(producerClasses()),
    ).also { result ->
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

/** Draws the runner fixture's screen named [screen] and reads its texts. */
internal fun JvmCompilationResult.probe(screen: String, withCurve: Boolean = true): List<TextProbe> {
    val probe = classLoader.loadClass(RUNNER_CLASS).getMethod(RUNNER_FUNCTION, String::class.java, Boolean::class.java)
    return (probe.invoke(null, screen, withCurve) as String)
        .split(TEXT_SEPARATOR)
        .map { text ->
            val (height, mark) = text.split(FIELD_SEPARATOR)
            TextProbe(heightPx = height.toInt(), markSizeSp = mark.takeIf { value -> value != NO_MARK }?.toFloat())
        }
}
