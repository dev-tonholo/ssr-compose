package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.compare.FolderComparison
import java.io.File

/** `ssrFontCheck`: compares the built font bundle with the saved one of its version. */
public object FontCheck {
    /**
     * @param savedRoot the folder of the saved bundles, `ssr/fonts`, which holds one folder per version.
     * @throws StepException when no bundle is saved for [version], or when any file differs, naming each one.
     */
    public fun run(bundleFolder: File, savedRoot: File, version: String) {
        val saved = savedRoot.resolve(fontVersionOf(text = version).value)
        if (!saved.resolve(FontBundleManifest.FILE_NAME).isFile) {
            throw StepException(
                message = "No font bundle is saved for the font version $version in $saved. " +
                    "Run ${FontStepNames.DUMP} to save it.",
            )
        }
        val differing = FolderComparison.differingFiles(expected = bundleFolder, actual = saved)
        if (differing.isNotEmpty()) {
            throw StepException(
                message = "The font bundle differs from the one saved for the font version $version in $saved: " +
                    "${differing.joinToString()}. A changed font needs a new font version: raise it, then run " +
                    "${FontStepNames.DUMP}.",
            )
        }
    }
}
