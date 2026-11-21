package dev.tonholo.ssrcompose.producer.internal.scene

private val sceneLock = Any()

/** Runs [block] while no other scene in the process creates, renders, reads or closes. */
internal fun <T> withSceneLock(block: () -> T): T = synchronized(sceneLock) { block() }
