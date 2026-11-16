package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartReader
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartWriter
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.data.ValueKind

internal const val APP_PART_SUBJECT = "part app"

private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_CAP_MEBIBYTES = 16
private const val FILLER_DIGITS = 5
private const val FILLER_PARAMETERS = 8
private const val TAIL_PARAMETERS = 14
private const val MAX_NAME_EXTRA = 60

internal const val DEFAULT_CAP_BYTES = DEFAULT_CAP_MEBIBYTES * BYTES_PER_MEBIBYTE

internal fun readPart(text: String, sizeCap: SizeCap = SizeCap.DEFAULT): CatalogPart =
    CatalogPartReader.read(bytes = text.encodeToByteArray(), sizeCap = sizeCap, subject = APP_PART_SUBJECT)

internal fun partText(parameter: String): String =
    """{"format":1,"part":"app","choices":[{"name":"A","entries":["b"]}],""" +
        """"components":[{"name":"Card","parameters":[$parameter]}]}"""

internal fun partWith(
    name: String = "app",
    components: List<ComponentDescriptor> = emptyList(),
    choices: List<ChoiceList> = emptyList(),
): CatalogPart = CatalogPart(name = PartName(value = name), choices = choices, components = components)

internal fun partWithComponent(name: String, component: String): CatalogPart =
    partWith(name = name, components = listOf(ComponentDescriptor(name = component, parameters = emptyList())))

internal fun partAtCapPair(name: String = "app"): Pair<CatalogPart, CatalogPart> {
    val emptySize = writtenSize(name = name, components = emptyList())
    val oneSize = writtenSize(name = name, components = listOf(fillerComponent(index = 0)))
    val twoSize = writtenSize(name = name, components = listOf(fillerComponent(index = 0), fillerComponent(index = 1)))
    val perComponent = twoSize - oneSize
    val tailOverhead = writtenSize(name = name, components = listOf(tailComponent(extraChars = 0))) - emptySize
    val fillerCount = (DEFAULT_CAP_BYTES - emptySize - tailOverhead) / perComponent
    val remainder = DEFAULT_CAP_BYTES - (emptySize + fillerCount * perComponent + tailOverhead)
    val fillers = List(size = fillerCount) { index -> fillerComponent(index = index) }
    val atCap = partWith(name = name, components = fillers + tailComponent(extraChars = remainder))
    val overCap = partWith(name = name, components = fillers + tailComponent(extraChars = remainder + 1))
    return atCap to overCap
}

private fun writtenSize(name: String, components: List<ComponentDescriptor>): Int =
    CatalogPartWriter.write(part = partWith(name = name, components = components)).size

private fun fillerComponent(index: Int): ComponentDescriptor = ComponentDescriptor(
    name = "C" + index.toString().padStart(length = FILLER_DIGITS, padChar = '0'),
    parameters = List(size = FILLER_PARAMETERS) { param ->
        Parameter.Value(name = "p$param", kind = ValueKind.Text, required = true)
    },
)

private fun tailComponent(extraChars: Int): ComponentDescriptor {
    var remaining = extraChars
    val componentExtra = minOf(remaining, MAX_NAME_EXTRA)
    remaining -= componentExtra
    val parameters = List(size = TAIL_PARAMETERS) { param ->
        val paramExtra = minOf(remaining, MAX_NAME_EXTRA)
        remaining -= paramExtra
        Parameter.Value(
            name = "x" + param.toString().padStart(length = 2, padChar = '0') + "a".repeat(n = paramExtra),
            kind = ValueKind.Text,
            required = true,
        )
    }
    return ComponentDescriptor(name = "Z" + "a".repeat(n = componentExtra), parameters = parameters)
}
