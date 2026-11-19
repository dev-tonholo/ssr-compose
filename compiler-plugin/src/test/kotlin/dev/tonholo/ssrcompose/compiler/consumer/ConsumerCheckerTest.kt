package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class ConsumerCheckerTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support: CompileTestSupport = CompileTestSupport()
    private val plainSupport: CompileTestSupport = CompileTestSupport(registrars = emptyList())

    @Test
    fun `given a marked composable with value, choice and list parameters, when compiled, then each is described`() {
        // Arrange
        val expected = expectedAllValueKindsFragment()

        // Act
        val result = compile(fixtureName = "AllValueKindsCard")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given defaults and nullable parameters of each kind, when compiled, then required and nullable are set`() {
        // Arrange
        val expected = expectedDefaultsAndNullablesFragment()

        // Act
        val result = compile(fixtureName = "DefaultsAndNullablesCard")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given type aliases of each kind, when compiled, then the expanded kinds and nullability are described`() {
        // Arrange
        val expected = expectedTypealiasValueKindsFragment()

        // Act
        val result = compile(fixtureName = "TypealiasValueKindsCard")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given a marked composable with modifier parameters, when compiled, then the modifiers are left out`() {
        // Arrange
        val expected = CatalogFragment(
            source = "fixtures/consumer/ModifierCard.fixture.kt",
            components = listOf(
                ComponentDescriptor(
                    name = "ModifierCard",
                    parameters = listOf(
                        Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                    ),
                ),
            ),
        )

        // Act
        val result = support.compile(
            fixtures = listOf("ModifierStandIn", "ModifierCard"),
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given an enum compiled in another module, when a marked composable uses it, then its entries are described`() {
        // Arrange
        val externalModule = plainSupport.compile(fixtureName = "ExternalTheme")
        assertEquals(expected = ExitCode.OK, actual = externalModule.exitCode, message = externalModule.messages)
        val expected = CatalogFragment(
            source = "fixtures/consumer/ExternalEnumCard.fixture.kt",
            choices = listOf(
                ChoiceList(name = "ExternalTheme", entries = listOf("light", "dark")),
            ),
            components = listOf(
                ComponentDescriptor(
                    name = "ExternalEnumCard",
                    parameters = listOf(
                        Parameter.Value(
                            name = "theme",
                            kind = ValueKind.Choice(listName = "ExternalTheme"),
                            required = true,
                        ),
                        Parameter.Value(
                            name = "fallbackThemes",
                            kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "ExternalTheme")),
                            required = false,
                        ),
                    ),
                ),
            ),
        )

        // Act
        val result = support.compile(
            fixtureName = "ExternalEnumCard",
            options = consumerOptions(),
            classpath = listOf(externalModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given type aliases from another module, when a marked composable uses them, then kinds are described`() {
        // Arrange
        val externalModule = plainSupport.compile(fixtures = listOf("ExternalTheme", "ExternalAliases"))
        assertEquals(expected = ExitCode.OK, actual = externalModule.exitCode, message = externalModule.messages)
        val expected = CatalogFragment(
            source = "fixtures/consumer/ExternalAliasCard.fixture.kt",
            choices = listOf(ChoiceList(name = "ExternalTheme", entries = listOf("light", "dark"))),
            components = listOf(
                ComponentDescriptor(
                    name = "ExternalAliasCard",
                    parameters = listOf(
                        requiredValue(name = "label", kind = ValueKind.Text),
                        optionalValue(
                            name = "themes",
                            kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "ExternalTheme")),
                            nullable = true,
                        ),
                    ),
                ),
            ),
        )

        // Act
        val result = support.compile(
            fixtureName = "ExternalAliasCard",
            options = consumerOptions(),
            classpath = listOf(externalModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    private fun compile(fixtureName: String) =
        support.compile(
            fixtureName = fixtureName,
            options = consumerOptions(),
        )

    private fun consumerOptions(): List<Pair<String, String>> = FragmentsFolder(directory = fragmentsDir).options()

    private fun readFragments(): List<CatalogFragment> = FragmentsFolder(directory = fragmentsDir).read()

    private fun expectedAllValueKindsFragment(): CatalogFragment =
        CatalogFragment(
            source = "fixtures/consumer/AllValueKindsCard.fixture.kt",
            choices = listOf(
                ChoiceList(name = "Style", entries = listOf("bodyMedium", "titleLarge")),
                ChoiceList(name = "Tag", entries = listOf("featured", "onSale")),
            ),
            components = listOf(
                ComponentDescriptor(
                    name = "AllValueKindsCard",
                    parameters = listOf(
                        requiredValue(name = "title", kind = ValueKind.Text),
                        requiredValue(name = "count", kind = ValueKind.Whole),
                        requiredValue(name = "total", kind = ValueKind.Whole),
                        requiredValue(name = "rating", kind = ValueKind.Decimal),
                        requiredValue(name = "price", kind = ValueKind.Decimal),
                        requiredValue(name = "enabled", kind = ValueKind.Flag),
                        requiredValue(name = "style", kind = ValueKind.Choice(listName = "Style")),
                        requiredValue(name = "labels", kind = ValueKind.ListOf(element = ValueKind.Text)),
                        requiredValue(name = "counts", kind = ValueKind.ListOf(element = ValueKind.Whole)),
                        requiredValue(name = "totals", kind = ValueKind.ListOf(element = ValueKind.Whole)),
                        requiredValue(name = "ratings", kind = ValueKind.ListOf(element = ValueKind.Decimal)),
                        requiredValue(name = "prices", kind = ValueKind.ListOf(element = ValueKind.Decimal)),
                        requiredValue(name = "flags", kind = ValueKind.ListOf(element = ValueKind.Flag)),
                        requiredValue(
                            name = "tags",
                            kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "Tag")),
                        ),
                    ),
                ),
            ),
        )

    private fun expectedDefaultsAndNullablesFragment(): CatalogFragment =
        CatalogFragment(
            source = "fixtures/consumer/DefaultsAndNullablesCard.fixture.kt",
            choices = listOf(ChoiceList(name = "Priority", entries = listOf("low", "high"))),
            components = listOf(
                ComponentDescriptor(
                    name = "DefaultsAndNullablesCard",
                    parameters = listOf(
                        requiredValue(name = "requiredNullableText", kind = ValueKind.Text, nullable = true),
                        optionalValue(name = "optionalText", kind = ValueKind.Text),
                        optionalValue(name = "optionalNullableText", kind = ValueKind.Text, nullable = true),
                        optionalValue(name = "optionalInt", kind = ValueKind.Whole),
                        requiredValue(name = "nullableInt", kind = ValueKind.Whole, nullable = true),
                        optionalValue(name = "optionalLong", kind = ValueKind.Whole),
                        requiredValue(name = "nullableLong", kind = ValueKind.Whole, nullable = true),
                        optionalValue(name = "optionalFloat", kind = ValueKind.Decimal),
                        requiredValue(name = "nullableFloat", kind = ValueKind.Decimal, nullable = true),
                        optionalValue(name = "optionalDouble", kind = ValueKind.Decimal),
                        requiredValue(name = "nullableDouble", kind = ValueKind.Decimal, nullable = true),
                        optionalValue(name = "optionalFlag", kind = ValueKind.Flag),
                        requiredValue(name = "nullableFlag", kind = ValueKind.Flag, nullable = true),
                        optionalValue(name = "optionalChoice", kind = ValueKind.Choice(listName = "Priority")),
                        requiredValue(
                            name = "nullableChoice",
                            kind = ValueKind.Choice(listName = "Priority"),
                            nullable = true,
                        ),
                        optionalValue(name = "optionalList", kind = ValueKind.ListOf(element = ValueKind.Text)),
                        requiredValue(
                            name = "nullableList",
                            kind = ValueKind.ListOf(element = ValueKind.Text),
                            nullable = true,
                        ),
                        optionalValue(
                            name = "optionalNullableList",
                            kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "Priority")),
                            nullable = true,
                        ),
                    ),
                ),
            ),
        )

    private fun expectedTypealiasValueKindsFragment(): CatalogFragment =
        CatalogFragment(
            source = "fixtures/consumer/TypealiasValueKindsCard.fixture.kt",
            choices = listOf(ChoiceList(name = "BadgeTone", entries = listOf("calm", "urgent"))),
            components = listOf(
                ComponentDescriptor(
                    name = "TypealiasValueKindsCard",
                    parameters = listOf(
                        requiredValue(name = "title", kind = ValueKind.Text),
                        optionalValue(name = "subtitle", kind = ValueKind.Text, nullable = true),
                        optionalValue(name = "count", kind = ValueKind.Whole),
                        requiredValue(name = "total", kind = ValueKind.Whole),
                        requiredValue(name = "score", kind = ValueKind.Decimal),
                        requiredValue(name = "price", kind = ValueKind.Decimal),
                        requiredValue(name = "enabled", kind = ValueKind.Flag),
                        requiredValue(name = "tone", kind = ValueKind.Choice(listName = "BadgeTone")),
                        requiredValue(
                            name = "secondaryTone",
                            kind = ValueKind.Choice(listName = "BadgeTone"),
                            nullable = true,
                        ),
                        requiredValue(name = "labels", kind = ValueKind.ListOf(element = ValueKind.Text)),
                        optionalValue(
                            name = "tones",
                            kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "BadgeTone")),
                            nullable = true,
                        ),
                    ),
                ),
            ),
        )
}
