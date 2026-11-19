package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.handshake.AppIdentity
import dev.tonholo.ssrcompose.core.handshake.DeviceContext

/**
 * The connected app of the current session.
 *
 * [device] is state: when the app reports a change, code that reads [device] recomposes and remembered state stays.
 */
@Stable
class Consumer internal constructor(
    device: DeviceContext,
    val app: AppIdentity,
    val catalogVersion: CatalogVersion,
) {
    var device: DeviceContext by mutableStateOf(value = device)
        internal set
}
