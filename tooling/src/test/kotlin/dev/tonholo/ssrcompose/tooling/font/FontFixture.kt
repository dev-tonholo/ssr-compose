package dev.tonholo.ssrcompose.tooling.font

import java.io.File

/**
 * An app folder with declared font files, and its saved font bundles under `ssr/fonts`, as `ssrFontBundle` and
 * `ssrFontDump` leave them.
 */
internal class FontFixture(val root: File) {
    val savedRoot: File = root.resolve("ssr/fonts")

    fun declare(
        weight: Int,
        italic: Boolean = false,
        text: String = "font $weight $italic",
        extension: String = "ttf",
    ) =
        FontDeclaration(
            family = FAMILY,
            weight = weight,
            italic = italic,
            file = root.resolve("src/main/res/font/inter_${weight}_$italic.$extension").apply {
                parentFile.mkdirs()
                writeText(text)
            },
        )

    fun bundle(version: String, declarations: List<FontDeclaration>): File {
        val bundleFolder = root.resolve("build/ssr/fonts/bundle")
        FontBundleStep.run(
            version = version,
            declarations = declarations,
            bundleFolder = bundleFolder,
        )
        return bundleFolder
    }

    fun publish(version: String, declarations: List<FontDeclaration>): File {
        val bundleFolder = bundle(version = version, declarations = declarations)
        FontDump.run(bundleFolder = bundleFolder, savedRoot = savedRoot, version = version)
        return savedRoot.resolve(version)
    }

    companion object {
        const val FAMILY = "brand"
        const val REGULAR = 400
        const val BOLD = 700
    }
}
