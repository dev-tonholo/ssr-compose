package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.builder.buildGetClassCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Builds `@OptIn(InternalSsrComposeApi::class)` for a created bridge function, so the bridge may call the producer's
 * bridge entry points while server code that calls the bridge needs no opt-in of its own.
 */
internal class InternalApiOptIn(private val session: FirSession) {
    private val marker: FirClassLikeSymbol<*>? by lazy {
        session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(ProducerNames.internalSsrComposeApi)
    }

    /** Returns the annotation, or null when the producer library is not on the classpath. */
    fun annotationOrNull(): FirAnnotation? {
        val marker = marker ?: return null
        return buildAnnotation {
            annotationTypeRef = buildResolvedTypeRef { coneType = ProducerNames.optIn.constructClassLikeType() }
            argumentMapping = buildAnnotationArgumentMapping {
                mapping[ProducerNames.markerClass] = buildCollectionLiteral {
                    argumentList = buildArgumentList { arguments += classLiteralOf(marker = marker) }
                    coneTypeOrNull = StandardClassIds.Array.constructClassLikeType(
                        typeArguments = arrayOf(ConeKotlinTypeProjectionOut(annotationClassType())),
                    )
                }
            }
        }
    }

    private fun classLiteralOf(marker: FirClassLikeSymbol<*>): FirExpression {
        val markerType = marker.classId.constructClassLikeType()
        return buildGetClassCall {
            argumentList = buildArgumentList {
                arguments += buildResolvedQualifier {
                    packageFqName = marker.classId.packageFqName
                    relativeClassFqName = marker.classId.relativeClassName
                    qualifierSymbol = marker
                    resolvedToCompanionObject = false
                    coneTypeOrNull = markerType
                }
            }
            coneTypeOrNull = StandardClassIds.KClass.constructClassLikeType(typeArguments = arrayOf(markerType))
        }
    }

    private fun annotationClassType(): ConeKotlinType =
        StandardClassIds.KClass.constructClassLikeType(
            typeArguments = arrayOf(ConeKotlinTypeProjectionOut(StandardClassIds.Annotation.constructClassLikeType())),
        )
}
