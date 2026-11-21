package dev.tonholo.ssrcompose.producer.internal.scene

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

internal const val SCENE_THREAD_NAME = "ssr-compose-scene"

/**
 * The producer's one scene thread, a daemon thread started by the first scene. Every scene of the process is
 * created, rendered, read and closed on it, and runs its effects on it, since Compose UI keeps process-wide state
 * with no lock (ADR 0021).
 */
internal val sceneDispatcher: CoroutineDispatcher by lazy {
    Executors.newSingleThreadExecutor { task -> Thread(task, SCENE_THREAD_NAME).apply { isDaemon = true } }
        .asCoroutineDispatcher()
}
