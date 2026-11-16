package dev.tonholo.ssrcompose.catalog.model

private const val PART_FILE_SUFFIX = ".part.json"
private val FINGERPRINT = Regex(pattern = "[0-9a-f]{64}")

/**
 * One line of the manifest: a part, and the fingerprint its bytes must have.
 *
 * @property fingerprint the SHA-256 of the part's bytes, in lowercase hexadecimal.
 * @property file the part's file name, found next to the manifest. It is always the part's name followed by
 * `.part.json`, so it is a plain file name: no folder, no parent folder and no scheme.
 * @throws IllegalArgumentException when [fingerprint] is not 64 lowercase hexadecimal digits.
 */
public data class PartEntry(public val name: PartName, public val fingerprint: String) {
    public val file: String = name.value + PART_FILE_SUFFIX

    init {
        require(FINGERPRINT.matches(input = fingerprint)) {
            "The fingerprint of the part ${name.value} must be 64 lowercase hexadecimal digits"
        }
    }
}
