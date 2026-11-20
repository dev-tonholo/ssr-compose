package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.ConsumerComponent

internal const val LABEL_COMPONENT = "Label"
internal const val LABEL_TEXT_PARAMETER = "text"
internal const val LABEL_CONTENT_SLOT = "content"
private val LABEL_CATALOG_VERSION = CatalogVersion(value = "1")

@Composable
internal fun Label(text: String, content: (@Composable () -> Unit)? = null) {
    ConsumerComponent(
        catalogVersion = LABEL_CATALOG_VERSION,
        name = LABEL_COMPONENT,
        values = mapOf(LABEL_TEXT_PARAMETER to DataValue.Text(value = text)),
        slots = mapOf(LABEL_CONTENT_SLOT to content),
    )
}

internal fun expectedLabel(text: String, children: List<PlacedComponent> = emptyList()): PlacedComponent =
    PlacedComponent(
        name = LABEL_COMPONENT,
        values = mapOf(LABEL_TEXT_PARAMETER to DataValue.Text(text)),
        slots = if (children.isEmpty()) emptyMap() else mapOf(LABEL_CONTENT_SLOT to children),
    )

internal fun expectedScreen(vararg components: PlacedComponent): ScreenContent =
    ScreenContent(components = components.toList())
