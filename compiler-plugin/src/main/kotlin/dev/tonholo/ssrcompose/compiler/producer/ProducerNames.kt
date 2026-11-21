package dev.tonholo.ssrcompose.compiler.producer

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal object ProducerNames {
    private val producerPackage = FqName("dev.tonholo.ssrcompose.producer")
    private val foundationTextPackage = FqName("androidx.compose.foundation.text")
    private val composeInternalPackage = FqName("androidx.compose.runtime.internal")
    private val coreAnnotationPackage = FqName("dev.tonholo.ssrcompose.core.annotation")
    private val coreCatalogPackage = FqName("dev.tonholo.ssrcompose.core.catalog")
    private val coreDataPackage = FqName("dev.tonholo.ssrcompose.core.data")
    private val kotlinPackage = FqName("kotlin")
    private val collectionsPackage = FqName("kotlin.collections")

    val composableFunction0 = ClassId(composeInternalPackage, Name.identifier("ComposableFunction0"))
    val composableFunction1 = ClassId(composeInternalPackage, Name.identifier("ComposableFunction1"))
    val remoteAction1 = ClassId(producerPackage, Name.identifier("RemoteAction1"))
    val unplaceableBridge = ClassId(producerPackage, Name.identifier("UnplaceableBridge"))
    val internalSsrComposeApi = ClassId(coreAnnotationPackage, Name.identifier("InternalSsrComposeApi"))
    val optIn = ClassId(kotlinPackage, Name.identifier("OptIn"))
    val markerClass: Name = Name.identifier("markerClass")
    val message: Name = Name.identifier("message")
    val consumerComponent = CallableId(producerPackage, Name.identifier("ConsumerComponent"))
    val basicText = CallableId(foundationTextPackage, Name.identifier("BasicText"))
    val scaledBasicText = CallableId(producerPackage, Name.identifier("ScaledBasicText"))
    val catalogVersion = ClassId(coreCatalogPackage, Name.identifier("CatalogVersion"))
    val textValue = ClassId(coreDataPackage, FqName("DataValue.Text"), isLocal = false)
    val pair = ClassId(kotlinPackage, Name.identifier("Pair"))
    val to = CallableId(kotlinPackage, Name.identifier("to"))
    val mapOf = CallableId(collectionsPackage, Name.identifier("mapOf"))
    val emptyMap = CallableId(collectionsPackage, Name.identifier("emptyMap"))
    val error = CallableId(kotlinPackage, Name.identifier("error"))
}
