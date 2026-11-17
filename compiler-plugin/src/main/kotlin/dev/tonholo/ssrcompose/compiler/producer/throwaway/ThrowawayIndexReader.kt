package dev.tonholo.ssrcompose.compiler.producer.throwaway

import java.io.File

/**
 * Reads the throwaway index format: a `component <Name>` line, then one `param <name> <kind> <required|optional>`
 * line for each of its parameters. Lines that start with `#` and blank lines are ignored. An `action-of-text` or
 * `slot-of-text` parameter cannot be optional.
 */
internal object ThrowawayIndexReader {
    private const val COMMENT_PREFIX = "#"
    private const val COMPONENT_KEYWORD = "component"
    private const val PARAMETER_KEYWORD = "param"
    private const val REQUIRED_WORD = "required"
    private const val OPTIONAL_WORD = "optional"
    private const val COMPONENT_FIELDS = 2
    private const val PARAMETER_FIELDS = 4
    private val whitespace = Regex("\\s+")

    fun read(file: File): ThrowawayIndex = parse(lines = file.readLines())

    fun parse(lines: List<String>): ThrowawayIndex {
        val builders = mutableListOf<ComponentBuilder>()
        lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith(COMMENT_PREFIX) }.forEach { line ->
            val fields = line.split(whitespace)
            when (fields.first()) {
                COMPONENT_KEYWORD -> builders += ComponentBuilder(name = componentName(line = line, fields = fields))
                PARAMETER_KEYWORD -> {
                    val owner = requireNotNull(builders.lastOrNull()) {
                        "A parameter comes before any component: $line"
                    }
                    owner.parameters += parameter(line = line, fields = fields)
                }

                else -> throw IllegalArgumentException("Unknown line in the throwaway index: $line")
            }
        }
        return ThrowawayIndex(components = builders.map { it.build() })
    }

    private fun componentName(line: String, fields: List<String>): String {
        require(fields.size == COMPONENT_FIELDS) { "A component line needs a name only: $line" }
        val (_, name) = fields
        return name
    }

    private fun parameter(line: String, fields: List<String>): ThrowawayParameter {
        require(fields.size == PARAMETER_FIELDS) { "A param line needs a name, a kind and required or optional: $line" }
        val (_, name, kindWord) = fields
        val requirement = fields.last()
        val kind = requireNotNull(ThrowawayParameterKind.entries.firstOrNull { it.word == kindWord }) {
            "Unknown parameter kind '$kindWord': $line"
        }
        val isRequired = when (requirement) {
            REQUIRED_WORD -> true
            OPTIONAL_WORD -> false
            else -> throw IllegalArgumentException("Expected required or optional: $line")
        }
        require(isRequired || kind.canBeOptional) {
            "The ${kind.word} parameter '$name' cannot be optional yet: $line"
        }
        return ThrowawayParameter(name = name, kind = kind, isRequired = isRequired)
    }

    private class ComponentBuilder(val name: String) {
        val parameters = mutableListOf<ThrowawayParameter>()

        fun build(): ThrowawayComponent = ThrowawayComponent(name = name, parameters = parameters.toList())
    }
}
