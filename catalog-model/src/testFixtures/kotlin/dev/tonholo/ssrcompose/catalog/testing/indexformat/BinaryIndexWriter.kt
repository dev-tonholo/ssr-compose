package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

internal class BinaryIndexWriter(private val catalog: Catalog) {
    private val components = catalog.sortedComponents()
    private val choices = catalog.sortedChoices()
    private val strings = collectStrings()
    private val stringIds = strings.withIndex().associate { (index, text) -> text to index }
    private val bodyWriter = BinaryBodyWriter(idOf = stringIds::getValue)

    fun write(): ByteArray = bytesOf { output ->
        output.writeInt(BINARY_MAGIC)
        output.writeByte(BINARY_FORMAT_VERSION)
        output.writeInt(stringIds.getValue(catalog.version.value))
        writeStrings(output = output)
        writeChoices(output = output)
        writeComponents(output = output)
    }

    private fun collectStrings(): List<String> = buildSet {
        add(catalog.version.value)
        choices.forEach { choice ->
            add(choice.name)
            addAll(choice.entries)
        }
        components.forEach { component ->
            add(component.name)
            component.parameters.forEach { parameter ->
                add(parameter.name)
                parameter.kinds().forEach { kind -> addChoiceNames(kind = kind) }
            }
        }
    }.sorted()

    private fun MutableSet<String>.addChoiceNames(kind: ValueKind) {
        when (kind) {
            is ValueKind.Choice -> add(kind.listName)
            is ValueKind.ListOf -> addChoiceNames(kind = kind.element)
            else -> Unit
        }
    }

    private fun Parameter.kinds(): List<ValueKind> = when (this) {
        is Parameter.Value -> listOf(kind)
        is Parameter.Callback -> arguments
        is Parameter.Slot -> arguments
    }

    private fun writeStrings(output: DataOutputStream) {
        val encoded = strings.map { it.encodeToByteArray() }
        output.writeInt(encoded.size)
        encoded.runningFold(initial = 0) { offset, bytes -> offset + bytes.size }.forEach(output::writeInt)
        encoded.forEach(output::write)
    }

    private fun writeChoices(output: DataOutputStream) {
        output.writeInt(choices.size)
        choices.forEach { choice ->
            output.writeInt(stringIds.getValue(choice.name))
            output.writeInt(choice.entries.size)
            choice.entries.forEach { entry -> output.writeInt(stringIds.getValue(entry)) }
        }
    }

    private fun writeComponents(output: DataOutputStream) {
        val bodies = components.map { component -> bytesOf { bodyWriter.write(output = it, component = component) } }
        output.writeInt(components.size)
        val offsets = bodies.runningFold(initial = 0) { offset, body -> offset + body.size }
        components.forEachIndexed { index, component ->
            output.writeInt(stringIds.getValue(component.name))
            output.writeInt(offsets[index])
        }
        output.writeInt(offsets.last())
        bodies.forEach(output::write)
    }

    private fun bytesOf(write: (DataOutputStream) -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use(write)
        return bytes.toByteArray()
    }
}
