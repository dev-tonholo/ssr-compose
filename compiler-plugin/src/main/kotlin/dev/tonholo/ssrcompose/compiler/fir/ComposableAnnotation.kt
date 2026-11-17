package dev.tonholo.ssrcompose.compiler.fir

import dev.tonholo.ssrcompose.compiler.SsrNames
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType

/**
 * Builds the `@Composable` annotation placed on FIR functions that the compiler plugin creates.
 */
internal fun buildComposableAnnotation(): FirAnnotation = buildAnnotation {
    annotationTypeRef = buildResolvedTypeRef { coneType = SsrNames.composable.constructClassLikeType() }
    argumentMapping = FirEmptyAnnotationArgumentMapping
}
