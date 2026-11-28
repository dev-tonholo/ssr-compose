package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleReader
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontEntry
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.fetch.LocationFetcher
import dev.tonholo.ssrcompose.tooling.fetch.UnreachableLocationException
import dev.tonholo.ssrcompose.tooling.fingerprint.FingerprintCheck
import dev.tonholo.ssrcompose.tooling.lock.LockEntry
import dev.tonholo.ssrcompose.tooling.output.OutputFolder
import dev.tonholo.ssrcompose.tooling.output.RelativePath
import java.io.File

/**
 * `ssrFontSync`: fetches, checks and pins the font bundles the server holds.
 *
 * @param savedRoot the folder of the saved bundles, `ssr/fonts`, which holds one folder per version.
 * @param lockFile `ssr/fonts.lock`.
 * @param report tells the developer about a saved copy that was kept.
 */
public class FontSync(
    private val fetcher: LocationFetcher,
    private val savedRoot: File,
    private val lockFile: File,
    private val report: (String) -> Unit,
) {
    /**
     * Syncs each of [held]. A bundle is saved whole or not at all. A saved copy whose `fonts.json` no longer
     * matches the lock is kept unless [update] is set, and a saved copy is kept when its location cannot be reached.
     *
     * @throws StepException when a bundle cannot be reached and has no saved copy, or its `fonts.json` or one of
     * its font files is not valid, does not match, or cannot be fetched. Nothing of that bundle is saved then.
     * When the lock file cannot be written, the step fails with an IOException and the saved copy is kept too.
     */
    public fun run(held: List<HeldFontBundle>, update: Boolean) {
        held.forEach { bundle -> sync(bundle = bundle, update = update) }
    }

    private fun sync(bundle: HeldFontBundle, update: Boolean) {
        val version = fontVersionOf(text = bundle.version)
        val pin = fontLock.read(file = lockFile).firstOrNull { it.version == version.value }
        val hasSavedCopy = pin != null && savedRoot.resolve(version.value).isDirectory
        val manifestBytes = fetchManifest(bundle = bundle, hasSavedCopy = hasSavedCopy) ?: return
        val manifest = readFetchedManifest(bytes = manifestBytes, bundle = bundle, version = version)
        val matchesPin = pin?.fingerprint == Fingerprint.of(bytes = manifestBytes)
        when {
            update || pin == null || (matchesPin && !hasSavedCopy) ->
                save(bundle = bundle, manifest = manifest, manifestBytes = manifestBytes)
            matchesPin -> recordLocation(bundle = bundle, pin = pin)
            hasSavedCopy -> report(changedMessage(bundle = bundle))
            else -> throw StepException(message = changedMessage(bundle = bundle, kept = NO_SAVED_COPY))
        }
    }

    private fun fetchManifest(bundle: HeldFontBundle, hasSavedCopy: Boolean): ByteArray? = try {
        val subject = manifestSubject(bundle = bundle)
        fetcher.fetch(location = bundle.location, sizeCap = MANIFEST_SIZE_CAP, subject = subject)
    } catch (cause: UnreachableLocationException) {
        if (!hasSavedCopy) {
            val message = "${cause.message}, and no copy is saved for font version ${bundle.version}"
            throw StepException(message = message, cause = cause)
        }
        report("${cause.message}. The saved copy of font version ${bundle.version} is kept.")
        null
    }

    private fun readFetchedManifest(
        bytes: ByteArray,
        bundle: HeldFontBundle,
        version: FontBundleVersion,
    ): FontBundleManifest {
        val manifest = readFontManifest(bytes = bytes, subject = manifestSubject(bundle = bundle))
        if (manifest.version != version) {
            throw StepException(
                message = "The ${manifestSubject(bundle = bundle)} is for font version ${manifest.version.value}, " +
                    "not the declared ${version.value}",
            )
        }
        return manifest
    }

    private fun save(bundle: HeldFontBundle, manifest: FontBundleManifest, manifestBytes: ByteArray) {
        val fingerprint = Fingerprint.of(bytes = manifestBytes)
        val pin = LockEntry(version = bundle.version, location = bundle.location, fingerprint = fingerprint)
        OutputFolder.replace(target = savedRoot.resolve(bundle.version)) { staged ->
            stage(staged = staged, bundle = bundle, manifest = manifest, manifestBytes = manifestBytes)
            pinInLock(entry = pin)
        }
    }

    private fun stage(
        staged: OutputFolder,
        bundle: HeldFontBundle,
        manifest: FontBundleManifest,
        manifestBytes: ByteArray,
    ) {
        manifest.fonts.forEach { entry ->
            staged.write(path = RelativePath(value = entry.file), bytes = fetchFont(bundle = bundle, entry = entry))
        }
        staged.write(path = RelativePath(value = FontBundleManifest.FILE_NAME), bytes = manifestBytes)
    }

    private fun recordLocation(bundle: HeldFontBundle, pin: LockEntry?) {
        if (pin == null || pin.location == bundle.location) return
        pinInLock(entry = pin.copy(location = bundle.location))
    }

    private fun pinInLock(entry: LockEntry) {
        val others = fontLock.read(file = lockFile).filter { it.version != entry.version }
        fontLock.write(file = lockFile, entries = others + entry)
    }

    private fun fetchFont(bundle: HeldFontBundle, entry: FontEntry): ByteArray {
        val subject = "font file ${entry.file} of font version ${bundle.version}"
        val bytes = try {
            val location = fetcher.sibling(location = bundle.location, fileName = entry.file)
            fetcher.fetch(location = location, sizeCap = FontBundleReader.FONT_FILE_SIZE_CAP, subject = subject)
        } catch (cause: UnreachableLocationException) {
            val message = "${cause.message}. Nothing of font version ${bundle.version} is saved."
            throw StepException(message = message, cause = cause)
        }
        FingerprintCheck.require(bytes = bytes, fingerprint = entry.fingerprint, subject = subject)
        return bytes
    }

    private fun manifestSubject(bundle: HeldFontBundle) =
        "fonts.json of font version ${bundle.version} at ${bundle.location}"

    private fun changedMessage(bundle: HeldFontBundle, kept: String = "The saved copy is kept.") =
        "The ${manifestSubject(bundle = bundle)} no longer matches ${lockFile.name}. $kept " +
            "Run ${FontStepNames.SYNC} --update to replace it."

    private companion object {
        const val MANIFEST_SIZE_CAP = FontBundleReader.MANIFEST_SIZE_CAP.toLong()
        const val NO_SAVED_COPY = "No copy is saved."
    }
}
