package dev.tonholo.ssrcompose.app.presentation

import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeScreenService(var result: Result<ScreenNode>) : ScreenService {
    override suspend fun screen(name: String): ScreenNode = result.getOrThrow()

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> = emptyFlow()
}
