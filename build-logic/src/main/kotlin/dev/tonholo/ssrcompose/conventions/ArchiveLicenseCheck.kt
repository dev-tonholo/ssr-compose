package dev.tonholo.ssrcompose.conventions

import java.io.File
import java.util.zip.ZipInputStream

internal const val META_INF_DIRECTORY = "META-INF"
internal const val LICENSE_ENTRY = "$META_INF_DIRECTORY/LICENSE"
internal const val NOTICE_ENTRY = "$META_INF_DIRECTORY/NOTICE"

private data class ArchiveLicenseFiles(
    val license: ByteArray?,
    val notice: ByteArray?,
)

/**
 * Lists what is wrong with the license files at the top level of a published archive, a jar or an AAR. Returns an
 * empty list when `META-INF/LICENSE` equals [expectedLicense] and `META-INF/NOTICE` equals [expectedNotice].
 */
internal fun archiveLicenseProblems(
    archive: File,
    expectedLicense: ByteArray,
    expectedNotice: ByteArray,
): List<String> {
    val files = readLicenseFiles(archive)
    return buildList {
        addProblemIfDifferent(entry = LICENSE_ENTRY, actual = files.license, expected = expectedLicense)
        addProblemIfDifferent(entry = NOTICE_ENTRY, actual = files.notice, expected = expectedNotice)
    }
}

private fun MutableList<String>.addProblemIfDifferent(entry: String, actual: ByteArray?, expected: ByteArray) {
    when {
        actual == null -> add("$entry is missing")
        !actual.contentEquals(expected) -> add("$entry differs from the repository ${entry.substringAfterLast('/')}")
    }
}

private fun readLicenseFiles(archive: File): ArchiveLicenseFiles {
    var license: ByteArray? = null
    var notice: ByteArray? = null
    ZipInputStream(archive.inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.forEach { entry ->
            when (entry.name) {
                LICENSE_ENTRY -> license = zip.readBytes()
                NOTICE_ENTRY -> notice = zip.readBytes()
            }
        }
    }
    return ArchiveLicenseFiles(license = license, notice = notice)
}
