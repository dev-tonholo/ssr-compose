package dev.tonholo.ssrcompose.server

import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ScreenServiceImpl(private val screens: Map<String, suspend () -> ScreenNode>) : ScreenService {
    override suspend fun screen(name: String): ScreenNode {
        val build = requireNotNull(screens[name]) { "Unknown screen: $name" }
        return build()
    }

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> = flow { emit(screen(name)) }
}
