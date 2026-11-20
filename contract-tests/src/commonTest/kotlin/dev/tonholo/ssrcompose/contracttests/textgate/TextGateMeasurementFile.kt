package dev.tonholo.ssrcompose.contracttests.textgate

import kotlinx.serialization.Serializable

@Serializable
data class TextGateMeasurementFile(
    val schemaVersion: Int,
    val deviceModel: String,
    val sdkInt: Int,
    val measurements: List<TextGateMeasurement>,
)
