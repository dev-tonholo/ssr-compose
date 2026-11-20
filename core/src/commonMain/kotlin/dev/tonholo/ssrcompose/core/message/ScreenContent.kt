package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.data.DataValue
import kotlin.jvm.JvmInline
import kotlinx.serialization.SerialName
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
    val slots: Map<String, ScreenContent> = emptyMap(),
)

/**
 * What a screen or a slot shows. On the wire it is an object tagged by `type`, so new kinds of content can join
 * without changing the type of a screen or a slot.
 */
@InternalSsrComposeApi
@Serializable
public sealed interface ScreenContent {
    /**
     * An ordered list of placed components, which the receiving layout arranges. At the screen root the consumer
     * shows it top to bottom. Provisional until Feature 2 adds server-rendered documents.
     */
    @Serializable
    @SerialName("Components")
    public data class Components(val components: List<PlacedComponent>) : ScreenContent
}
