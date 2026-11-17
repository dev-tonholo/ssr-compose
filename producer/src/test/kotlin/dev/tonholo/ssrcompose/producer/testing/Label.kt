package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.ConsumerComponent

internal const val LABEL_COMPONENT = "Label"
internal const val LABEL_TEXT_PARAMETER = "text"
internal const val LABEL_CONTENT_SLOT = "content"
private const val LABEL_CATALOG_VERSION = "1"

@Composable
internal fun Label(text: String, content: (@Composable () -> Unit)? = null) {
    ConsumerComponent(
        catalogVersion = LABEL_CATALOG_VERSION,
        name = LABEL_COMPONENT,
        valueNames = arrayOf(LABEL_TEXT_PARAMETER),
        values = arrayOf(text),
        actionNames = emptyArray(),
        actions = emptyArray(),
        slotNames = arrayOf(LABEL_CONTENT_SLOT),
        slots = arrayOf(content),
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
