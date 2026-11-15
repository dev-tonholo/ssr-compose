package dev.tonholo.ssrcompose.protocol

import kotlinx.serialization.KSerializer

interface ComponentContract<P> {
    val id: String
    val propsSerializer: KSerializer<P>
}
