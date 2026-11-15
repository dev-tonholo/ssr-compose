package dev.tonholo.ssrcompose.contracttests

import dev.tonholo.ssrcompose.consumer.ConsumerModule
import dev.tonholo.ssrcompose.producer.ProducerModule
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildExperimentTest {
    @Test
    fun `given the consumer and producer modules, when their names are read, then both are visible to the contract tests`() {
        // Arrange
        val consumer = ConsumerModule()
        val producer = ProducerModule()

        // Act
        val names = listOf(consumer.name, producer.name)

        // Assert
        assertEquals(expected = listOf("consumer", "producer"), actual = names)
    }
}
