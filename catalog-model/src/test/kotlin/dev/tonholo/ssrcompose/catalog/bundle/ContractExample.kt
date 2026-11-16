package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind

private val style = ValueKind.Choice(listName = "Style")

/**
 * The catalog of the example in `contracts/catalog-bundle.md`: version 3, from `:app` and `:feature:payment`.
 */
internal fun contractExampleCatalog(): Catalog = Catalog(
    version = CatalogVersion(value = "3"),
    parts = listOf(appPart(), featurePaymentPart()),
)

private fun appPart(): CatalogPart = CatalogPart(
    name = PartName(value = "app"),
    choices = listOf(ChoiceList(name = "Style", entries = listOf("bodyMedium", "titleLarge"))),
    components = listOf(
        ComponentDescriptor(
            name = "Greeting",
            parameters = listOf(
                Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                Parameter.Value(name = "style", kind = style, required = false),
            ),
        ),
    ),
)

private fun featurePaymentPart(): CatalogPart = CatalogPart(
    name = PartName(value = "feature-payment"),
    choices = emptyList(),
    components = listOf(
        ComponentDescriptor(
            name = "CameraScanner",
            parameters = listOf(
                Parameter.Slot(
                    name = "cancel",
                    accepts = SlotAccepts.Any,
                    arguments = listOf(ValueKind.Text),
                    required = true,
                ),
                Parameter.Callback(name = "onCancelClick", required = true),
                Parameter.Callback(name = "onScan", arguments = listOf(ValueKind.Bytes), required = true),
            ),
        ),
        ComponentDescriptor(
            name = "PaymentSection",
            parameters = listOf(
                Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                Parameter.Callback(name = "onPayClick", required = true),
                Parameter.Slot(name = "content", accepts = SlotAccepts.Consumer, required = false),
            ),
        ),
    ),
)
