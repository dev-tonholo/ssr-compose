package dev.tonholo.ssrcompose.catalog.index

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.io.requireWithin
import dev.tonholo.ssrcompose.catalog.index.binary.BINARY_FORMAT_VERSION
import dev.tonholo.ssrcompose.catalog.index.binary.BINARY_MAGIC
import dev.tonholo.ssrcompose.catalog.index.binary.ComponentBodyWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Writes the index the compiler plugin reads. The same catalog always gives the same bytes, whatever the order of its
 * parts, and the index holds no path and no time.
 */
public object CatalogIndexWriter {
    /**
     * Returns the bytes of the index of [catalog].
     *
     * @param sizeCap the most bytes the index may have. The default is the cap the compiler plugin reads with.
     * @throws CatalogFileException when the index is over [sizeCap].
     */
    public fun write(catalog: Catalog, sizeCap: SizeCap = CatalogIndexReader.DEFAULT_SIZE_CAP): ByteArray =
        IndexBytes(catalog = catalog).write().requireWithin(sizeCap = sizeCap, subject = INDEX_SUBJECT)
}

private class IndexBytes(private val catalog: Catalog) {
    private val components: List<ComponentDescriptor> = catalog.parts.flatMap { it.components }.sortedBy { it.name }
    private val choices: List<ChoiceList> = catalog.parts.flatMap { it.choices }.distinct().sortedBy { it.name }
    private val strings: List<String> = collectStrings()
    private val stringIds: Map<String, Int> = strings.withIndex().associate { (index, text) -> text to index }
    private val bodyWriter = ComponentBodyWriter(idOf = stringIds::getValue)

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
