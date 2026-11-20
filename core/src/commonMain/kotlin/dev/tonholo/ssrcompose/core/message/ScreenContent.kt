package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.data.DataValue
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * A short text the producer assigns to one callback of one placed component.
 * It is unique inside a session and means nothing outside it.
 */
@Serializable
@JvmInline
public value class ActionId(public val value: String)

/**
 * A component of the session's catalog with the parameters the screen gave it.
 * A parameter the screen did not give is absent from all three maps.
 */
@InternalSsrComposeApi
@Serializable
public data class PlacedComponent(
    val name: String,
    val values: Map<String, DataValue> = emptyMap(),
    val callbacks: Map<String, ActionId> = emptyMap(),
    val slots: Map<String, List<PlacedComponent>> = emptyMap(),
)

/**
 * Placed components shown top to bottom. This top-level arrangement is provisional.
 */
@InternalSsrComposeApi
@Serializable
@JvmInline
public value class ScreenContent(public val components: List<PlacedComponent>)
