package dev.tonholo.ssrcompose.compiler.testing

import dev.tonholo.ssrcompose.catalog.index.CatalogIndexWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.File

/** The catalogs the compile tests hold, and the index files written from them. */
internal object TestCatalogs {
    private const val PART_NAME = "test"

    val requiredTitle = Parameter.Value(name = "title", kind = ValueKind.Text, required = true)
    val requiredAction = Parameter.Callback(name = "onSelect", arguments = listOf(ValueKind.Text), required = true)

    val first: Catalog = catalogOf(
        version = "1",
        ComponentDescriptor(
            name = "Greeting",
            parameters = listOf(
                requiredTitle,
                requiredAction,
                optionalText(name = "subtitle"),
                Parameter.Slot(name = "content", required = false),
            ),
        ),
    )

    val second: Catalog = catalogOf(
        version = "2",
        ComponentDescriptor(
            name = "Greeting",
            parameters = listOf(requiredTitle, requiredAction, optionalText(name = "badge")),
        ),
    )

    val unplaceable: Catalog = catalogOf(
        version = "3",
        ComponentDescriptor(
            name = "PaymentSection",
            parameters = listOf(
                requiredTitle,
                Parameter.Slot(name = "methodPicker", arguments = listOf(ValueKind.Text), required = true),
            ),
        ),
        ComponentDescriptor(name = "Receipt", parameters = listOf(requiredTitle)),
    )

    val defaultedTitle: Catalog = catalogOf(
        version = "4",
        ComponentDescriptor(
            name = "Banner",
            parameters = listOf(Parameter.Value(name = "title", kind = ValueKind.Text, required = false)),
        ),
    )

    fun optionalText(name: String): Parameter.Value =
        Parameter.Value(name = name, kind = ValueKind.Text, required = false, nullable = true)

    fun catalogOf(
        version: String,
        vararg components: ComponentDescriptor,
        choices: List<ChoiceList> = emptyList(),
    ): Catalog =
        Catalog(
            version = CatalogVersion(value = version),
            parts = listOf(
                CatalogPart(
                    name = PartName(value = PART_NAME),
                    choices = choices,
                    components = components.toList(),
                ),
            ),
        )

    /** Writes the index of [catalog] to a temporary file and returns its path. */
    fun indexPath(catalog: Catalog): String =
        File.createTempFile("catalog", ".index").run {
            deleteOnExit()
            writeBytes(array = CatalogIndexWriter.write(catalog = catalog))
            absolutePath
        }
}
