package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.output.OutputFolder
import dev.tonholo.ssrcompose.tooling.output.RelativePath
import java.io.File

/** `ssrFontDump`: saves the built font bundle as the saved bundle of its version. */
public object FontDump {
    /**
     * Replaces `[savedRoot]/[version]` with the files of [bundleFolder], so any file the new `fonts.json` does not
     * list is gone. The saved bundles of other versions are kept.
     *
     * @throws StepException when [version] is not a valid font version or [bundleFolder] holds no `fonts.json`,
     * in which case the saved bundle is kept as it was.
     */
    public fun run(bundleFolder: File, savedRoot: File, version: String) {
        val saved = savedRoot.resolve(fontVersionOf(text = version).value)
        requireBuilt(bundleFolder = bundleFolder)
        OutputFolder.replace(target = saved) { staged ->
            bundleFolder.walkTopDown().filter { it.isFile }.forEach { file ->
                val path = RelativePath(value = file.relativeTo(bundleFolder).invariantSeparatorsPath)
                staged.copy(path = path, from = file)
            }
        }
    }

    private fun requireBuilt(bundleFolder: File) {
        if (!bundleFolder.resolve(FontBundleManifest.FILE_NAME).isFile) {
            throw StepException(
                message = "No font bundle is built in $bundleFolder. Run ${FontStepNames.BUNDLE} to build it.",
            )
        }
    }
}
