package dev.tonholo.ssrcompose.app.presentation

import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService

class FakeScreenService(var result: Result<ScreenNode>) : ScreenService {
    override suspend fun screen(name: String): ScreenNode = result.getOrThrow()
}
