package dev.tonholo.ssrcompose.compiler.consumer.renderer.fir.checker

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction

/**
 * Registers the FIR declaration checker that validates `@ConsumerComposable` functions for renderer creation.
 */
internal class RendererCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val namedFunctionCheckers: Set<FirDeclarationChecker<FirNamedFunction>> =
            setOf(RendererComposableChecker(session = session))
    }
}
