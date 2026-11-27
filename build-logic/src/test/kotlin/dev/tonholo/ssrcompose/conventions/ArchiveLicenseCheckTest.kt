package dev.tonholo.ssrcompose.conventions

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveLicenseCheckTest {
    @Test
    fun `given jar with the same LICENSE and NOTICE, when checked, then no problem is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.jar",
            content = zipBytes(
                "META-INF/LICENSE" to LICENSE_TEXT,
                "META-INF/NOTICE" to NOTICE_TEXT,
                "dev/example/Hello.class" to CLASS_BYTES,
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = emptyList(), actual = problems)
    }

    @Test
    fun `given jar without a NOTICE, when checked, then the missing NOTICE is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.jar",
            content = zipBytes(
                "META-INF/LICENSE" to LICENSE_TEXT,
                "dev/example/Hello.class" to CLASS_BYTES,
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = listOf("META-INF/NOTICE is missing"), actual = problems)
    }

    @Test
    fun `given jar without a LICENSE, when checked, then the missing LICENSE is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.jar",
            content = zipBytes(
                "META-INF/NOTICE" to NOTICE_TEXT,
                "dev/example/Hello.class" to CLASS_BYTES,
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = listOf("META-INF/LICENSE is missing"), actual = problems)
    }

    @Test
    fun `given jar with a LICENSE that differs from the repository, when checked, then the difference is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.jar",
            content = zipBytes(
                "META-INF/LICENSE" to "An older licence text".toByteArray(),
                "META-INF/NOTICE" to NOTICE_TEXT,
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = listOf("META-INF/LICENSE differs from the repository LICENSE"), actual = problems)
    }

    @Test
    fun `given jar with a NOTICE that differs from the repository, when checked, then the difference is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.jar",
            content = zipBytes(
                "META-INF/LICENSE" to LICENSE_TEXT,
                "META-INF/NOTICE" to "An older notice".toByteArray(),
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = listOf("META-INF/NOTICE differs from the repository NOTICE"), actual = problems)
    }

    @Test
    fun `given aar with both files at its top level, when checked, then no problem is reported`() {
        // Arrange
        val archive = zipFile(
            name = "module.aar",
            content = zipBytes(
                "META-INF/LICENSE" to LICENSE_TEXT,
                "META-INF/NOTICE" to NOTICE_TEXT,
                "classes.jar" to zipBytes("dev/example/Hello.class" to CLASS_BYTES),
                "AndroidManifest.xml" to ByteArray(0),
            ),
        )

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(expected = emptyList(), actual = problems)
    }

    @Test
    fun `given aar with license files only inside its classes jar, when checked, then both files are reported missing`() {
        // Arrange
        val classesJar = zipBytes(
            "META-INF/LICENSE" to LICENSE_TEXT,
            "META-INF/NOTICE" to NOTICE_TEXT,
            "dev/example/Hello.class" to CLASS_BYTES,
        )
        val archive = zipFile(name = "module.aar", content = zipBytes("classes.jar" to classesJar))

        // Act
        val problems = archiveLicenseProblems(archive = archive, expectedLicense = LICENSE_TEXT, expectedNotice = NOTICE_TEXT)

        // Assert
        assertEquals(
            expected = listOf("META-INF/LICENSE is missing", "META-INF/NOTICE is missing"),
            actual = problems,
        )
    }

    private fun zipFile(name: String, content: ByteArray): File {
        val file = Files.createTempFile("archive", ".${name.substringAfterLast('.')}").toFile()
        file.writeBytes(content)
        file.deleteOnExit()
        return file
    }

    private fun zipBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private companion object {
        val LICENSE_TEXT = "Apache License, Version 2.0 test text".toByteArray()
        val NOTICE_TEXT = "ssr-compose notice".toByteArray()
        val CLASS_BYTES = byteArrayOf(0xCA.toByte(), 0xFE.toByte())
    }
}
