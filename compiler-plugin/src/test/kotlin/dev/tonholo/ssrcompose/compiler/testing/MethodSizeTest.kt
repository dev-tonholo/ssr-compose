package dev.tonholo.ssrcompose.compiler.testing

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class MethodSizeTest {
    @TestFactory
    fun `given a method descriptor, when its parameter units are counted, then wide types count twice`() =
        mapOf(
            "(Lpkg/abc)def;J)V" to 3,
            "([J)V" to 1,
            "([[DJ)V" to 3,
            "()V" to 0,
        ).map { (descriptor, units) ->
            dynamicTest(descriptor) {
                // Arrange
                val method = MethodSize(
                    name = "m",
                    descriptor = descriptor,
                    codeLength = 0,
                    maxStack = 0,
                    maxLocals = 0,
                )

                // Act
                val counted = method.parameterUnits

                // Assert
                assertEquals(expected = units, actual = counted)
            }
        }
}
