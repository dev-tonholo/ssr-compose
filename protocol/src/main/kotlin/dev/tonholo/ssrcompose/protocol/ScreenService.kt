package dev.tonholo.ssrcompose.protocol

import kotlinx.rpc.annotations.Rpc

@Rpc
interface ScreenService {
    suspend fun screen(name: String): ScreenNode
}
