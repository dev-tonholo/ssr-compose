package dev.tonholo.ssrcompose.compiler.producer

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object ProducerNames {
    private val producerPackage = FqName("dev.tonholo.ssrcompose.producer")
    private val composePackage = FqName("androidx.compose.runtime")
    private val composeInternalPackage = FqName("androidx.compose.runtime.internal")

    val composable = ClassId(composePackage, Name.identifier("Composable"))
    val composableFunction0 = ClassId(composeInternalPackage, Name.identifier("ComposableFunction0"))
    val remoteAction1 = ClassId(producerPackage, Name.identifier("RemoteAction1"))
    val consumerComponent = CallableId(producerPackage, Name.identifier("ConsumerComponent"))
    val arrayOf = CallableId(FqName("kotlin"), Name.identifier("arrayOf"))
}
