package dev.tonholo.ssrcompose.catalog.bundle.mapping

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFileKinds as Kinds
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.KindFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ParameterFile
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.data.ValueKind

internal fun Parameter.toFile(): ParameterFile = when (this) {
    is Parameter.Value -> kind.toKindFile().let { shape ->
        ParameterFile(
            name = name,
            kind = shape.kind,
            choice = shape.choice,
            of = shape.of,
            nullable = nullable,
            required = required,
        )
    }
    is Parameter.Callback -> ParameterFile(
        name = name,
        kind = Kinds.CALLBACK,
        arguments = arguments.toKindFiles(),
        required = required,
    )
    is Parameter.Slot -> ParameterFile(
        name = name,
        kind = Kinds.SLOT,
        arguments = arguments.toKindFiles(),
        accepts = accepts.fileName,
        required = required,
    )
}

internal fun ParameterFile.toModel(): Parameter = when (kind) {
    Kinds.CALLBACK -> toCallback()
    Kinds.SLOT -> toSlot()
    else -> toValue()
}

private fun List<ValueKind>.toKindFiles(): List<KindFile>? = takeIf { it.isNotEmpty() }?.map(ValueKind::toKindFile)

private fun ParameterFile.toValue(): Parameter.Value {
    require(arguments == null && accepts == null) { "A value parameter takes no arguments or accepts" }
    return Parameter.Value(
        name = name,
        kind = valueKind(kind = kind, choice = choice, element = of),
        required = required,
        nullable = nullable,
    )
}

private fun ParameterFile.toCallback(): Parameter.Callback {
    requireNoValueFields(what = "A callback")
    require(accepts == null) { "A callback takes no accepts" }
    return Parameter.Callback(
        name = name,
        arguments = arguments.orEmpty().map(KindFile::toValueKind),
        required = required,
    )
}

private fun ParameterFile.toSlot(): Parameter.Slot {
    requireNoValueFields(what = "A slot")
    return Parameter.Slot(
        name = name,
        accepts = slotAccepts(fileName = requireNotNull(accepts) { "A slot needs accepts" }),
        arguments = arguments.orEmpty().map(KindFile::toValueKind),
        required = required,
    )
}

private fun ParameterFile.requireNoValueFields(what: String) {
    require(choice == null && of == null && !nullable) { "$what takes no choice, of or nullable" }
}

private fun slotAccepts(fileName: String): SlotAccepts =
    SlotAccepts.entries.firstOrNull { it.fileName == fileName }
        ?: throw CatalogFileException(message = "The slot accepts a value that is not known")
