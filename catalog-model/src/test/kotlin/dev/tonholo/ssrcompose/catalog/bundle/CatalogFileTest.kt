package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogFileTest {
    @Test
    fun `given the hand-written example of the contract, when it is read, then it is the expected catalog`() {
        // Arrange
        val text = CONTRACT_EXAMPLE

        // Act
        val actual = CatalogFile.read(text = text)

        // Assert
        assertEquals(expected = CONTRACT_EXAMPLE_CATALOG, actual = actual)
    }

    @Test
    fun `given abc, when the fingerprint is computed, then it is the known SHA-256 in lowercase hexadecimal`() {
        // Arrange
        val bytes = "abc".encodeToByteArray()

        // Act
        val actual = Fingerprint.of(bytes = bytes)

        // Assert
        assertEquals(expected = SHA256_ABC, actual = actual)
    }

    private companion object {
        const val SHA256_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

        val CONTRACT_EXAMPLE_CATALOG = Catalog(
            version = CatalogVersion(value = "3"),
            choices = listOf(ChoiceList(name = "Style", entries = listOf("bodyMedium", "titleLarge"))),
            components = listOf(
                ComponentDescriptor(
                    name = "CameraScanner",
                    parameters = listOf(
                        Parameter.Slot(
                            name = "cancel",
                            accepts = SlotAccepts.Any,
                            arguments = listOf(ValueKind.Text),
                            required = true,
                        ),
                        Parameter.Callback(name = "onCancelClick", required = true),
                        Parameter.Callback(name = "onScan", arguments = listOf(ValueKind.Bytes), required = true),
                    ),
                ),
                ComponentDescriptor(
                    name = "Greeting",
                    parameters = listOf(
                        Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                        Parameter.Value(name = "style", kind = ValueKind.Choice(listName = "Style"), required = false),
                    ),
                ),
                ComponentDescriptor(
                    name = "PaymentSection",
                    parameters = listOf(
                        Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                        Parameter.Callback(name = "onPayClick", required = true),
                        Parameter.Slot(name = "content", accepts = SlotAccepts.Consumer, required = false),
                    ),
                ),
            ),
        )

        val CONTRACT_EXAMPLE = """
            {
              "format": 1,
              "version": "3",
              "choices": [
                { "name": "Style", "entries": ["bodyMedium", "titleLarge"] }
              ],
              "components": [
                {
                  "name": "CameraScanner",
                  "parameters": [
                    {
                      "name": "cancel", "kind": "slot", "arguments": [{ "kind": "text" }], "accepts": "any",
                      "required": true
                    },
                    { "name": "onCancelClick", "kind": "callback", "required": true },
                    {
                      "name": "onScan", "kind": "callback", "arguments": [{ "kind": "bytes" }], "required": true
                    }
                  ]
                },
                {
                  "name": "Greeting",
                  "parameters": [
                    { "name": "title", "kind": "text", "required": true },
                    { "name": "style", "kind": "choice", "choice": "Style", "required": false }
                  ]
                },
                {
                  "name": "PaymentSection",
                  "parameters": [
                    { "name": "title", "kind": "text", "required": true },
                    { "name": "onPayClick", "kind": "callback", "required": true },
                    { "name": "content", "kind": "slot", "accepts": "consumer", "required": false }
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}
