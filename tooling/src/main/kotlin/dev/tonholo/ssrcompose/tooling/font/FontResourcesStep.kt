package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontVersionsIndex
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.output.OutputFolder
import dev.tonholo.ssrcompose.tooling.output.RelativePath
import java.io.File
import java.io.IOException

/** `ssrFontResources`: puts the saved font bundles where the producer loads them when it starts. */
public object FontResourcesStep {
    private val INDEX_PATH = RelativePath(value = FontVersionsIndex.PATH)

    /**
     * Checks every saved bundle of [versions] as `ssrFontVerify` does, except for files `fonts.json` does not list.
     * Then empties [outputFolder], copies `fonts.json` and each font file it lists to
     * `ssrcompose/fonts/<version>/`, and writes the index of the versions. Nothing is copied when a check fails.
     * A version given more than once is handled once. With no version, it only empties [outputFolder].
     *
     * @throws StepException when a version has no saved copy, has a symbolic link in it, at its folder or at the
     * saved root, or does not match [lockFile] or its own `fonts.json`, or when its `fonts.json` is not valid.
     */
    public fun run(versions: List<String>, savedRoot: File, lockFile: File, outputFolder: File) {
        val bundleVersions = versions.map { fontVersionOf(text = it) }.distinct()
        if (bundleVersions.isEmpty()) {
            OutputFolder(root = outputFolder).clear()
            return
        }
        requireSound(versions = bundleVersions, savedRoot = savedRoot, lockFile = lockFile)
        val output = OutputFolder(root = outputFolder).apply { clear() }
        bundleVersions.forEach { version ->
            output.copyBundle(version = version, saved = savedRoot.resolve(version.value))
        }
        output.write(path = INDEX_PATH, bytes = FontVersionsIndex.write(versions = bundleVersions))
    }

    private fun requireSound(versions: List<FontBundleVersion>, savedRoot: File, lockFile: File) {
        val pins = fontLock.read(file = lockFile).associateBy { it.version }
        val problems = versions.flatMap { version ->
            SavedBundleCheck(
                version = version.value,
                saved = savedRoot.resolve(version.value),
                pin = pins[version.value],
                reportUnlistedFiles = false,
            ).problems()
        }
        if (problems.isNotEmpty()) {
            throw StepException(
                message = "The saved font bundles cannot be used:\n" +
                    problems.joinToString(separator = "\n") { "- $it" } +
                    "\nRun ${FontStepNames.SYNC} --update to fetch them again.",
            )
        }
    }

    private fun OutputFolder.copyBundle(version: FontBundleVersion, saved: File) {
        val manifestFile = saved.resolve(FontBundleManifest.FILE_NAME)
        val manifestBytes = readSaved(file = manifestFile)
        val manifest = readFontManifest(bytes = manifestBytes, subject = "saved $manifestFile")
        val names = listOf(FontBundleManifest.FILE_NAME) + manifest.fonts.map { it.file }
        val folder = "${FontVersionsIndex.FOLDER}/${version.value}"
        names.forEach { name ->
            write(path = RelativePath(value = "$folder/$name"), bytes = readSaved(file = saved.resolve(name)))
        }
    }

    private fun readSaved(file: File): ByteArray = try {
        file.readBytesWithoutFollowingLinks()
    } catch (cause: IOException) {
        throw StepException(message = "The saved $file cannot be read: ${cause.message}", cause = cause)
    }
}
