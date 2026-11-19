package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.catalog.options.SourceFileList
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.sourceFolderOption
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamedBridgeCreationTest {
    @TempDir
    lateinit var workDir: File

    @Test
    fun `given two components and code naming one, when compiled, then only the named one gets a function`() {
        // Arrange
        val source = callerOf(component = GREETING)

        // Act
        val result = compile(source = source, held = PLAIN_CATALOG)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = result))
    }

    @TestFactory
    fun `given each way code can name a bridge, when compiled, then the bridge is created`() =
        NAMING_SHAPES.map { (shape, source) ->
            dynamicTest(shape) {
                // Arrange
                val held = PLAIN_CATALOG

                // Act
                val result = compile(source = source, held = held)

                // Assert
                assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
                assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = result))
            }
        }

    @Test
    fun `given two held catalogs, when code names a component of each, then both catalogs create both bridges`() {
        // Arrange
        val options = producerOptions(
            catalog(catalog = PLAIN_CATALOG, bridgePackage = BRIDGE_PACKAGE),
            catalog(catalog = SECOND_CATALOG, bridgePackage = SECOND_PACKAGE),
        )

        // Act
        val result = CompileTestSupport().compileSource(
            fileName = CALLER_FILE,
            source = TWO_CATALOG_CALLER,
            options = options,
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = setOf(GREETING, CARD), actual = createdFunctions(result = result))
        assertEquals(
            expected = setOf(GREETING, CARD),
            actual = createdFunctions(result = result, bridgePackage = SECOND_PACKAGE),
        )
    }

    @TestFactory
    fun `given an unused component the plugin cannot create, when compiled, then the build passes`() =
        UNCREATABLE_COMPONENTS.map { component ->
            dynamicTest(component.name) {
                // Arrange
                val held = TestCatalogs.catalogOf(version = VERSION, GREETING_COMPONENT, component)

                // Act
                val result = compile(source = callerOf(component = GREETING), held = held)

                // Assert
                assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
                assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = result))
            }
        }

    @TestFactory
    fun `given a false match of a component the plugin cannot create, when compiled, then the build passes`() =
        FALSE_MATCHES.map { (shape, sources) ->
            dynamicTest(shape) {
                // Arrange
                val held = TestCatalogs.catalogOf(version = VERSION, GREETING_COMPONENT, UNSUPPORTED_COMPONENT)
                val files = writeSources(shape = shape, sources = sources)

                // Act
                val result = CompileTestSupport().compileFiles(
                    files = files,
                    options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
                )

                // Assert
                assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
            }
        }

    @TestFactory
    fun `given a real use of a component the plugin cannot create, when compiled, then the error names it`() =
        REAL_USES.map { (shape, source) ->
            dynamicTest(shape) {
                // Arrange
                val held = TestCatalogs.catalogOf(version = VERSION, GREETING_COMPONENT, UNSUPPORTED_COMPONENT)

                // Act
                val result = compile(source = source, held = held)

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(UNSUPPORTED_ERROR), message = result.messages)
                assertFalse(actual = result.messages.contains(NO_SUCH_PARAMETER), message = result.messages)
            }
        }

    @TestFactory
    fun `given a name that is not a use of a refused component, when compiled, then only Kotlin reports it`() =
        KOTLIN_ONLY_ERRORS.map { (shape, sources, kotlinError) ->
            dynamicTest(shape) {
                // Arrange
                val held = TestCatalogs.catalogOf(version = VERSION, GREETING_COMPONENT, UNSUPPORTED_COMPONENT)
                val files = writeSources(shape = shape, sources = sources)

                // Act
                val result = CompileTestSupport().compileFiles(
                    files = files,
                    options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
                )

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(kotlinError), message = result.messages)
                assertFalse(actual = result.messages.contains(UNSUPPORTED_ERROR), message = result.messages)
            }
        }

    @Test
    fun `given a stale list that names a compiled file, when compiled, then its bridge is still created`() {
        // Arrange
        val source = workDir.resolve("Compiled.kt").apply { writeText(text = callerOf(component = GREETING)) }
        val list = workDir.resolve("excluded.list").apply {
            writeText(text = SourceFileList.encode(paths = listOf(source.absolutePath)))
        }
        val options = producerOptions(catalog(catalog = PLAIN_CATALOG, bridgePackage = BRIDGE_PACKAGE)) +
            (CompilerPluginOptions.EXCLUDED_SOURCES to list.absolutePath)

        // Act
        val result = CompileTestSupport().compileFiles(files = listOf(source), options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = result))
    }

    @Test
    fun `given a real call of a component over the parameter limit, when compiled, then the error names it`() {
        // Arrange
        val held = TestCatalogs.catalogOf(version = VERSION, OVER_LIMIT_COMPONENT)

        // Act
        val result = compile(source = callerOf(component = OVER_LIMIT, arguments = ""), held = held)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expected = "The composable '$OVER_LIMIT' has $OVER_LIMIT_PARAMETERS parameters, " +
            "and a bridge can have at most $PARAMETER_LIMIT."
        assertTrue(actual = result.messages.contains(expected), message = result.messages)
    }

    @Test
    fun `given a name only in a source folder file the compiler is not given, when compiled, then it is created`() {
        // Arrange
        workDir.resolve("Elsewhere.kt").writeText(text = callerOf(component = CARD))
        val options = producerOptions(catalog(catalog = PLAIN_CATALOG, bridgePackage = BRIDGE_PACKAGE)) +
            sourceFolderOption(folder = workDir)

        // Act
        val result = CompileTestSupport().compileSource(
            fileName = CALLER_FILE,
            source = callerOf(component = GREETING),
            options = options,
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = setOf(GREETING, CARD), actual = createdFunctions(result = result))
    }

    @Test
    fun `given a public inline function calling a bridge, when compiled, then Kotlin refuses the internal call`() {
        // Arrange
        val held = PLAIN_CATALOG

        // Act
        val result = compile(source = PUBLIC_INLINE_CALLER, held = held)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(NON_PUBLIC_INLINE_CALL), message = result.messages)
    }

    @Test
    fun `given an internal inline function calling a bridge, when compiled, then it compiles`() {
        // Arrange
        val held = PLAIN_CATALOG

        // Act
        val result = compile(source = INTERNAL_INLINE_CALLER, held = held)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = result))
    }

    @Test
    fun `given main and test naming one bridge, when test compiles against main, then test calls its own class`() {
        // Arrange
        val main = compile(source = callerOf(component = GREETING), held = PLAIN_CATALOG, moduleName = MAIN_MODULE)

        // Act
        val test = compileTestAgainst(main = main)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = main.exitCode, message = main.messages)
        assertEquals(expected = ExitCode.OK, actual = test.exitCode, message = test.messages)
        assertEquals(expected = setOf(GREETING), actual = createdFunctions(result = test))
        assertFalse(actual = bridgeClassNames(main).intersect(bridgeClassNames(test)).isNotEmpty())
    }

    @Test
    fun `given main and test naming one bridge, when test compiles against main, then it calls its own class`() {
        // Arrange
        val main = compile(source = callerOf(component = GREETING), held = PLAIN_CATALOG, moduleName = MAIN_MODULE)

        // Act
        val test = compileTestAgainst(main = main)

        // Assert
        val testCaller = test.outputDirectory.resolve(TEST_CALLER_CLASS).readBytes().toString(Charsets.ISO_8859_1)
        val ownBridge = bridgeClassNames(test).single().removeSuffix(CLASS_FILE_SUFFIX)
        val mainBridge = bridgeClassNames(main).single().removeSuffix(CLASS_FILE_SUFFIX)
        val callsOwnBridge = testCaller.contains("$BRIDGE_FOLDER/$ownBridge")
        val callsMainBridge = testCaller.contains("$BRIDGE_FOLDER/$mainBridge")
        assertTrue(actual = callsOwnBridge, message = "TestScreen does not call $ownBridge")
        assertFalse(actual = callsMainBridge, message = "TestScreen calls $mainBridge")
    }

    private fun compileTestAgainst(main: JvmCompilationResult): JvmCompilationResult =
        CompileTestSupport().compileSource(
            fileName = TEST_CALLER_FILE,
            source = TEST_CALLER,
            options = producerOptions(catalog(catalog = PLAIN_CATALOG, bridgePackage = BRIDGE_PACKAGE)),
            classpath = listOf(main.outputDirectory),
            friendPaths = listOf(main.outputDirectory),
            moduleName = TEST_MODULE,
        )

    private fun compile(source: String, held: Catalog, moduleName: String? = null): JvmCompilationResult =
        CompileTestSupport().compileSource(
            fileName = CALLER_FILE,
            source = source,
            options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
            moduleName = moduleName,
        )

    private fun writeSources(shape: String, sources: List<String>): List<File> {
        val folder = workDir.resolve(shape.replace(' ', '-')).apply { mkdirs() }
        return sources.mapIndexed { index, source ->
            folder.resolve("Source$index.kt").apply { writeText(text = source) }
        }
    }

    private fun bridgeClassNames(result: JvmCompilationResult): Set<String> =
        result.outputDirectory.resolve(BRIDGE_PACKAGE.replace('.', '/')).listFiles().orEmpty()
            .mapTo(destination = HashSet()) { it.name }

    private fun createdFunctions(
        result: JvmCompilationResult,
        bridgePackage: String = BRIDGE_PACKAGE,
    ): Set<String> =
        result.outputDirectory.resolve(bridgePackage.replace('.', '/')).listFiles().orEmpty()
            .filter { it.extension == CLASS_EXTENSION }
            .flatMap { file ->
                result.classLoader.loadClass("$bridgePackage.${file.nameWithoutExtension}").declaredMethods.toList()
            }
            .map { it.name }
            .filterNot { it.contains('$') }
            .toSet()

    private companion object {
        const val VERSION = "1"
        const val GREETING = "Greeting"
        const val CARD = "Card"
        const val UNSUPPORTED = "Unsupported"
        const val OVER_LIMIT = "OverLimit"
        const val PARAMETER_LIMIT = 118
        const val OVER_LIMIT_PARAMETERS = PARAMETER_LIMIT + 1
        const val BRIDGE_PACKAGE = "app.bridge.named"
        const val SECOND_PACKAGE = "app.bridge.second"
        const val CALLER_FILE = "Caller.kt"
        const val TEST_CALLER_FILE = "TestCaller.kt"
        const val TEST_CALLER_CLASS = "app/screens/TestCallerKt.class"
        const val CLASS_FILE_SUFFIX = ".class"
        val BRIDGE_FOLDER = BRIDGE_PACKAGE.replace('.', '/')
        const val MAIN_MODULE = "server"
        const val TEST_MODULE = "server_test"
        const val CLASS_EXTENSION = "class"
        const val NO_SUCH_PARAMETER = "No parameter with name"
        const val AMBIGUITY = "Overload resolution ambiguity"
        const val AMBIGUITY_WORD = "ambiguity"
        const val TOO_MANY_ARGUMENTS = "Too many arguments"
        const val PRIVATE_IN_FILE = "it is private in file"
        const val UNRESOLVED_REFERENCE = "Unresolved reference"
        const val NON_PUBLIC_INLINE_CALL = "Public-API inline function cannot access non-public-API"
        const val UNSUPPORTED_ERROR = "The composable '$UNSUPPORTED' cannot get a bridge: its parameter 'count'"

        val GREETING_COMPONENT = ComponentDescriptor(name = GREETING, parameters = listOf(TestCatalogs.requiredTitle))
        val CARD_COMPONENT = ComponentDescriptor(name = CARD, parameters = listOf(TestCatalogs.requiredTitle))
        val UNSUPPORTED_COMPONENT = ComponentDescriptor(
            name = UNSUPPORTED,
            parameters = listOf(Parameter.Value(name = "count", kind = ValueKind.Whole, required = true)),
        )
        val OVER_LIMIT_COMPONENT = ComponentDescriptor(
            name = OVER_LIMIT,
            parameters = List(size = OVER_LIMIT_PARAMETERS) { index -> TestCatalogs.optionalText(name = "p$index") },
        )
        val UNCREATABLE_COMPONENTS = listOf(UNSUPPORTED_COMPONENT, OVER_LIMIT_COMPONENT)
        val PLAIN_CATALOG = TestCatalogs.catalogOf(version = VERSION, GREETING_COMPONENT, CARD_COMPONENT)
        val SECOND_CATALOG = TestCatalogs.catalogOf(version = "2", GREETING_COMPONENT, CARD_COMPONENT)

        fun callerOf(component: String, arguments: String = "title = \"text\""): String =
            // language=kotlin
            """
            package app.screens

            import androidx.compose.runtime.Composable
            import $BRIDGE_PACKAGE.$component

            @Composable
            fun Screen() {
                $component($arguments)
            }
            """.trimIndent()

        // language=kotlin
        val TWO_CATALOG_CALLER = """
            package app.screens

            import androidx.compose.runtime.Composable
            import $BRIDGE_PACKAGE.$GREETING
            import $SECOND_PACKAGE.$CARD

            @Composable
            fun Screen() {
                $GREETING(title = "first")
                $CARD(title = "second")
            }
        """.trimIndent()

        // language=kotlin
        val PUBLIC_INLINE_CALLER = """
            package app.screens

            import androidx.compose.runtime.Composable
            import $BRIDGE_PACKAGE.$GREETING

            @Composable
            inline fun Screen() {
                $GREETING(title = "text")
            }
        """.trimIndent()

        // language=kotlin
        val INTERNAL_INLINE_CALLER = """
            package app.screens

            import androidx.compose.runtime.Composable
            import $BRIDGE_PACKAGE.$GREETING

            @Composable
            internal inline fun Screen() {
                $GREETING(title = "text")
            }
        """.trimIndent()

        // language=kotlin
        val TEST_CALLER = """
            package app.screens

            import androidx.compose.runtime.Composable
            import $BRIDGE_PACKAGE.$GREETING

            @Composable
            fun TestScreen() {
                $GREETING(title = "from test")
            }
        """.trimIndent()

        val NAMING_SHAPES: List<Pair<String, String>> = listOf(
            // language=kotlin
            "aliased import" to """
                package app.screens

                import androidx.compose.runtime.Composable
                import $BRIDGE_PACKAGE.$GREETING as Hello

                @Composable
                fun Screen() {
                    Hello(title = "text")
                }
            """.trimIndent(),
            // language=kotlin
            "star import" to """
                package app.screens

                import androidx.compose.runtime.Composable
                import $BRIDGE_PACKAGE.*

                @Composable
                fun Screen() {
                    $GREETING(title = "text")
                }
            """.trimIndent(),
            // language=kotlin
            "fully qualified call" to """
                package app.screens

                import androidx.compose.runtime.Composable

                @Composable
                fun Screen() {
                    $BRIDGE_PACKAGE.$GREETING(title = "text")
                }
            """.trimIndent(),
            // language=kotlin
            "callable reference" to """
                package app.screens

                import $BRIDGE_PACKAGE.*

                val reference = ::$GREETING
            """.trimIndent(),
            // language=kotlin
            "backticked name" to """
                package app.screens

                import androidx.compose.runtime.Composable
                import $BRIDGE_PACKAGE.`$GREETING`

                @Composable
                fun Screen() {
                    `$GREETING`(title = "text")
                }
            """.trimIndent(),
        )

        // language=kotlin
        const val FIRST_USER_FUNCTION = "package app.one\n\nfun $UNSUPPORTED(): Int = 1\n"

        // language=kotlin
        const val SECOND_USER_FUNCTION = "package app.two\n\nfun $UNSUPPORTED(): Int = 2\n"

        // language=kotlin
        const val PRIVATE_FUNCTION = "package $BRIDGE_PACKAGE\n\nprivate fun $UNSUPPORTED(): Int = 1\n"

        val KOTLIN_ONLY_ERRORS: List<Triple<String, List<String>, String>> = listOf(
            Triple(
                "ambiguous call",
                listOf(
                    FIRST_USER_FUNCTION,
                    SECOND_USER_FUNCTION,
                    // language=kotlin
                    """
                    package app.screens

                    import app.one.*
                    import app.two.*
                    import $BRIDGE_PACKAGE.*

                    val value: Int = $UNSUPPORTED()
                    """.trimIndent(),
                ),
                AMBIGUITY,
            ),
            Triple(
                "ambiguous callable reference",
                listOf(
                    FIRST_USER_FUNCTION,
                    SECOND_USER_FUNCTION,
                    // language=kotlin
                    """
                    package app.screens

                    import app.one.*
                    import app.two.*
                    import $BRIDGE_PACKAGE.*

                    val reference = ::$UNSUPPORTED
                    """.trimIndent(),
                ),
                AMBIGUITY_WORD,
            ),
            Triple(
                "wrong arguments to a user function",
                listOf(
                    FIRST_USER_FUNCTION,
                    // language=kotlin
                    """
                    package app.screens

                    import app.one.*
                    import $BRIDGE_PACKAGE.*

                    val value: Int = $UNSUPPORTED(1)
                    """.trimIndent(),
                ),
                TOO_MANY_ARGUMENTS,
            ),
            Triple(
                "call of a private function in another file",
                listOf(
                    PRIVATE_FUNCTION,
                    // language=kotlin
                    """
                    package app.screens

                    import $BRIDGE_PACKAGE.*

                    val value: Int = $UNSUPPORTED()
                    """.trimIndent(),
                ),
                PRIVATE_IN_FILE,
            ),
            Triple(
                "import of a private function in another file",
                listOf(
                    PRIVATE_FUNCTION,
                    // language=kotlin
                    """
                    package app.screens

                    import $BRIDGE_PACKAGE.$UNSUPPORTED

                    val anchor: Int = 1
                    """.trimIndent(),
                ),
                PRIVATE_IN_FILE,
            ),
            Triple(
                "missing member of an object in the bridge package",
                listOf(
                    // language=kotlin
                    "package $BRIDGE_PACKAGE\n\nobject Holder\n",
                    // language=kotlin
                    """
                    package app.screens

                    import $BRIDGE_PACKAGE.*

                    val value = Holder.$UNSUPPORTED()
                    """.trimIndent(),
                ),
                UNRESOLVED_REFERENCE,
            ),
            Triple(
                "missing member of a value",
                listOf(
                    // language=kotlin
                    """
                    package app.screens

                    import $BRIDGE_PACKAGE.*

                    val value = "text".$UNSUPPORTED()
                    """.trimIndent(),
                ),
                UNRESOLVED_REFERENCE,
            ),
            Triple(
                "call in a file that does not import the bridge package",
                listOf(
                    // language=kotlin
                    "package app.screens\n\nval value = $UNSUPPORTED()\n",
                ),
                UNRESOLVED_REFERENCE,
            ),
            Triple(
                "import of a name nested under the component",
                listOf(
                    // language=kotlin
                    "package app.screens\n\nimport $BRIDGE_PACKAGE.$UNSUPPORTED.Inner\n\nval anchor: Int = 1\n",
                ),
                UNRESOLVED_REFERENCE,
            ),
        )

        val FALSE_MATCHES: List<Pair<String, List<String>>> = listOf(
            // language=kotlin
            "comment" to listOf("package app.screens\n\n// $UNSUPPORTED is only mentioned here\nval anchor: Int = 1\n"),
            // language=kotlin
            "string" to listOf("package app.screens\n\nval text: String = \"$UNSUPPORTED\"\n"),
            "unrelated local class" to listOf(
                // language=kotlin
                "package app.screens\n\nclass $UNSUPPORTED\n\nval local = $UNSUPPORTED()\n",
            ),
            // language=kotlin
            "class in the bridge package" to listOf("package $BRIDGE_PACKAGE\n\nclass $UNSUPPORTED\n"),
            // language=kotlin
            "function in the bridge package" to listOf("package $BRIDGE_PACKAGE\n\nfun $UNSUPPORTED(): Int = 1\n"),
            "user class imported from the bridge package" to listOf(
                // language=kotlin
                "package $BRIDGE_PACKAGE\n\nclass $UNSUPPORTED\n",
                // language=kotlin
                "package app.screens\n\nimport $BRIDGE_PACKAGE.$UNSUPPORTED\n\nval value = $UNSUPPORTED()\n",
            ),
            "star import of the bridge package" to listOf(
                // language=kotlin
                """
                package app.screens

                import $BRIDGE_PACKAGE.*

                // $UNSUPPORTED is only mentioned here
                val anchor: Int = 1
                """.trimIndent(),
            ),
            "user function through a second star import" to listOf(
                // language=kotlin
                "package app.other\n\nfun $UNSUPPORTED(): Int = 1\n",
                // language=kotlin
                """
                package app.screens

                import app.other.*
                import $BRIDGE_PACKAGE.*

                val value: Int = $UNSUPPORTED()
                """.trimIndent(),
            ),
        )

        val REAL_USES: List<Pair<String, String>> = listOf(
            "call" to callerOf(component = UNSUPPORTED, arguments = "count = 1"),
            // language=kotlin
            "callable reference" to """
                package app.screens

                import $BRIDGE_PACKAGE.$UNSUPPORTED

                val reference = ::$UNSUPPORTED
            """.trimIndent(),
            // language=kotlin
            "import alone" to """
                package app.screens

                import $BRIDGE_PACKAGE.$UNSUPPORTED

                val anchor: Int = 1
            """.trimIndent(),
            // language=kotlin
            "aliased import" to """
                package app.screens

                import $BRIDGE_PACKAGE.$UNSUPPORTED as Renamed

                val anchor: Int = 1
            """.trimIndent(),
            // language=kotlin
            "call through a star import" to """
                package app.screens

                import androidx.compose.runtime.Composable
                import $BRIDGE_PACKAGE.*

                @Composable
                fun Screen() {
                    $UNSUPPORTED(count = 1)
                }
            """.trimIndent(),
            // language=kotlin
            "callable reference through a star import" to """
                package app.screens

                import $BRIDGE_PACKAGE.*

                val reference = ::$UNSUPPORTED
            """.trimIndent(),
            // language=kotlin
            "fully qualified call" to """
                package app.screens

                import androidx.compose.runtime.Composable

                @Composable
                fun Screen() {
                    $BRIDGE_PACKAGE.$UNSUPPORTED(count = 1)
                }
            """.trimIndent(),
            // language=kotlin
            "call from the bridge package" to """
                package $BRIDGE_PACKAGE

                import androidx.compose.runtime.Composable

                @Composable
                fun Screen() {
                    $UNSUPPORTED(count = 1)
                }
            """.trimIndent(),
        )
    }
}
