package dev.tonholo.ssrcompose.protocol

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ActionId(val value: String)
