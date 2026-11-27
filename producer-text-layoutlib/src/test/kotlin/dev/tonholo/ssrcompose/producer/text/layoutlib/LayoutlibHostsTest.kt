package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LayoutlibHostsTest {
    @Test
    fun `given a supported host, when its runtime is asked for, then it is the runtime for that host`() {
        // Arrange
        val hosts = listOf("Mac OS X" to "aarch64", "Linux" to "amd64")

        // Act
        val runtimes = hosts.map { (os, arch) -> layoutlibRuntimeName(os = os, arch = arch) }

        // Assert
        assertEquals(expected = listOf("mac-arm", "linux"), actual = runtimes)
    }

    @Test
    fun `given a host with no layoutlib runtime, when its runtime is asked for, then it is refused by name`() {
        // Arrange
        val hosts = listOf("Windows 11" to "amd64", "Mac OS X" to "x86_64", "Linux" to "aarch64")

        // Act
        val failures = hosts.map { (os, arch) ->
            "$os $arch" to runCatching { layoutlibRuntimeName(os = os, arch = arch) }.exceptionOrNull()
        }

        // Assert
        failures.forEach { (host, failure) ->
            val refusal = assertIs<UnsupportedOperationException>(value = failure, message = host)
            assertTrue(actual = host in refusal.message.orEmpty(), message = refusal.message)
        }
    }
}
