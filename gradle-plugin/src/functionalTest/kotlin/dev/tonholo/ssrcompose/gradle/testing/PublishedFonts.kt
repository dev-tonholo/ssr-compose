package dev.tonholo.ssrcompose.gradle.testing

import dev.tonholo.ssrcompose.tooling.font.FontBundleStep
import dev.tonholo.ssrcompose.tooling.font.FontDeclaration
import dev.tonholo.ssrcompose.tooling.font.FontDump
import java.io.File

/** An app folder that has saved font bundles under `ssr/fonts`, as `ssrFontDump` leaves them. */
internal class PublishedFonts(private val appFolder: File) {
    fun publish(version: String, text: String = "font"): File {
        val declarations = WEIGHTS.map { weight ->
            val file = appFolder.resolve("src/main/res/font/inter_$weight.ttf").apply {
                parentFile.mkdirs()
                writeText("$text $weight")
            }
            FontDeclaration(family = FAMILY, weight = weight, italic = false, file = file)
        }
        val bundle = appFolder.resolve("build/ssr/fonts/bundle")
        FontBundleStep.run(
            version = version,
            declarations = declarations,
            bundleFolder = bundle,
        )
        FontDump.run(bundleFolder = bundle, savedRoot = appFolder.resolve(SAVED_FOLDER), version = version)
        return appFolder.resolve("$SAVED_FOLDER/$version")
    }

    companion object {
        const val FAMILY = "brand"
        const val SAVED_FOLDER = "ssr/fonts"
        val WEIGHTS = listOf(400, 700)

        /** The `fonts { }` block of a server that holds [version], published in the sibling folder `app`. */
        fun serverBlock(version: String): String =
            // language=kotlin
            """
            fonts {
                bundle(version = "$version", location = "../app/ssr/fonts/$version/fonts.json")
            }
            """.trimIndent()
    }
}
