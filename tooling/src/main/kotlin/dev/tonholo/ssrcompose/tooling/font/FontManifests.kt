package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleException
import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleReader
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.lock.LockFile

/** `ssr/fonts.lock`: the same shape as `ssr/catalogs.lock`, under `fonts`. */
internal val fontLock = LockFile(section = "fonts")

internal fun readFontManifest(bytes: ByteArray, subject: String): FontBundleManifest = try {
    FontBundleReader.read(bytes = bytes)
} catch (cause: FontBundleException) {
    throw StepException(message = "The $subject is not valid: ${cause.message}", cause = cause)
}
