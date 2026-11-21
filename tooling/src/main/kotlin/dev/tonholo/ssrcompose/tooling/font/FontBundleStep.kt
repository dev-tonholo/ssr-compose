package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.catalog.bundle.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.core.font.FontBundleException
import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleReader
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontBundleWriter
import dev.tonholo.ssrcompose.core.font.FontEntry
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.output.OutputFolder
import dev.tonholo.ssrcompose.tooling.output.RelativePath
import java.io.File

/** `ssrFontBundle`: builds the app's font bundle from its declared fonts. */
public object FontBundleStep {
    private val MANIFEST_PATH = RelativePath(value = FontBundleManifest.FILE_NAME)

    /**
     * Writes the bundle, `fonts.json` and one file per declaration, into [bundleFolder], emptied first. The bytes
     * depend only on the declared fonts and [version], never on where the files are or in which order they are
     * declared.
     *
     * @throws StepException when [version] is not a valid font version, a declared file is missing, over 16 MiB or
     * not `ttf` or `otf`, a family is not valid, two declarations share a family, weight and italic, or the
     * `fonts.json` would be over the size cap the reader allows.
     */
    public fun run(version: String, declarations: List<FontDeclaration>, bundleFolder: File) {
        val bundleVersion = fontVersionOf(text = version)
        val bundle = OutputFolder(root = bundleFolder).apply { clear() }
        val entries = declarations.map { declaration -> bundle.writeFont(declaration = declaration) }
        bundle.write(path = MANIFEST_PATH, bytes = manifestBytesOf(version = bundleVersion, entries = entries))
    }

    private fun manifestBytesOf(version: FontBundleVersion, entries: List<FontEntry>): ByteArray = try {
        FontBundleWriter.write(manifest = FontBundleManifest.sortedOf(version = version, fonts = entries))
    } catch (cause: IllegalArgumentException) {
        throw notBundled(subject = "The declared fonts", cause = cause)
    } catch (cause: FontBundleException) {
        throw notBundled(subject = "The declared fonts", cause = cause)
    }

    private fun OutputFolder.writeFont(declaration: FontDeclaration): FontEntry {
        val bytes = readDeclared(file = declaration.file)
        val entry = try {
            FontEntry(
                family = declaration.family,
                weight = declaration.weight,
                italic = declaration.italic,
                file = FontEntry.fileNameOf(
                    family = declaration.family,
                    weight = declaration.weight,
                    italic = declaration.italic,
                    extension = declaration.file.extension.lowercase(),
                ),
                fingerprint = Fingerprint.of(bytes = bytes),
            )
        } catch (cause: IllegalArgumentException) {
            throw notBundled(subject = "The font ${declaration.file.name}", cause = cause)
        }
        write(path = RelativePath(value = entry.file), bytes = bytes)
        return entry
    }

    private fun readDeclared(file: File): ByteArray {
        if (!file.isFile) throw StepException(message = "The declared font ${file.name} is not found at $file")
        try {
            FontBundleReader.requireFontFileSize(sizeBytes = file.length())
        } catch (cause: FontBundleException) {
            throw notBundled(subject = "The declared font ${file.name}", cause = cause)
        }
        return file.readBytes()
    }

    private fun notBundled(subject: String, cause: Exception): StepException =
        StepException(message = "$subject cannot be bundled: ${cause.message}", cause = cause)
}
