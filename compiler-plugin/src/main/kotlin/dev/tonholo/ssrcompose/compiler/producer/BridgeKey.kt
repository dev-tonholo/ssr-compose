package dev.tonholo.ssrcompose.compiler.producer

import org.jetbrains.kotlin.GeneratedDeclarationKey

/**
 * Marks every declaration created for one held catalog. Each catalog has its own key, so the body of a created
 * function knows which catalog version it passes on.
 */
internal class BridgeKey(val catalogVersion: String) : GeneratedDeclarationKey() {
    override fun toString(): String = "BridgeKey(catalogVersion=$catalogVersion)"
}
