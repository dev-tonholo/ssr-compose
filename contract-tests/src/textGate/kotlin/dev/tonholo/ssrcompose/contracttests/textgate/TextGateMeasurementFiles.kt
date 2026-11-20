package dev.tonholo.ssrcompose.contracttests.textgate

import kotlinx.serialization.json.Json

/**
 * Reads and writes the file the device half records and the producer half compares against.
 */
object TextGateMeasurementFiles {
    const val SCHEMA_VERSION = 1
    const val FILE_NAME = "text-gate-measurements.json"

    private val json = Json { prettyPrint = true }

    fun encode(file: TextGateMeasurementFile): String = json.encodeToString(value = file)

    /**
     * @throws IllegalArgumentException when the file has another schema version.
     * @throws kotlinx.serialization.SerializationException when a field is missing or has the wrong type.
     */
    fun decode(text: String): TextGateMeasurementFile {
        val file = json.decodeFromString<TextGateMeasurementFile>(string = text)
        require(file.schemaVersion == SCHEMA_VERSION) {
            "Unsupported text gate file version ${file.schemaVersion}, expected $SCHEMA_VERSION"
        }
        return file
    }
}
