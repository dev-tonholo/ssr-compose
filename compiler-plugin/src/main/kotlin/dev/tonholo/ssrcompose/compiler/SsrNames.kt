package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Shared [ClassId] definitions used across both consumer and producer compiler plugin components.
 */
internal object SsrNames {
    private val composePackage: FqName = FqName("androidx.compose.runtime")

    val composable: ClassId = ClassId(
        packageFqName = composePackage,
        topLevelName = Name.identifier("Composable"),
    )

    val consumerComposable: ClassId = ClassId.topLevel(
        topLevelFqName = FqName(ConsumerComposable::class.java.name),
    )

    val producerComposable: ClassId = ClassId.topLevel(
        topLevelFqName = FqName(ProducerComposable::class.java.name),
    )
}
