package dev.tonholo.ssrcompose.tooling.fetch

import com.sun.net.httpserver.HttpServer
import dev.tonholo.ssrcompose.tooling.StepException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** Fetching a location with a size cap: https and file paths only, unless cleartext is allowed. */
class LocationFetcherTest {
    @TempDir
    lateinit var base: File

    private var server: HttpServer? = null

    @AfterEach
    fun stopServer() {
        server?.stop(0)
    }

    private fun serve(path: String, body: ByteArray, chunked: Boolean = false): String {
        val running = server ?: HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
            .also { started -> started.start() }
            .also { started -> server = started }
        running.createContext(path) { exchange ->
            exchange.sendResponseHeaders(OK, if (chunked) CHUNKED else body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        return "http://127.0.0.1:${running.address.port}$path"
    }

    private fun letters(size: Int): List<Byte> = ByteArray(size = size) { LETTER }.toList()

    private fun saved(path: String, size: Int): File =
        base.resolve(path).apply { parentFile.mkdirs() }.apply { writeBytes(ByteArray(size = size) { LETTER }) }

    private fun failureOf(fetcher: LocationFetcher, location: String): KClass<out Throwable>? =
        runCatching { fetcher.fetch(location = location, sizeCap = CAP, subject = SUBJECT) }
            .exceptionOrNull()?.let { failure -> failure::class }

    @Test
    fun `given a relative file path and a file URL, when fetched, then each gives the file's bytes`() {
        // Arrange
        val file = saved(path = "app/fonts.json", size = SMALL)
        val fetcher = LocationFetcher(baseFolder = base)

        // Act
        val actual = listOf("app/fonts.json", file.toURI().toString())
            .map { location -> fetcher.fetch(location = location, sizeCap = CAP, subject = SUBJECT).toList() }

        // Assert
        assertEquals(expected = listOf(letters(size = SMALL), letters(size = SMALL)), actual = actual)
    }

    @Test
    fun `given a relative path through a folder that does not exist, when fetched, then it gives the file's bytes`() {
        // Arrange
        saved(path = "app/fonts.json", size = SMALL)
        val fetcher = LocationFetcher(baseFolder = base.resolve("server"))

        // Act
        val actual = fetcher.fetch(location = "../app/fonts.json", sizeCap = CAP, subject = SUBJECT)

        // Assert
        assertEquals(expected = letters(size = SMALL), actual = actual.toList())
    }

    @Test
    fun `given an http address, when fetched without the cleartext opt-in, then it is refused`() {
        // Arrange
        val location = serve(path = "/fonts.json", body = ByteArray(size = SMALL))

        // Act
        val actual = failureOf(fetcher = LocationFetcher(baseFolder = base), location = location)

        // Assert
        assertEquals(expected = StepException::class, actual = actual)
    }

    @Test
    fun `given an http address and the cleartext opt-in, when fetched, then it gives the served bytes`() {
        // Arrange
        val location = serve(path = "/fonts.json", body = ByteArray(size = SMALL) { LETTER })
        val fetcher = LocationFetcher(baseFolder = base, allowCleartext = true)

        // Act
        val actual = fetcher.fetch(location = location, sizeCap = CAP, subject = SUBJECT)

        // Assert
        assertEquals(expected = ByteArray(size = SMALL) { LETTER }.toList(), actual = actual.toList())
    }

    @Test
    fun `given a file, a sized answer and a chunked answer over the cap, when fetched, then each is refused`() {
        // Arrange
        val fetcher = LocationFetcher(baseFolder = base, allowCleartext = true)
        saved(path = "big.ttf", size = CAP.toInt() + 1)
        val sized = serve(path = "/sized.ttf", body = ByteArray(size = CAP.toInt() + 1))
        val chunked = serve(path = "/chunked.ttf", body = ByteArray(size = CAP.toInt() + 1), chunked = true)
        val locations = listOf("big.ttf", sized, chunked)

        // Act
        val actual = locations.map { location -> failureOf(fetcher = fetcher, location = location) }

        // Assert
        assertEquals(expected = locations.map { StepException::class }, actual = actual)
    }

    @Test
    fun `given a malformed location, when fetched, then it is refused naming the subject and the location`() {
        // Arrange
        val fetcher = LocationFetcher(baseFolder = base)
        val locations = listOf("file://host/x", "file:rel", "https://exa mple.com/x")

        // Act
        val failures = locations.map { location ->
            runCatching { fetcher.fetch(location = location, sizeCap = CAP, subject = SUBJECT) }.exceptionOrNull()
        }

        // Assert
        assertEquals(expected = locations.map { StepException::class }, actual = failures.map { it?.let { it::class } })
        locations.zip(failures).forEach { (location, failure) ->
            assertContains(charSequence = failure?.message.orEmpty(), other = SUBJECT)
            assertContains(charSequence = failure?.message.orEmpty(), other = location)
        }
    }

    @Test
    fun `given an https address with an uppercase scheme, when fetched, then it is not refused as another scheme`() {
        // Arrange
        val closedPort = ServerSocket(0).use { socket -> socket.localPort }
        val location = "HTTPS://127.0.0.1:$closedPort/fonts.json"

        // Act
        val actual = failureOf(fetcher = LocationFetcher(baseFolder = base), location = location)

        // Assert
        assertEquals(expected = UnreachableLocationException::class, actual = actual)
    }

    @Test
    fun `given a file of exactly the cap, when fetched, then it is read`() {
        // Arrange
        saved(path = "edge.ttf", size = CAP.toInt())

        // Act
        val actual = LocationFetcher(baseFolder = base).fetch(location = "edge.ttf", sizeCap = CAP, subject = SUBJECT)

        // Assert
        assertEquals(expected = CAP.toInt(), actual = actual.size)
    }

    @Test
    fun `given a missing file, a 404 and a closed port, when fetched, then each location is unreachable`() {
        // Arrange
        val fetcher = LocationFetcher(baseFolder = base, allowCleartext = true)
        val missing = serve(path = "/fonts.json", body = ByteArray(size = SMALL)).replace("/fonts.json", "/other.json")
        val closedPort = ServerSocket(0).use { socket -> "http://127.0.0.1:${socket.localPort}/fonts.json" }

        // Act
        val actual = listOf("missing.json", missing, closedPort).map { failureOf(fetcher = fetcher, location = it) }

        // Assert
        assertEquals(expected = List(size = 3) { UnreachableLocationException::class }, actual = actual)
    }

    @Test
    fun `given a file that cannot be read, when fetched, then the location is unreachable`() {
        // Arrange
        val file = saved(path = "locked.json", size = SMALL)
        file.setReadable(false)
        assumeFalse(file.canRead())

        // Act
        val actual = failureOf(fetcher = LocationFetcher(baseFolder = base), location = "locked.json")

        // Assert
        assertEquals(expected = UnreachableLocationException::class, actual = actual)
    }

    @Test
    fun `given a location, when a file next to it is resolved, then it sits in the same folder`() {
        // Arrange
        val fetcher = LocationFetcher(baseFolder = base)
        saved(path = "app/brand/400-normal.ttf", size = SMALL)

        // Act
        val https = fetcher.sibling(location = "https://example.com/ssr/fonts/1/fonts.json", fileName = FONT_FILE)
        val local = fetcher.sibling(location = "app/fonts.json", fileName = FONT_FILE)

        // Assert
        assertEquals(expected = "https://example.com/ssr/fonts/1/$FONT_FILE", actual = https)
        val fetched = fetcher.fetch(location = local, sizeCap = CAP, subject = SUBJECT)
        assertEquals(expected = letters(size = SMALL), actual = fetched.toList())
    }

    private companion object {
        const val OK = 200
        const val CHUNKED = 0L
        const val CAP = 1_024L
        const val SMALL = 10
        const val LETTER = 'a'.code.toByte()
        const val SUBJECT = "fonts.json"
        const val FONT_FILE = "brand/400-normal.ttf"
    }
}
