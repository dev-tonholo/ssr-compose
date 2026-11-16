package dev.tonholo.ssrcompose.catalog.options

/**
 * One value of the repeated `catalog` option: a held catalog's version, the path of its index and its bridge package.
 */
public data class CatalogOptionValue(
    val version: String,
    val indexPath: String,
    val bridgePackage: String,
) {
    /** Writes this value as the text passed to the compiler plugin. */
    public fun encode(): String = CatalogOptionFields.join(listOf(version, indexPath, bridgePackage))

    public companion object {
        /** The character that separates the fields of an encoded value. */
        public const val FIELD_SEPARATOR: Char = '|'

        /** The names of the fields, in the order they are written. */
        public val FIELD_NAMES: List<String> = listOf("version", "index path", "bridge package")

        /**
         * Reads a value written by [encode].
         *
         * @throws IllegalArgumentException when [text] is not a well formed value. The message names the option.
         */
        public fun decode(text: String): CatalogOptionValue {
            val fields = split(text)
            require(fields.size == FIELD_NAMES.size) {
                val needed = FIELD_NAMES.joinToString(separator = ", ")
                malformed(text = text, reason = "it needs ${FIELD_NAMES.size} fields ($needed), found ${fields.size}")
            }
            require(fields.none { it.isEmpty() }) { malformed(text = text, reason = "no field may be empty") }
            val (version, indexPath, bridgePackage) = fields
            return CatalogOptionValue(version = version, indexPath = indexPath, bridgePackage = bridgePackage)
        }

        private fun split(text: String): List<String> =
            try {
                CatalogOptionFields.split(text)
            } catch (failure: IllegalArgumentException) {
                throw IllegalArgumentException(malformed(text = text, reason = failure.message.orEmpty()), failure)
            }

        private fun malformed(text: String, reason: String): String =
            "The '${CompilerPluginOptions.CATALOG}' option value '$text' is malformed: $reason."
    }
}
