package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.tooling.fetch.LocationFetcher
import java.io.File
import java.nio.file.Files

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

    /** Saves the files of [from] as version [to], and pins [to] to the same `fonts.json` bytes. */
    fun misfile(from: String, to: String) {
        savedRoot.resolve(from).copyRecursively(target = savedRoot.resolve(to))
        val pin = fontLock.read(file = lockFile).first { entry -> entry.version == from }
        fontLock.write(file = lockFile, entries = fontLock.read(file = lockFile) + pin.copy(version = to))
    }

    /**
     * Replaces [relativePath] of the saved [version] with a link to a copy of the same bytes outside the folder.
     *
     * @return the copy, so a test can change what the link leads to.
     */
    fun replaceWithLink(version: String, relativePath: String): File {
        val saved = savedRoot.resolve("$version/$relativePath")
        val outside = server.resolve("outside").apply { mkdirs() }.resolve("copy.bin")
        saved.copyTo(target = outside, overwrite = true)
        saved.delete()
        Files.createSymbolicLink(saved.toPath(), outside.toPath())
        return outside
    }

    /**
     * Replaces the folder [relativePath] of the saved [version] with a link to a copy of it outside the folder.
     *
     * @return the copy, so a test can change what the link leads to.
     */
    fun replaceFolderWithLink(version: String, relativePath: String): File {
        val saved = savedRoot.resolve("$version/$relativePath")
        val outside = server.resolve("outside-folder").apply { mkdirs() }
        saved.copyRecursively(target = outside, overwrite = true)
        saved.deleteRecursively()
        Files.createSymbolicLink(saved.toPath(), outside.toPath())
        return outside
    }

    /**
     * Moves the saved root `ssr/fonts` to a folder outside the project and leaves a link to it in its place.
     */
    fun replaceSavedRootWithLink() {
        val outside = server.resolve("elsewhere/fonts").apply { parentFile.mkdirs() }
        check(savedRoot.renameTo(outside)) { "Cannot move $savedRoot" }
        Files.createSymbolicLink(savedRoot.toPath(), outside.toPath())
    }

    fun savedFiles(version: String): Map<String, List<Byte>> = filesOf(folder = savedRoot.resolve(version))

    companion object {
        fun filesOf(folder: File): Map<String, List<Byte>> = folder.walkTopDown()
            .filter { it.isFile }
            .associate { it.relativeTo(folder).invariantSeparatorsPath to it.readBytes().toList() }
            .toSortedMap()
    }
}
