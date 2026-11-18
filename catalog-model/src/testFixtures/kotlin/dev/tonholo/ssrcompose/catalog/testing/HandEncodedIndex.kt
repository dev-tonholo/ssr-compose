package dev.tonholo.ssrcompose.catalog.testing

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** One entry of the component table of a hand-encoded index. */
data class HandEncodedComponent(val nameId: Int, val bodyOffset: Int)

/** One entry of the choice table of a hand-encoded index. */
data class HandEncodedChoice(val nameId: Int, val entryIds: List<Int>)

/**
 * Encodes index files byte by byte, so tests can build the damaged shapes the real writer never produces. The format
 * values live here on purpose, so these files check the production reader independently.
 */
object HandEncodedIndex {
    private const val MAGIC = 0x53535249
    private const val FORMAT_VERSION = 1
    private const val VERSION_STRING_ID = 0
    private const val TAG_VALUE = 0
    private const val TAG_CALLBACK = 1
    private const val TAG_SLOT = 2
    private const val ACCEPTS_ANY = 0
    private const val KIND_TEXT = 0
    private const val KIND_CHOICE = 5
    private const val KIND_LIST = 6

    const val FLAG_REQUIRED: Int = 1
    const val FLAG_NULLABLE: Int = 2

    /** The body of a component with no parameter. */
    val EMPTY_BODY: ByteArray = bodyOf()

    /** The kind of a text. */
    val TEXT_KIND: ByteArray = bytesOf { writeByte(KIND_TEXT) }

    /** Returns an index of [strings], [choices] and [components] with [bodies]. String 0 is the catalog version. */
    fun encode(
        strings: List<ByteArray>,
        components: List<HandEncodedComponent>,
        bodies: ByteArray,
        choices: List<HandEncodedChoice> = emptyList(),
    ): ByteArray = bytesOf {
        writeInt(MAGIC)
        writeByte(FORMAT_VERSION)
        writeInt(VERSION_STRING_ID)
        writeStrings(strings = strings)
        writeInt(choices.size)
        choices.forEach { choice -> writeChoice(choice = choice) }
        writeInt(components.size)
        components.forEach { component ->
            writeInt(component.nameId)
            writeInt(component.bodyOffset)
        }
        writeInt(bodies.size)
        write(bodies)
    }

    /** Returns the body of a component with [parameters]. */
    fun bodyOf(vararg parameters: ByteArray): ByteArray = bytesOf {
        writeInt(parameters.size)
        parameters.forEach { parameter -> write(parameter) }
    }

    /** Returns a value parameter named by string [nameId]. */
    fun value(nameId: Int, flags: Int = FLAG_REQUIRED, kind: ByteArray = TEXT_KIND): ByteArray = bytesOf {
        writeHeader(tag = TAG_VALUE, nameId = nameId, flags = flags)
        write(kind)
    }

    /** Returns a callback parameter named by string [nameId] that carries [kinds]. */
    fun callback(nameId: Int, kinds: List<ByteArray>, flags: Int = FLAG_REQUIRED): ByteArray = bytesOf {
        writeHeader(tag = TAG_CALLBACK, nameId = nameId, flags = flags)
        writeKinds(kinds = kinds)
    }

    /** Returns a slot parameter named by string [nameId] that passes [kinds]. */
    fun slot(nameId: Int, kinds: List<ByteArray> = emptyList(), flags: Int = FLAG_REQUIRED): ByteArray = bytesOf {
        writeHeader(tag = TAG_SLOT, nameId = nameId, flags = flags)
        writeByte(ACCEPTS_ANY)
        writeKinds(kinds = kinds)
    }

    /** Returns the kind of a choice from the list named by string [listNameId]. */
    fun choiceKind(listNameId: Int): ByteArray = bytesOf {
        writeByte(KIND_CHOICE)
        writeInt(listNameId)
    }

    /** Returns the kind of a list of [element]. */
    fun listKind(element: ByteArray): ByteArray = bytesOf {
        writeByte(KIND_LIST)
        write(element)
    }
}

private fun DataOutputStream.writeStrings(strings: List<ByteArray>) {
    writeInt(strings.size)
    strings.runningFold(initial = 0) { offset, text -> offset + text.size }.forEach { offset -> writeInt(offset) }
    strings.forEach { text -> write(text) }
}

private fun DataOutputStream.writeChoice(choice: HandEncodedChoice) {
    writeInt(choice.nameId)
    writeInt(choice.entryIds.size)
    choice.entryIds.forEach { entryId -> writeInt(entryId) }
}

private fun DataOutputStream.writeHeader(tag: Int, nameId: Int, flags: Int) {
    writeByte(tag)
    writeInt(nameId)
    writeByte(flags)
}

private fun DataOutputStream.writeKinds(kinds: List<ByteArray>) {
    writeInt(kinds.size)
    kinds.forEach { kind -> write(kind) }
}

private fun bytesOf(write: DataOutputStream.() -> Unit): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use(write)
    return bytes.toByteArray()
}
