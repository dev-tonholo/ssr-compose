package dev.tonholo.ssrcompose.core.data

import dev.tonholo.ssrcompose.core.data.internal.Base64ByteArraySerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The neutral model for values on the wire. No encoding is baked into it.
 */
@Serializable
public sealed interface DataValue {
    @Serializable
    @SerialName("Text")
    public data class Text(val value: String) : DataValue

    @Serializable
    @SerialName("WholeNumber")
    public data class WholeNumber(val value: Long) : DataValue

    @Serializable
    @SerialName("DecimalNumber")
    public data class DecimalNumber(val value: Double) : DataValue

    @Serializable
    @SerialName("Flag")
    public data class Flag(val value: Boolean) : DataValue

    /** An entry of a choice list, by name. */
    @Serializable
    @SerialName("Choice")
    public data class Choice(val entry: String) : DataValue

    @Serializable
    @SerialName("List")
    public data class ListValue(val items: List<DataValue>) : DataValue

    /** Bytes exist only as a callback argument. Two values are equal when their bytes are equal. */
    @Serializable
    @SerialName("Bytes")
    public class Bytes(
        @Serializable(with = Base64ByteArraySerializer::class)
        public val value: ByteArray,
    ) : DataValue {
        override fun equals(other: Any?): Boolean = other is Bytes && value.contentEquals(other = other.value)

        override fun hashCode(): Int = value.contentHashCode()

        override fun toString(): String = "Bytes(size=${value.size})"
    }

    @Serializable
    @SerialName("None")
    public data object None : DataValue
}
