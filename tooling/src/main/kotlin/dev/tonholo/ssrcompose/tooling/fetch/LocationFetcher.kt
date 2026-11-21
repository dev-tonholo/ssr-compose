package dev.tonholo.ssrcompose.tooling.fetch

import dev.tonholo.ssrcompose.tooling.StepException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

private const val FILE_SCHEME = "file"
private const val SECURE_SCHEME = "https"
private const val CLEARTEXT_SCHEME = "http"
private const val TIMEOUT_MILLISECONDS = 30_000
private val SCHEME_PREFIX = Regex(pattern = "^[A-Za-z][A-Za-z0-9+.-]+:")

/**
 * Fetches a location with a size cap. A location is an `https` address, a `file` address, or a file path, which
 * is read from [baseFolder] when it is relative.
 */
public class LocationFetcher internal constructor(
    private val baseFolder: File,
    allowCleartext: Boolean,
) {
    public constructor(baseFolder: File) : this(baseFolder = baseFolder, allowCleartext = false)

    private val allowedSchemes = setOfNotNull(FILE_SCHEME, SECURE_SCHEME, CLEARTEXT_SCHEME.takeIf { allowCleartext })

    /**
     * Reads the whole of [location], refusing it before reading when its size is known to be over [sizeCap], and
     * as soon as more than [sizeCap] bytes arrive otherwise.
     *
     * @param subject what the file is, such as `fonts.json`. Failures name it.
     * @throws StepException when the location's scheme is not accepted or it is over the cap.
     * @throws UnreachableLocationException when the location cannot be reached.
     */
    public fun fetch(location: String, sizeCap: Long, subject: String): ByteArray {
        val uri = parsed(location = location, subject = subject)
        val scheme = uri.scheme.lowercase()
        if (scheme !in allowedSchemes) {
            throw StepException(message = "Only https addresses and file paths are accepted: $subject at $location")
        }
        val source = CappedSource(location = location, sizeCap = sizeCap, subject = subject)
        return when (scheme) {
            FILE_SCHEME -> source.readFile(file = fileOf(uri = uri, location = location, subject = subject))
            else -> source.readUrl(url = uri.toURL())
        }
    }

    /** The location of [fileName], a relative path, in the same folder as [location]. */
    public fun sibling(location: String, fileName: String): String =
        uriOf(location = location).resolve(fileName).toString()

    private fun parsed(location: String, subject: String): URI = try {
        uriOf(location = location)
    } catch (cause: URISyntaxException) {
        throw malformed(location = location, subject = subject, cause = cause)
    }

    private fun fileOf(uri: URI, location: String, subject: String): File = try {
        File(uri)
    } catch (cause: IllegalArgumentException) {
        throw malformed(location = location, subject = subject, cause = cause)
    }

    private fun malformed(location: String, subject: String, cause: Exception) =
        StepException(message = "The location of the $subject is not valid: $location", cause = cause)

    private fun uriOf(location: String): URI = when {
        SCHEME_PREFIX.containsMatchIn(input = location) -> URI(location)
        else -> baseFolder.resolve(location).normalize().toURI()
    }
}

private class CappedSource(val location: String, val sizeCap: Long, val subject: String) {
    fun readFile(file: File): ByteArray {
        if (!file.isFile) throw UnreachableLocationException(message = "The $subject is not found at $location")
        requireWithinCap(size = file.length())
        return try {
            file.inputStream().use { stream -> readCapped(stream = stream) }
        } catch (cause: IOException) {
            throw unreachable(cause = cause)
        }
    }

    fun readUrl(url: URL): ByteArray {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLISECONDS
        connection.readTimeout = TIMEOUT_MILLISECONDS
        return try {
            requireSuccess(connection = connection)
            requireWithinCap(size = connection.contentLengthLong)
            connection.inputStream.use { stream -> readCapped(stream = stream) }
        } catch (cause: IOException) {
            throw cause as? UnreachableLocationException ?: unreachable(cause = cause)
        } finally {
            connection.disconnect()
        }
    }

    private fun unreachable(cause: IOException) =
        UnreachableLocationException(message = "The $subject cannot be fetched from $location", cause = cause)

    private fun requireSuccess(connection: HttpURLConnection) {
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            throw UnreachableLocationException(message = "The $subject at $location answered $code")
        }
    }

    private fun readCapped(stream: InputStream): ByteArray {
        val bytes = stream.readNBytes(sizeCap.coerceAtMost(maximumValue = Int.MAX_VALUE - 1L).toInt() + 1)
        requireWithinCap(size = bytes.size.toLong())
        return bytes
    }

    private fun requireWithinCap(size: Long) {
        if (size > sizeCap) throw StepException(message = "The $subject at $location is larger than $sizeCap bytes")
    }
}
