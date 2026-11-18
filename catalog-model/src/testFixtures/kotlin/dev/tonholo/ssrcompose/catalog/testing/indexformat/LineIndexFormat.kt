package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.data.ValueKind

private const val HEADER = "ssr-catalog-index-lines 1"
private const val SEPARATOR = "\t"
private const val LIST_SEPARATOR = ","
private const val VERSION_WORD = "version"
private const val CHOICE_WORD = "choice"
private const val COMPONENT_WORD = "component"
private const val VALUE_WORD = "value"
private const val CALLBACK_WORD = "callback"
private const val SLOT_WORD = "slot"
private const val CHOICE_PREFIX = "choice:"
private const val LIST_PREFIX = "list:"
private const val FIRST_FIELD = 1
private const val SECOND_FIELD = 2
private const val THIRD_FIELD = 3
private const val FOURTH_FIELD = 4
private const val VERSION_FIELDS = 2
private const val CHOICE_FIELDS = 3
private const val COMPONENT_FIELDS = 2
private const val VALUE_FIELDS = 5
private const val CALLBACK_FIELDS = 4
private const val SLOT_FIELDS = 5
private val RECORD_FIELDS = mapOf(
    VERSION_WORD to VERSION_FIELDS,
    CHOICE_WORD to CHOICE_FIELDS,
    COMPONENT_WORD to COMPONENT_FIELDS,
    VALUE_WORD to VALUE_FIELDS,
    CALLBACK_WORD to CALLBACK_FIELDS,
    SLOT_WORD to SLOT_FIELDS,
)
private val PLAIN_KINDS = mapOf(
    "text" to ValueKind.Text,
    "whole" to ValueKind.Whole,
    "decimal" to ValueKind.Decimal,
    "flag" to ValueKind.Flag,
    "bytes" to ValueKind.Bytes,
)
private val PLAIN_WORDS = PLAIN_KINDS.entries.associate { (word, kind) -> kind to word }

/** A text index, one record per line. Opening it parses every line. */
object LineIndexFormat : CandidateIndexFormat {
    override val name: String = "lines"

    override fun write(catalog: Catalog): ByteArray = buildString {
        appendLine(HEADER)
        appendLine(listOf(VERSION_WORD, catalog.version.value).joinToString(SEPARATOR))
        catalog.sortedChoices().forEach { choice ->
            val entries = choice.entries.joinToString(LIST_SEPARATOR)
            appendLine(listOf(CHOICE_WORD, choice.name, entries).joinToString(SEPARATOR))
        }
        catalog.sortedComponents().forEach { component ->
            appendLine(listOf(COMPONENT_WORD, component.name).joinToString(SEPARATOR))
            component.parameters.forEach { parameter -> appendLine(parameter.toLine()) }
        }
    }.encodeToByteArray()

    override fun open(bytes: ByteArray): CandidateIndex {
        val lines = bytes.decodeToString().lineSequence().filter { it.isNotEmpty() }.iterator()
        require(lines.hasNext() && lines.next() == HEADER) { "This file is not a line index" }
        return LineIndexParser().parse(lines = lines)
    }
}

private class LineIndex(
    override val version: String,
    override val choices: List<ChoiceList>,
    private val components: Map<String, ComponentDescriptor>,
) : CandidateIndex {
    override val componentNames: List<String> = components.keys.toList()

    override fun component(name: String): ComponentDescriptor? = components[name]
}

private class LineIndexParser {
    private var version: String? = null
    private val choices = mutableListOf<ChoiceList>()
    private val components = LinkedHashMap<String, ComponentDescriptor>()
    private var openName: String? = null
    private val openParameters = mutableListOf<Parameter>()

    fun parse(lines: Iterator<String>): CandidateIndex {
        lines.forEach { line -> read(fields = line.split(SEPARATOR)) }
        closeComponent()
        val readVersion = requireNotNull(version) { "A line index must have a version" }
        return LineIndex(version = readVersion, choices = choices, components = components)
    }

    private fun read(fields: List<String>) {
        val word = fields.first()
        require(fields.size == RECORD_FIELDS[word]) { "Malformed line in a line index: $word" }
        when (word) {
            VERSION_WORD -> version = fields[FIRST_FIELD].also { require(it.isNotEmpty()) { "The version is empty" } }
            CHOICE_WORD -> choices += ChoiceList(
                name = fields[FIRST_FIELD],
                entries = fields[SECOND_FIELD].split(LIST_SEPARATOR),
            )

            COMPONENT_WORD -> {
                closeComponent()
                openName = fields[FIRST_FIELD]
            }

            else -> {
                require(openName != null) { "A parameter line comes before any component" }
                openParameters += parameterOf(fields = fields)
            }
        }
    }

    private fun closeComponent() {
        val name = openName ?: return
        components[name] = ComponentDescriptor(name = name, parameters = openParameters.toList())
        openParameters.clear()
    }
}

private fun parameterOf(fields: List<String>): Parameter {
    val name = fields[FIRST_FIELD]
    return when (val word = fields.first()) {
        VALUE_WORD -> Parameter.Value(
            name = name,
            kind = kindOf(word = fields[SECOND_FIELD]),
            required = fields[THIRD_FIELD].toBooleanStrict(),
            nullable = fields[FOURTH_FIELD].toBooleanStrict(),
        )

        CALLBACK_WORD -> Parameter.Callback(
            name = name,
            arguments = kindsOf(words = fields[SECOND_FIELD]),
            required = fields[THIRD_FIELD].toBooleanStrict(),
        )

        SLOT_WORD -> Parameter.Slot(
            name = name,
            accepts = SlotAccepts.valueOf(fields[SECOND_FIELD]),
            arguments = kindsOf(words = fields[THIRD_FIELD]),
            required = fields[FOURTH_FIELD].toBooleanStrict(),
        )

        else -> throw IllegalArgumentException("Unknown line in a line index: $word")
    }
}

private fun Parameter.toLine(): String {
    val fields = when (this) {
        is Parameter.Value -> listOf(VALUE_WORD, name, wordOf(kind = kind), "$required", "$nullable")
        is Parameter.Callback -> listOf(CALLBACK_WORD, name, wordsOf(kinds = arguments), "$required")
        is Parameter.Slot -> listOf(SLOT_WORD, name, accepts.name, wordsOf(kinds = arguments), "$required")
    }
    return fields.joinToString(SEPARATOR)
}

private fun wordsOf(kinds: List<ValueKind>): String = kinds.joinToString(LIST_SEPARATOR, transform = ::wordOf)

private fun wordOf(kind: ValueKind): String = when (kind) {
    is ValueKind.Choice -> "$CHOICE_PREFIX${kind.listName}"
    is ValueKind.ListOf -> "$LIST_PREFIX${wordOf(kind = kind.element)}"
    else -> PLAIN_WORDS.getValue(kind)
}

private fun kindsOf(words: String): List<ValueKind> =
    if (words.isEmpty()) emptyList() else words.split(LIST_SEPARATOR).map(::kindOf)

private fun kindOf(word: String): ValueKind = when {
    word.startsWith(CHOICE_PREFIX) -> ValueKind.Choice(listName = word.removePrefix(CHOICE_PREFIX))
    word.startsWith(LIST_PREFIX) -> ValueKind.ListOf(element = kindOf(word = word.removePrefix(LIST_PREFIX)))
    else -> requireNotNull(PLAIN_KINDS[word]) { "Unknown kind in a line index: $word" }
}
