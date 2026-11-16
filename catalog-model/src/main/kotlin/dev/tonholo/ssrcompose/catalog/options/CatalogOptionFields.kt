package dev.tonholo.ssrcompose.catalog.options

private const val SEPARATOR = CatalogOptionValue.FIELD_SEPARATOR
private const val ESCAPE = '\\'

internal object CatalogOptionFields {
    fun join(fields: List<String>): String =
        fields.joinToString(separator = SEPARATOR.toString()) { field -> escape(field) }

    fun split(text: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < text.length) {
            when (val char = text[index]) {
                ESCAPE -> {
                    current.append(readEscaped(text = text, escapeIndex = index))
                    index++
                }

                SEPARATOR -> {
                    fields += current.toString()
                    current.clear()
                }

                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }

    private fun escape(field: String): String =
        buildString {
            field.forEach { char ->
                if (char == SEPARATOR || char == ESCAPE) append(ESCAPE)
                append(char)
            }
        }

    private fun readEscaped(text: String, escapeIndex: Int): Char {
        val escaped = text.getOrNull(escapeIndex + 1)
        require(escaped == SEPARATOR || escaped == ESCAPE) {
            "a backslash must be followed by a backslash or a '$SEPARATOR'"
        }
        return escaped
    }
}
