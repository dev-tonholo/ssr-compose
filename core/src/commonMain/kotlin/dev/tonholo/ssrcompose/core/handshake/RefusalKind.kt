package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

@Serializable
public enum class RefusalKind {
    ContractVersionTooOld,
    ContractVersionTooNew,
    CatalogVersionNotSupported,
    IntroductionNotValid,
}
