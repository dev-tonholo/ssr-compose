package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.tooling.fetch.LocationFetcher
import java.io.File

/**
 * An app folder that publishes font bundles next to a server folder that syncs them, as two projects that reach
 * each other by a relative file path.
 */
internal class ServerFontFixture(root: File) {
    val app = FontFixture(root = root.resolve("app"))
    val server: File = root.resolve("server")
    val savedRoot: File = server.resolve("ssr/fonts")
    val lockFile: File = server.resolve("ssr/fonts.lock")
    val reports = mutableListOf<String>()

    fun locationOf(version: String): String = "../app/ssr/fonts/$version/fonts.json"

    fun publish(version: String, vararg weights: Int, text: String = "font"): File =
        app.publish(version = version, declarations = weights.map { app.declare(weight = it, text = "$text $it") })

    fun sync(version: String, location: String = locationOf(version = version), update: Boolean = false) {
        FontSync(
            fetcher = LocationFetcher(baseFolder = server),
            savedRoot = savedRoot,
            lockFile = lockFile,
            report = { message -> reports += message },
        ).run(held = listOf(HeldFontBundle(version = version, location = location)), update = update)
    }

    fun savedFiles(version: String): Map<String, List<Byte>> = filesOf(folder = savedRoot.resolve(version))

    companion object {
        fun filesOf(folder: File): Map<String, List<Byte>> = folder.walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(folder).invariantSeparatorsPath to it.readBytes().toList() }
            .toSortedMap()
    }
}
