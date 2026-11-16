package dev.tonholo.ssrcompose.core.handshake

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/**
 * The version of the contract between consumer and producer. Versions are in order.
 */
@Serializable
@JvmInline
public value class ContractVersion(public val value: Int) : Comparable<ContractVersion> {
    override fun compareTo(other: ContractVersion): Int = value.compareTo(other = other.value)
}
