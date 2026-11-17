package dev.tonholo.ssrcompose.consumer.internal

import dev.tonholo.ssrcompose.core.data.DataValue

internal object KindName {
    const val TEXT = "text"
    const val WHOLE = "whole"
    const val DECIMAL = "decimal"
    const val FLAG = "flag"
    const val CHOICE = "choice"
    const val LIST = "list"
    const val BYTES = "bytes"
    const val NONE = "none"
}

internal fun DataValue.kindName(): String = when (this) {
    is DataValue.Text -> KindName.TEXT
    is DataValue.WholeNumber -> KindName.WHOLE
    is DataValue.DecimalNumber -> KindName.DECIMAL
    is DataValue.Flag -> KindName.FLAG
    is DataValue.Choice -> KindName.CHOICE
    is DataValue.ListValue -> KindName.LIST
    is DataValue.Bytes -> KindName.BYTES
    DataValue.None -> KindName.NONE
}
