package dev.tonholo.ssrcompose.core.handshake

/**
 * The producer's reply to an [Introduction]. [Refused.reason] is for the user and carries no internal details.
 */
public sealed interface Answer {
    public data class Accepted(val contractVersion: ContractVersion) : Answer

    public data class Refused(val kind: RefusalKind, val reason: String) : Answer
}
