package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedComponent
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex
import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.compiler.testing.ClassFileSizes
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BridgeLimitsTest {
    @Test
    fun `given a held catalog, when compiled, then every class file targets the JVM version of the project`() {
        // Arrange
        val components = listOf(SMALL_COMPONENT)

        // Act
        val result = compileHolding(components = components)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        val versions = ClassFileSizes.of(directory = result.outputDirectory).map { it.majorVersion }.toSet()
        assertEquals(expected = setOf(JVM_17_CLASS_FILE_VERSION), actual = versions)
    }

    @TestFactory
    fun `given the largest parameter count, when compiled, then the bridge loads within the method size ceiling`() =
        SHAPES.map { (shapeName, shape) ->
            dynamicTest(shapeName) {
                // Arrange
                val component = componentOf(name = COMPONENT, parameters = MAX_PARAMETERS, shape = shape)

                // Act
                val result = compileHolding(components = listOf(component))

                // Assert
                assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
                val bridge = loadEveryClass(result = result).single { type -> type.declares(function = COMPONENT) }
                val bridgeFile = "${bridge.name.replace('.', '/')}.$CLASS_EXTENSION"
                val largest = ClassFileSizes.of(directory = result.outputDirectory).single { it.path == bridgeFile }
                    .methods.maxOf { it.codeLength }
                assertTrue(actual = largest <= SAFE_METHOD_BYTES, message = "$shapeName method of $largest bytes")
            }
        }

    @Test
    fun `given one parameter more than the limit, when compiled, then the error names the component and the limit`() {
        // Arrange
        val component = componentOf(name = COMPONENT, parameters = MAX_PARAMETERS + 1, shape = OPTIONAL_TEXT)

        // Act
        val result = compileHolding(components = listOf(component), source = callerOf(component = COMPONENT))

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains("'$COMPONENT'"), message = result.messages)
        assertTrue(actual = result.messages.contains("at most $MAX_PARAMETERS"), message = result.messages)
    }

    @Test
    fun `given the largest count of optional wide parameters, when a hand-written twin is loaded, then it is valid`() {
        // Arrange
        val source = wideTwinSource(parameters = MAX_PARAMETERS)

        // Act
        val result = composeOnly().compileSource(fileName = WIDE_FILE, source = source)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        loadEveryClass(result = result)
    }

    @Test
    fun `given one optional wide parameter more, when a hand-written twin is loaded, then the JVM refuses the class`() {
        // Arrange
        val source = wideTwinSource(parameters = MAX_PARAMETERS + 1)

        // Act
        val result = composeOnly().compileSource(fileName = WIDE_FILE, source = source)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertFailsWith<ClassFormatError> { loadEveryClass(result = result) }
    }

    @Test
    fun `given a class at the function limit, when compiled, then every function is in that class`() {
        // Arrange
        val components = sameClassNames().take(MAX_FUNCTIONS_PER_CLASS).map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }

        // Act
        val result = compileHolding(components = components)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = MAX_FUNCTIONS_PER_CLASS, actual = createdFunctions(result = result).size)
    }

    @Tag(SCALE_TAG)
    @TestFactory
    fun `given the widest class at the function limit, when compiled, then it stays within every class file limit`() =
        WIDEST_SHAPES.flatMap { (shapeName, shape) ->
            listOf(false, true).map { sourceInformation ->
                dynamicTest("$shapeName, source information $sourceInformation") {
                    // Arrange
                    val components = sameClassNames().take(MAX_FUNCTIONS_PER_CLASS).map { name ->
                        widestComponent(name = name, shape = shape)
                    }

                    // Act
                    val result = compileHolding(
                        components = components,
                        composeOptions = listOf(SOURCE_INFORMATION_OPTION to "$sourceInformation"),
                    )

                    // Assert
                    assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
                    loadEveryClass(result = result)
                    assertEquals(expected = MAX_FUNCTIONS_PER_CLASS, actual = createdFunctions(result = result).size)
                    assertFullestClassFits(result = result, shapeName = "$shapeName/$sourceInformation")
                    assertMetadataReadsBack(result = result, component = components.first().name)
                }
            }
        }

    private fun assertFullestClassFits(result: JvmCompilationResult, shapeName: String) {
        val fullest = ClassFileSizes.of(directory = result.outputDirectory)
            .single { it.path.contains("/$SAME_CLASS_PREFIX") }
        val methods = fullest.methods
        println(
            "SCALE | fullest bridge class | $shapeName | constant pool ${fullest.constantPoolCount} | " +
                "methods ${fullest.methodCount} | fields ${fullest.fieldCount} | " +
                "interfaces ${fullest.interfaceCount} | longest utf8 ${fullest.longestUtf8Bytes} | " +
                "code ${methods.maxOf { it.codeLength }} | stack ${methods.maxOf { it.maxStack }} | " +
                "locals ${methods.maxOf { it.maxLocals }} | units ${methods.maxOf { it.parameterUnits }} | " +
                "array dimensions ${methods.maxOf { it.arrayDimensions }} | " +
                "longest metadata chunk ${longestMetadataChunkBytes(result = result)}",
        )
        assertTrue(actual = fullest.constantPoolCount <= MAX_CONSTANT_POOL, message = "${fullest.constantPoolCount}")
        assertEquals(expected = 0, actual = fullest.fieldCount)
        assertEquals(expected = 0, actual = fullest.interfaceCount)
        assertEquals(expected = 0, actual = methods.maxOf { it.arrayDimensions })
        assertTrue(actual = methods.maxOf { it.codeLength } <= SAFE_METHOD_BYTES)
        assertTrue(actual = methods.maxOf { it.parameterUnits } <= MAX_PARAMETER_UNITS)
    }

    private fun longestMetadataChunkBytes(result: JvmCompilationResult): Int =
        sameClass(result = result).getAnnotation(Metadata::class.java)
            .data1
            .maxOf { chunk -> chunk.sumOf(::modifiedUtf8Bytes) }

    private fun modifiedUtf8Bytes(character: Char): Int = when (character.code) {
        in ONE_BYTE_UTF8_CODES -> 1
        in TWO_BYTE_UTF8_CODES -> 2
        else -> THREE_BYTE_CHARACTER
    }

    private fun assertMetadataReadsBack(result: JvmCompilationResult, component: String) {
        // language=kotlin
        val caller = "package app.caller\n\nimport $BRIDGE_PACKAGE.$component\n\nval anchor: Int = 1\n"

        val readBack = composeOnly().compileSource(
            fileName = CALLER_FILE,
            source = caller,
            classpath = listOf(result.outputDirectory),
            friendPaths = listOf(result.outputDirectory),
        )

        assertEquals(expected = ExitCode.OK, actual = readBack.exitCode, message = readBack.messages)
    }

    @Test
    fun `given a bridge package at the byte limit, when compiled, then the bridge is created`() {
        // Arrange
        val bridgePackage = "é".repeat(n = MAX_PACKAGE_BYTES / 2) + "a"

        // Act
        val result = compileHolding(components = listOf(SMALL_COMPONENT), bridgePackage = bridgePackage)

        // Assert
        assertBridgeCreated(result = result, bridgePackage = bridgePackage)
    }

    @TestFactory
    fun `given a bridge package the JVM keeps for itself, when compiled, then the error names the version`() =
        listOf("java", "java.bridge").map { bridgePackage ->
            dynamicTest(bridgePackage) {
                // Arrange
                val components = listOf(SMALL_COMPONENT)

                // Act
                val result = compileHolding(components = components, bridgePackage = bridgePackage)

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(JVM_RESERVED), message = result.messages)
                assertTrue(actual = result.messages.contains("catalog version $VERSION"), message = result.messages)
            }
        }

    @TestFactory
    fun `given a Windows device name in a package segment, when compiled, then the error names the segment`() =
        mapOf(
            "app.con.bridge" to "con",
            "AUX.bridge" to "AUX",
            "app.Com1" to "Com1",
            "app.lpt9" to "lpt9",
            "nul" to "nul",
            "app.PRN" to "PRN",
        ).map { (bridgePackage, segment) ->
            dynamicTest(bridgePackage) {
                // Arrange
                val components = listOf(SMALL_COMPONENT)

                // Act
                val result = compileHolding(components = components, bridgePackage = bridgePackage)

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains("'$segment'"), message = result.messages)
                assertTrue(actual = result.messages.contains(WINDOWS_RESERVED), message = result.messages)
            }
        }

    @TestFactory
    fun `given a package that only resembles a reserved name, when compiled, then the bridge is created`() =
        listOf("javax.bridge", "app.bridge.java", "app.console", "app.com10").map { bridgePackage ->
            dynamicTest(bridgePackage) {
                // Arrange
                val components = listOf(SMALL_COMPONENT)

                // Act
                val result = compileHolding(components = components, bridgePackage = bridgePackage)

                // Assert
                assertBridgeCreated(result = result, bridgePackage = bridgePackage)
            }
        }

    @Test
    fun `given a bridge package one byte over the limit, when compiled, then the error states the limit`() {
        // Arrange
        val bridgePackage = "é".repeat(n = (MAX_PACKAGE_BYTES + 1) / 2)

        // Act
        val result = compileHolding(components = listOf(SMALL_COMPONENT), bridgePackage = bridgePackage)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains("at most $MAX_PACKAGE_BYTES"), message = result.messages)
    }

    @TestFactory
    fun `given a bridge package that is not made of identifiers, when compiled, then the error names the version`() =
        NOT_PACKAGE_NAMES.map { bridgePackage ->
            dynamicTest(bridgePackage) {
                // Arrange
                val components = listOf(SMALL_COMPONENT)

                // Act
                val result = compileHolding(components = components, bridgePackage = bridgePackage)

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(PACKAGE_NOT_VALID), message = result.messages)
                assertTrue(actual = result.messages.contains("catalog version $VERSION"), message = result.messages)
            }
        }

    @Test
    fun `given two catalogs in one bridge package, when compiled, then the error names both and the package`() {
        // Arrange
        val first = TestCatalogs.catalogOf(version = VERSION, SMALL_COMPONENT)
        val otherComponent = ComponentDescriptor(name = "Other", parameters = emptyList())
        val second = TestCatalogs.catalogOf(version = OTHER_VERSION, otherComponent)

        // Act
        val result = compileHoldingBoth(first = first, second = second)

        // Assert
        assertSharedPackageError(result = result)
    }

    @Test
    fun `given one component in two catalogs of one bridge package, when compiled, then the error is clear`() {
        // Arrange
        val first = TestCatalogs.catalogOf(version = VERSION, SMALL_COMPONENT)
        val second = TestCatalogs.catalogOf(version = OTHER_VERSION, SMALL_COMPONENT)

        // Act
        val result = compileHoldingBoth(first = first, second = second)

        // Assert
        assertSharedPackageError(result = result)
    }

    @Test
    fun `given bridge packages that differ only in case, when compiled, then the error names both spellings`() {
        // Arrange
        val first = TestCatalogs.catalogOf(version = VERSION, SMALL_COMPONENT)
        val second = TestCatalogs.catalogOf(version = OTHER_VERSION, SMALL_COMPONENT)

        // Act
        val result = compileHoldingBoth(first = first, second = second, secondPackage = OTHER_CASE_PACKAGE)

        // Assert
        assertSharedPackageError(result = result)
        assertTrue(actual = result.messages.contains("'$OTHER_CASE_PACKAGE'"), message = result.messages)
    }

    @Test
    fun `given bridge packages that differ only in the form of a sigma, when compiled, then the error names both`() {
        // Arrange
        val first = TestCatalogs.catalogOf(version = VERSION, SMALL_COMPONENT)
        val second = TestCatalogs.catalogOf(version = OTHER_VERSION, SMALL_COMPONENT)

        // Act
        val result = compileHoldingBoth(
            first = first,
            second = second,
            firstPackage = MIDDLE_SIGMA_PACKAGE,
            secondPackage = FINAL_SIGMA_PACKAGE,
        )

        // Assert
        assertSharedPackageError(result = result, bridgePackage = MIDDLE_SIGMA_PACKAGE)
        assertTrue(actual = result.messages.contains("'$FINAL_SIGMA_PACKAGE'"), message = result.messages)
    }

    @Test
    fun `given a hard keyword in the bridge package, when a caller imports it, then every class loads`() {
        // Arrange
        val components = listOf(SMALL_COMPONENT)

        // Act
        val result = compileHolding(
            components = components,
            bridgePackage = KEYWORD_PACKAGE,
            source = KEYWORD_PACKAGE_CALLER,
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        loadEveryClass(result = result)
    }

    private fun compileHoldingBoth(
        first: Catalog,
        second: Catalog,
        firstPackage: String = BRIDGE_PACKAGE,
        secondPackage: String = BRIDGE_PACKAGE,
    ): JvmCompilationResult =
        CompileTestSupport().compileSource(
            fileName = ANCHOR_FILE,
            source = ANCHOR_SOURCE,
            options = producerOptions(
                catalog(catalog = first, bridgePackage = firstPackage),
                catalog(catalog = second, bridgePackage = secondPackage),
            ),
        )

    private fun assertSharedPackageError(result: JvmCompilationResult, bridgePackage: String = BRIDGE_PACKAGE) {
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains("'$VERSION'"), message = result.messages)
        assertTrue(actual = result.messages.contains("'$OTHER_VERSION'"), message = result.messages)
        assertTrue(actual = result.messages.contains("'$bridgePackage'"), message = result.messages)
    }

    private fun assertBridgeCreated(result: JvmCompilationResult, bridgePackage: String) {
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        val bridges = loadEveryClass(result = result).filter { type -> type.declares(function = SMALL_COMPONENT.name) }
        assertEquals(expected = listOf(bridgePackage), actual = bridges.map { it.packageName })
    }

    private fun Class<*>.declares(function: String): Boolean = declaredMethods.any { it.name == function }

    @TestFactory
    fun `given a bridge package at the class file text limit, when compiled, then the error is clear`() =
        listOf(UTF8_LIMIT_BYTES, UTF8_LIMIT_BYTES + 1).map { bytes ->
            dynamicTest("$bytes bytes") {
                // Arrange
                val bridgePackage = multibyteText(bytes = bytes)

                // Act
                val result = compileHolding(components = listOf(SMALL_COMPONENT), bridgePackage = bridgePackage)

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains("at most $MAX_PACKAGE_BYTES"), message = result.messages)
            }
        }

    @TestFactory
    fun `given a catalog version at the class file text limit, when compiled, then the error is clear`() =
        listOf(UTF8_LIMIT_BYTES, UTF8_LIMIT_BYTES + 1).map { bytes ->
            dynamicTest("$bytes bytes") {
                // Arrange
                val held = TestCatalogs.catalogOf(version = VERSION, SMALL_COMPONENT)
                val option = catalog(
                    catalog = held,
                    version = multibyteText(bytes = bytes),
                    bridgePackage = BRIDGE_PACKAGE,
                )

                // Act
                val result = CompileTestSupport().compileSource(
                    fileName = ANCHOR_FILE,
                    source = ANCHOR_SOURCE,
                    options = producerOptions(option),
                )

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(VERSION_NOT_VALID), message = result.messages)
            }
        }

    @TestFactory
    fun `given a component name at the class file text limit, when compiled, then the error is clear`() =
        listOf(UTF8_LIMIT_BYTES, UTF8_LIMIT_BYTES + 1).map { bytes ->
            dynamicTest("$bytes bytes") {
                // Arrange
                val index = File.createTempFile("long-name", ".index").apply {
                    deleteOnExit()
                    writeBytes(
                        array = handEncodedIndexNamingModelRefusedComponent(component = multibyteText(bytes = bytes)),
                    )
                }
                val option = CatalogOptionValue(
                    version = VERSION,
                    indexPath = index.absolutePath,
                    bridgePackage = BRIDGE_PACKAGE,
                )

                // Act
                val result = CompileTestSupport().compileSource(
                    fileName = ANCHOR_FILE,
                    source = TestCatalogs.sourceNaming(names = listOf(multibyteText(bytes = bytes))),
                    options = producerOptions(option),
                )

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains(NAME_TOO_LONG), message = result.messages)
            }
        }

    private fun multibyteText(bytes: Int): String =
        "語".repeat(n = bytes / THREE_BYTE_CHARACTER) + "a".repeat(n = bytes % THREE_BYTE_CHARACTER)

    private fun handEncodedIndexNamingModelRefusedComponent(component: String): ByteArray =
        HandEncodedIndex.encode(
            strings = listOf(VERSION, component).map { it.encodeToByteArray() },
            components = listOf(HandEncodedComponent(nameId = LONG_COMPONENT_NAME_ID, bodyOffset = 0)),
            bodies = HandEncodedIndex.EMPTY_BODY,
        )

    private fun createdFunctions(result: JvmCompilationResult): Set<String> =
        sameClass(result = result).declaredMethods
            .map { it.name }
            .filter { it.startsWith(SAME_CLASS_NAME_PREFIX) && '$' !in it }
            .toSet()

    @Test
    fun `given one function more than the class limit, when compiled, then the error names the class and the limit`() {
        // Arrange
        val components = sameClassNames().take(MAX_FUNCTIONS_PER_CLASS + 1).map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }

        // Act
        val result = compileHolding(
            components = components,
            source = callerOf(component = components.first().name) + commentNaming(components = components),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(SAME_CLASS_PREFIX), message = result.messages)
        assertTrue(actual = result.messages.contains("at most $MAX_FUNCTIONS_PER_CLASS"), message = result.messages)
    }

    @Test
    fun `given a class over the limit only in comments, when compiled, then the build passes`() {
        // Arrange
        val components = sameClassNames().take(MAX_FUNCTIONS_PER_CLASS + 1).map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }

        // Act
        val result = compileHolding(components = components)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given a catalog over the class limit, when code names only the limit, then each named bridge exists`() {
        // Arrange
        val components = sameClassNames().take(MAX_FUNCTIONS_PER_CLASS + 1).map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }
        val named = components.take(MAX_FUNCTIONS_PER_CLASS)

        // Act
        val result = compileHolding(
            components = components,
            source = callerOf(component = named.first().name, arguments = TITLE_ARGUMENT) +
                commentNaming(components = named),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = MAX_FUNCTIONS_PER_CLASS, actual = createdFunctions(result = result).size)
    }

    private fun compileHolding(
        components: List<ComponentDescriptor>,
        bridgePackage: String = BRIDGE_PACKAGE,
        composeOptions: List<Pair<String, String>> = emptyList(),
        source: String = TestCatalogs.sourceNaming(names = components.map { it.name }),
    ): JvmCompilationResult {
        val held = TestCatalogs.catalogOf(version = VERSION, components = components.toTypedArray())
        return CompileTestSupport().compileSource(
            fileName = ANCHOR_FILE,
            source = source,
            options = producerOptions(catalog(catalog = held, bridgePackage = bridgePackage)),
            composeOptions = composeOptions,
        )
    }

    private fun sameClass(result: JvmCompilationResult): Class<*> {
        val classFile = result.outputDirectory.resolve(BRIDGE_PACKAGE.replace('.', '/')).listFiles().orEmpty()
            .single { it.name.startsWith(SAME_CLASS_PREFIX) }
        return result.classLoader.loadClass("$BRIDGE_PACKAGE.${classFile.nameWithoutExtension}")
    }

    private fun commentNaming(components: List<ComponentDescriptor>): String =
        components.joinToString(separator = " ", prefix = "\n// ") { it.name }

    private fun callerOf(component: String, arguments: String = ""): String =
        // language=kotlin
        """
        package app.caller

        import androidx.compose.runtime.Composable
        import $BRIDGE_PACKAGE.$component

        @Composable
        fun Screen() {
            $component($arguments)
        }
        """.trimIndent()

    private fun composeOnly() = CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))

    private fun loadEveryClass(result: JvmCompilationResult): List<Class<*>> {
        val root = result.outputDirectory
        return root.walkTopDown().filter { it.extension == CLASS_EXTENSION }.map { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath.removeSuffix(".$CLASS_EXTENSION")
            Class.forName(path.replace('/', '.'), true, result.classLoader)
        }.toList()
    }

    private fun componentOf(name: String, parameters: Int, shape: (String) -> Parameter) =
        ComponentDescriptor(name = name, parameters = List(size = parameters) { index -> shape("p$index") })

    private fun widestComponent(name: String, shape: (String) -> Parameter) = ComponentDescriptor(
        name = name,
        parameters = List(size = MAX_PARAMETERS) { index ->
            shape("${name}p${index}_".padEnd(length = LONGEST_NAME, padChar = 'x'))
        },
    )

    private fun sameClassNames(): List<String> {
        val resource = checkNotNull(javaClass.classLoader.getResourceAsStream(SAME_CLASS_NAMES)) { SAME_CLASS_NAMES }
        return resource.bufferedReader().use { reader -> reader.readLines().filter { it.isNotBlank() } }
    }

    private fun wideTwinSource(parameters: Int): String {
        // language=kotlin
        val declared = List(size = parameters) { index -> "p$index: Long = 0L" }.joinToString()

        // language=kotlin
        val used = List(size = parameters) { index -> "Sink(p$index)" }.joinToString(separator = "\n")

        // language=kotlin
        return """
            package app.twin

            import androidx.compose.runtime.Composable

            @Composable
            fun Sink(value: Long) {
            }

            @Composable
            fun Wide($declared) {
                $used
            }
        """.trimIndent()
    }

    private companion object {
        const val JVM_17_CLASS_FILE_VERSION = 61
        const val MAX_PARAMETERS = 118
        const val MAX_FUNCTIONS_PER_CLASS = 150
        const val SAFE_METHOD_BYTES = 32_767
        const val MAX_CONSTANT_POOL = 65_535
        const val MAX_PARAMETER_UNITS = 255
        const val MAX_PACKAGE_BYTES = 255
        const val UTF8_LIMIT_BYTES = 65_535
        const val THREE_BYTE_CHARACTER = 3
        const val VERSION_NOT_VALID = "The catalog version option is not valid"
        const val PACKAGE_NOT_VALID = "is not made of Kotlin identifiers separated by dots"
        const val OTHER_VERSION = "2"
        const val NAME_TOO_LONG = "must be a Kotlin identifier of at most 64 characters"
        const val JVM_RESERVED = "which the JVM keeps for itself"
        const val WINDOWS_RESERVED = "which Windows reserves for a device"
        const val LONG_COMPONENT_NAME_ID = 1
        const val LONGEST_NAME = 64
        const val COMPONENT = "Largest"
        const val VERSION = "1"
        const val BRIDGE_PACKAGE = "app.bridge.limits"
        const val OTHER_CASE_PACKAGE = "app.Bridge.limits"
        const val MIDDLE_SIGMA_PACKAGE = "app.σ"
        const val FINAL_SIGMA_PACKAGE = "app.ς"
        const val KEYWORD_PACKAGE = "in.acme.bridge"
        const val SAME_CLASS_PREFIX = "Bridge93_"
        const val TITLE_ARGUMENT = "title = \"text\""
        const val SAME_CLASS_NAME_PREFIX = "Same"
        const val SAME_CLASS_NAMES = "bridge-limits/same-class-component-names.txt"
        const val ANCHOR_FILE = "Anchor.kt"
        const val WIDE_FILE = "Wide.kt"
        const val CALLER_FILE = "Caller.kt"
        const val SOURCE_INFORMATION_OPTION = "sourceInformation"
        const val CLASS_EXTENSION = "class"

        // language=kotlin
        const val ANCHOR_SOURCE = "package app\n\nval anchor: Int = 1\n"

        // language=kotlin
        const val KEYWORD_PACKAGE_CALLER = """
            package app.caller

            import androidx.compose.runtime.Composable
            import `in`.acme.bridge.Small

            @Composable
            fun Screen() {
                Small(title = "Welcome")
            }
        """

        val NOT_PACKAGE_NAMES = listOf(
            "app..bridge",
            "app.bridge.",
            ".app",
            "a;b",
            "a[b",
            "a<b",
            "app._.bridge",
            "app.1a",
        )
        val SMALL_COMPONENT = ComponentDescriptor(name = "Small", parameters = listOf(TestCatalogs.requiredTitle))
        val OPTIONAL_TEXT: (String) -> Parameter = { name -> TestCatalogs.optionalText(name = name) }
        val SHAPES: Map<String, (String) -> Parameter> = mapOf(
            "optional text" to OPTIONAL_TEXT,
            "required text" to { name -> Parameter.Value(name = name, kind = ValueKind.Text, required = true) },
            "required action" to { name ->
                Parameter.Callback(name = name, arguments = listOf(ValueKind.Text), required = true)
            },
            "optional slot" to { name -> Parameter.Slot(name = name, required = false) },
            "required slot" to { name -> Parameter.Slot(name = name, required = true) },
        )
        val MIXED_SHAPES: List<(String) -> Parameter> = SHAPES.values.take(n = 4)
        val WIDEST_SHAPES: Map<String, (String) -> Parameter> = SHAPES + mapOf(
            "required slot with a text argument" to { name ->
                Parameter.Slot(name = name, arguments = listOf(ValueKind.Text), required = true)
            },
            "mixed" to { name -> MIXED_SHAPES[name.hashCode().mod(MIXED_SHAPES.size)](name) },
        )
        val ONE_BYTE_UTF8_CODES = 0x01..0x7F
        val TWO_BYTE_UTF8_CODES = (0x00..0x00) + (0x80..0x7FF)
    }
}
