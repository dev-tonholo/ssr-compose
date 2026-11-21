package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontVersionResource
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.output.OutputFolder
import dev.tonholo.ssrcompose.tooling.output.RelativePath
import java.io.File

/** `ssrFontVersion<Variant>`: writes the resource that carries the app's font version to the consumer. */
public object FontVersionResourceStep {
    private val RESOURCE_PATH = RelativePath(value = FontVersionResource.PATH)

    /**
     * Empties [resourceFolder] and writes the font version resource into it.
     *
     * @throws StepException when [version] is not a valid font version.
     */
    public fun run(version: String, resourceFolder: File) {
        val bundleVersion = fontVersionOf(text = version)
        OutputFolder(root = resourceFolder).apply {
            clear()
            write(path = RESOURCE_PATH, bytes = FontVersionResource.write(version = bundleVersion))
        }
    }
}
