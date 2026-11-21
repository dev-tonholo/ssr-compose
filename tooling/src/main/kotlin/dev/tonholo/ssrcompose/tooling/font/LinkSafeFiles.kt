package dev.tonholo.ssrcompose.tooling.font

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Reads the whole file, refusing a symbolic link at the file's own path.
 *
 * @throws IOException when the file is a link or cannot be read.
 */
internal fun File.readBytesWithoutFollowingLinks(): ByteArray =
    Files.newInputStream(toPath(), LinkOption.NOFOLLOW_LINKS).use { stream -> stream.readAllBytes() }
