package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor

/** One way to store the index of a catalog, measured by the scale experiment of T031a. */
interface CandidateIndexFormat {
    val name: String

    fun write(catalog: Catalog): ByteArray

    /** @throws IllegalArgumentException when [bytes] are not an index of this format. */
    fun open(bytes: ByteArray): CandidateIndex
}

/** An opened index. */
interface CandidateIndex {
    val version: String
    val choices: List<ChoiceList>

    /** The name of every component, sorted. */
    val componentNames: List<String>

    fun component(name: String): ComponentDescriptor?
}

/** The formats the scale experiment compares. */
object CandidateIndexFormats {
    val all: List<CandidateIndexFormat> = listOf(LineIndexFormat, BinaryIndexFormat)
}

internal fun Catalog.sortedComponents(): List<ComponentDescriptor> =
    parts.flatMap { it.components }.sortedBy { it.name }

internal fun Catalog.sortedChoices(): List<ChoiceList> =
    parts.flatMap { it.choices }.distinct().sortedBy { it.name }
