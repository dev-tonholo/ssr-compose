package dev.tonholo.ssrcompose.catalog.bundle.mapping

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFileKinds as Kinds
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ElementKindFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.KindFile
import dev.tonholo.ssrcompose.core.data.ValueKind

internal fun ValueKind.toKindFile(): KindFile = when (this) {
    ValueKind.Text -> KindFile(kind = Kinds.TEXT)
    ValueKind.Whole -> KindFile(kind = Kinds.WHOLE)
    ValueKind.Decimal -> KindFile(kind = Kinds.DECIMAL)
    ValueKind.Flag -> KindFile(kind = Kinds.FLAG)
    ValueKind.Bytes -> KindFile(kind = Kinds.BYTES)
    is ValueKind.Choice -> KindFile(kind = Kinds.CHOICE, choice = listName)
    is ValueKind.ListOf -> KindFile(kind = Kinds.LIST, of = element.toElementKindFile())
}

private fun ValueKind.toElementKindFile(): ElementKindFile =
    toKindFile().let { shape -> ElementKindFile(kind = shape.kind, choice = shape.choice) }

internal fun KindFile.toValueKind(): ValueKind = valueKind(kind = kind, choice = choice, element = of)

private fun ElementKindFile.toValueKind(): ValueKind {
    if (kind == Kinds.LIST) {
        throw CatalogFileException(message = "A list holds a text, whole, decimal, flag or choice")
    }
    return valueKind(kind = kind, choice = choice, element = null)
}

internal fun valueKind(kind: String, choice: String?, element: ElementKindFile?): ValueKind = when (kind) {
    Kinds.CHOICE -> choiceKind(choice = choice, element = element)
    Kinds.LIST -> listKind(choice = choice, element = element)
    else -> plainKind(kind = kind).also { requireNoDetail(kind = kind, choice = choice, element = element) }
}

private fun plainKind(kind: String): ValueKind = when (kind) {
    Kinds.TEXT -> ValueKind.Text
    Kinds.WHOLE -> ValueKind.Whole
    Kinds.DECIMAL -> ValueKind.Decimal
    Kinds.FLAG -> ValueKind.Flag
    Kinds.BYTES -> ValueKind.Bytes
    else -> throw CatalogFileException(message = "The kind is not known")
}

private fun requireNoDetail(kind: String, choice: String?, element: ElementKindFile?) {
    require(choice == null && element == null) { "The kind $kind takes no choice or element" }
}

private fun choiceKind(choice: String?, element: ElementKindFile?): ValueKind {
    require(element == null) { "The kind ${Kinds.CHOICE} takes no element" }
    return ValueKind.Choice(listName = requireNotNull(choice) { "The kind ${Kinds.CHOICE} needs a choice list" })
}

private fun listKind(choice: String?, element: ElementKindFile?): ValueKind {
    require(choice == null) { "The kind ${Kinds.LIST} takes no choice list" }
    val elementFile = requireNotNull(element) { "The kind ${Kinds.LIST} needs an element kind" }
    return ValueKind.ListOf(element = elementFile.toValueKind())
}
