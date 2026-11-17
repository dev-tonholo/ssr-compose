package dev.tonholo.ssrcompose.compiler.producer.fir

import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/** Registers only the check that fails every use of a bridge that cannot be placed. It creates no bridge. */
internal class UnplaceableBridgeCheckRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirAdditionalCheckersExtension.Factory { session -> UnplaceableBridgeCheckers(session = session) }
    }
}
