package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.UnplaceableComponent
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayComponent
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Puts the `UnplaceableBridge` annotation, with the error as its message, on the bridge function of a component the
 * server cannot place. The annotation is saved in the compiled bridge, so a module that depends on it can read it.
 */
internal class UnplaceableBridgeMarker {
    fun mark(function: FirNamedFunction, component: ThrowawayComponent) {
        val message = UnplaceableComponent.messageOf(component) ?: return
        function.replaceAnnotations(function.annotations + unplaceableBridge(message = message))
    }

    private fun unplaceableBridge(message: String): FirAnnotation = buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef {
            coneType = ProducerNames.unplaceableBridge.constructClassLikeType()
        }
        argumentMapping = buildAnnotationArgumentMapping {
            mapping[ProducerNames.message] = buildLiteralExpression(
                source = null,
                kind = ConstantValueKind.String,
                value = message,
                setType = true,
            )
        }
    }
}
