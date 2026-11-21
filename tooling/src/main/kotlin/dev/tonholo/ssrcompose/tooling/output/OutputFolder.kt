package dev.tonholo.ssrcompose.tooling.output

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists

private const val HIDDEN_PREFIX = "."
private const val STAGING_SUFFIX = ".staging"
private const val BACKUP_SUFFIX = ".previous"

private object LinkSafeDeletion : SimpleFileVisitor<Path>() {
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        Files.delete(file)
        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
        if (exc != null) throw exc
        Files.delete(dir)
        return FileVisitResult.CONTINUE
    }
}

/** Deletes [folder] and what it holds. A symbolic link is deleted itself, and its target is never touched. */
private fun deleteWithoutFollowingLinks(folder: File) {
    val path = folder.toPath()
    if (path.exists(LinkOption.NOFOLLOW_LINKS)) Files.walkFileTree(path, LinkSafeDeletion)
}

/** A folder that a build step writes its outputs into, by [RelativePath] only. */
public class OutputFolder(public val root: File) {
    public fun fileOf(path: RelativePath): File = root.resolve(path.value)

    public fun write(path: RelativePath, bytes: ByteArray) {
        val file = fileOf(path = path)
        file.parentFile.mkdirs()
        file.writeBytes(array = bytes)
    }

    public fun copy(path: RelativePath, from: File) {
        from.copyTo(target = fileOf(path = path), overwrite = true)
    }

    /** Deletes the folder's content, leaving it empty. */
    public fun clear() {
        deleteWithoutFollowingLinks(folder = root)
        root.mkdirs()
    }

    public companion object {
        /**
         * Replaces [target] whole: [fill] writes the new content into a staging folder next to it, and only when it
         * returns does the staging folder take the target's place. When [fill] throws, [target] is kept as it was.
         * The staging and backup folders start with a dot, which no output folder name such as a version does.
         *
         * @throws IOException when the staging folder cannot take the target's place.
         */
        public fun replace(target: File, fill: (OutputFolder) -> Unit) {
            val staging = target.resolveSibling(HIDDEN_PREFIX + target.name + STAGING_SUFFIX)
            val staged = OutputFolder(root = staging).apply { clear() }
            var filled = false
            try {
                fill(staged)
                filled = true
            } finally {
                if (!filled) deleteWithoutFollowingLinks(folder = staging)
            }
            swap(staging = staging, target = target)
        }

        private fun swap(staging: File, target: File) {
            val backup = target.resolveSibling(HIDDEN_PREFIX + target.name + BACKUP_SUFFIX)
            deleteWithoutFollowingLinks(folder = backup)
            if (target.exists() && !target.renameTo(backup)) throw IOException("Cannot move aside $target")
            if (!staging.renameTo(target)) {
                backup.renameTo(target)
                throw IOException("Cannot move the new content into $target")
            }
            deleteWithoutFollowingLinks(folder = backup)
        }
    }
}
