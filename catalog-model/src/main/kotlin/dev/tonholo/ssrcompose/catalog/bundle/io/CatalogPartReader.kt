package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.mapping.toPart
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogPartFile
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint

internal fun partSubject(name: PartName): String = "part ${name.value}"

internal object CatalogPartReader {
    fun read(bytes: ByteArray, sizeCap: SizeCap, subject: String): CatalogPart =
        readWithinCap(bytes = bytes.requireWithin(sizeCap = sizeCap, subject = subject), subject = subject)

    /**
     * Reads the part [entry] describes. The bytes are checked against the entry's fingerprint before anything in
     * them is parsed, and the part must be the one the entry names.
     */
    fun read(entry: PartEntry, bytes: ByteArray, sizeCap: SizeCap): CatalogPart {
        val subject = partSubject(name = entry.name)
        val withinCap = bytes.requireWithin(sizeCap = sizeCap, subject = subject)
        if (Fingerprint.of(bytes = withinCap) != entry.fingerprint) {
            throw CatalogFileException(message = "The $subject does not match its fingerprint in the manifest")
        }
        val part = readWithinCap(bytes = withinCap, subject = subject)
        if (part.name != entry.name) {
            throw CatalogFileException(message = "The file of the $subject is for the ${partSubject(name = part.name)}")
        }
        return part
    }

    private fun readWithinCap(bytes: ByteArray, subject: String): CatalogPart = readFile(
        subject = subject,
        bytes = bytes,
        serializer = CatalogPartFile.serializer(),
        toModel = CatalogPartFile::toPart,
        writeModel = CatalogPartWriter::writeTo,
    )
}
