package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleException
import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleReader
import dev.tonholo.ssrcompose.core.font.FontEntry
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.fingerprint.FingerprintCheck
import dev.tonholo.ssrcompose.tooling.lock.LockEntry
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Checks the saved copy of one font version against its lock entry and its own `fonts.json`. Nothing is read through
 * a symbolic link, and a symbolic link in the folder, the folder itself or the saved root above it is a problem.
 *
 * @param reportUnlistedFiles whether a file that `fonts.json` does not list is a problem.
 */
internal class SavedBundleCheck(
    private val version: String,
    private val saved: File,
    private val pin: LockEntry?,
    private val reportUnlistedFiles: Boolean,
) {
    fun problems(): List<String> = try {
        savedProblems()
    } catch (cause: IOException) {
        listOf("The saved copy of font version $version cannot be read: ${cause.message}")
    }

    private fun savedProblems(): List<String> {
        val manifestFile = saved.resolve(FontBundleManifest.FILE_NAME)
        val content = SavedContent.of(folder = saved)
        val linkProblems = content.links.map { link -> "The saved $link of font version $version is a symbolic link" }
        return linkProblems + when {
            content.isLinked(path = FontBundleManifest.FILE_NAME) -> emptyList()
            FontBundleManifest.FILE_NAME !in content.files ->
                listOf("The font version $version has no saved copy in $saved")
            manifestFile.length() > FontBundleReader.MANIFEST_SIZE_CAP ->
                listOf("The fonts.json of font version $version is over its size cap")
            else -> problemsOf(bytes = manifestFile.readBytesWithoutFollowingLinks(), content = content)
        }
    }

    private fun problemsOf(bytes: ByteArray, content: SavedContent): List<String> =
        pinProblems(bytes = bytes) + manifestProblems(bytes = bytes, content = content)

    private fun pinProblems(bytes: ByteArray): List<String> = when (pin) {
        null -> listOf("The font version $version has no entry in the lock file")
        else -> listOfNotNull(
            failureOf {
                val subject = "fonts.json of font version $version in the lock file"
                FingerprintCheck.require(bytes = bytes, fingerprint = pin.fingerprint, subject = subject)
            },
        )
    }

    private fun manifestProblems(bytes: ByteArray, content: SavedContent): List<String> = try {
        val manifest = FontBundleReader.read(bytes = bytes)
        versionProblems(manifest = manifest) +
            manifest.fonts.mapNotNull { entry -> fileProblem(entry = entry, content = content) } +
            unlistedFiles(manifest = manifest, content = content)
    } catch (cause: FontBundleException) {
        listOf("The fonts.json of font version $version is not valid: ${cause.message}")
    }

    private fun versionProblems(manifest: FontBundleManifest): List<String> =
        if (manifest.version.value == version) {
            emptyList()
        } else {
            listOf("The fonts.json of font version $version is for font version ${manifest.version.value}")
        }

    private fun fileProblem(entry: FontEntry, content: SavedContent): String? {
        val file = saved.resolve(entry.file)
        val subject = "font file ${entry.file} of font version $version"
        return when {
            content.isLinked(path = entry.file) -> null
            entry.file !in content.files -> "The $subject is missing"
            file.length() > FontBundleReader.FONT_FILE_SIZE_CAP -> "The $subject is over its size cap"
            else -> failureOf {
                val bytes = file.readBytesWithoutFollowingLinks()
                FingerprintCheck.require(bytes = bytes, fingerprint = entry.fingerprint, subject = subject)
            }
        }
    }

    private fun unlistedFiles(manifest: FontBundleManifest, content: SavedContent): List<String> {
        if (!reportUnlistedFiles) return emptyList()
        val listed = manifest.fonts.map { it.file }.toSet() + FontBundleManifest.FILE_NAME
        return (content.files - listed).sorted()
            .map { path -> "The file $path of font version $version is not listed in its fonts.json" }
    }

    private fun failureOf(check: () -> Unit): String? = try {
        check()
        null
    } catch (failure: StepException) {
        failure.message
    }
}

/**
 * What a folder holds, found without following a symbolic link: its regular files and its links, by relative path.
 * When the folder or its parent is a link, it holds nothing that may be read.
 */
private class SavedContent(
    val files: Set<String>,
    val links: List<String>,
    private val coversEverything: Boolean = false,
) {
    /** Whether [path] is a link, or lies in a linked folder, so nothing at it may be read. */
    fun isLinked(path: String): Boolean =
        coversEverything || links.any { link -> path == link || path.startsWith("$link/") }

    companion object {
        fun of(folder: File): SavedContent {
            val root = folder.toPath()
            val linkedFolders = listOfNotNull(folder.parentFile, folder)
                .filter { candidate -> Files.isSymbolicLink(candidate.toPath()) }
            return when {
                linkedFolders.isNotEmpty() -> SavedContent(
                    files = emptySet(),
                    links = linkedFolders.map { linked -> linked.name },
                    coversEverything = true,
                )
                !Files.exists(root, LinkOption.NOFOLLOW_LINKS) -> SavedContent(files = emptySet(), links = emptyList())
                else -> walked(root = root)
            }
        }

        private fun walked(root: Path): SavedContent {
            val files = mutableSetOf<String>()
            val links = mutableListOf<String>()
            Files.walkFileTree(
                root,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val name = relativeName(root = root, file = file)
                        if (attrs.isSymbolicLink) links += name else files += name
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = throw exc
                },
            )
            return SavedContent(files = files, links = links.sorted())
        }

        private fun relativeName(root: Path, file: Path): String {
            val relative = root.relativize(file).toString().replace(oldChar = File.separatorChar, newChar = '/')
            return relative.ifEmpty { root.fileName.toString() }
        }
    }
}
