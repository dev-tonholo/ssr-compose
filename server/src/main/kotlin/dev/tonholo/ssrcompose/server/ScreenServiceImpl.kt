package dev.tonholo.ssrcompose.server

import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService

class ScreenServiceImpl(private val screens: Map<String, suspend () -> ScreenNode>) : ScreenService {
    override suspend fun screen(name: String): ScreenNode {
        val build = requireNotNull(screens[name]) { "Unknown screen: $name" }
        return build()
    }
}
