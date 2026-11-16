package dev.tonholo.ssrcompose.core.data

/**
 * The kind of a value a parameter holds or passes. [Bytes] is only allowed as a callback argument.
 */
public sealed interface ValueKind {
    public data object Text : ValueKind

    public data object Whole : ValueKind

    public data object Decimal : ValueKind

    public data object Flag : ValueKind

    public data object Bytes : ValueKind

    /** One entry of the choice list called [listName]. */
    public data class Choice(public val listName: String) : ValueKind

    /**
     * A list of one element kind.
     *
     * @throws IllegalArgumentException when the element is [Bytes] or another list.
     */
    public data class ListOf(public val element: ValueKind) : ValueKind {
        init {
            require(element != Bytes && element !is ListOf) { "A list holds a text, whole, decimal, flag or choice" }
        }
    }
}
