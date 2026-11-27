package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.util.Properties

private const val HOSTS_RESOURCE = "layoutlib-hosts.properties"

/**
 * The name of layoutlib's native runtime for a host, from the `os.name` and `os.arch` system properties: both the
 * runtime jar's classifier and its native library folder.
 *
 * @throws UnsupportedOperationException when no runtime is set up for the host.
 */
internal fun layoutlibRuntimeName(os: String, arch: String): String {
    val hosts = supportedHosts()
    return hosts.getProperty("$os/$arch") ?: throw UnsupportedOperationException(
        "layoutlib has no runtime set up for $os $arch, only for ${hosts.stringPropertyNames().sorted()}",
    )
}

private fun supportedHosts(): Properties {
    val table = requireNotNull(TextEngine::class.java.getResourceAsStream(HOSTS_RESOURCE)) { "no $HOSTS_RESOURCE" }
    return Properties().apply { table.use(::load) }
}
