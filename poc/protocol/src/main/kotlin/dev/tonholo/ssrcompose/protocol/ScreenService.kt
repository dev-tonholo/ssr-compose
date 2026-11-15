package dev.tonholo.ssrcompose.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ScreenService {
    suspend fun screen(name: String): ScreenNode

    fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode>
}
