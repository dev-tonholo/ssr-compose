/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified for ssr-compose from ImageComposeScene.skiko.kt of Compose Multiplatform 1.12.2: it lays the content out and
// never draws it, so it has no raster surface, and it takes no input.

package dev.tonholo.ssrcompose.producer.internal.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import kotlin.coroutines.CoroutineContext

/**
 * A [SceneHost] over Compose UI desktop's scene API. It composes, measures and places a screen, and never draws it.
 */
@OptIn(InternalComposeUiApi::class)
internal class SkikoSceneHost(
    setup: SceneSetup,
    coroutineContext: CoroutineContext,
    onInvalidated: () -> Unit,
) : SceneHost {
    private val sceneSize = IntSize(width = setup.widthPx, height = setup.heightPx)

    private val sceneWindowInfo = object : WindowInfo {
        override val isWindowFocused = true
        override val containerSize = sceneSize
        override val containerDpSize = with(setup.density) {
            DpSize(width = sceneSize.width.toDp(), height = sceneSize.height.toDp())
        }
    }

    private val windowRoots = mutableListOf<SemanticsOwner>()

    private val windowRootsListener = object : PlatformContext.SemanticsOwnerListener {
        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
            windowRoots.add(semanticsOwner)
        }

        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
            windowRoots.remove(semanticsOwner)
        }

        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) = Unit

        override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) = Unit
    }

    private val platformContext = object : PlatformContext by PlatformContext.Empty() {
        override val windowInfo: WindowInfo get() = sceneWindowInfo
        override val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener get() = windowRootsListener
    }

    private val frameRecomposer = FrameRecomposer(coroutineContext = coroutineContext, invalidate = onInvalidated)

    /*
     * Only drawing clears the scene's own pending draw flag, so that flag stays set here. A layer that moves asks for
     * nothing but a redraw, so a request made after the last frame is what says there is something new to read.
     */
    @Volatile
    private var redrawRequestedSinceFrame = false

    private val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        density = setup.density,
        layoutDirection = setup.layoutDirection,
        size = sceneSize,
        platformContext = platformContext,
        invalidateLayout = onInvalidated,
        invalidateDraw = {
            redrawRequestedSinceFrame = true
            onInvalidated()
        },
    )

    override fun setContent(content: @Composable () -> Unit) {
        scene.setContent(content = content)
    }

    override fun renderFrame(nanoTime: Long) {
        frameRecomposer.performFrame(frameTimeNanos = nanoTime)
        scene.measureAndLayout()
        settleStateWrittenWhileLayingOut()
        redrawRequestedSinceFrame = false
    }

    override fun semanticsRoots(): List<SemanticsNode> = windowRoots.map { owner -> owner.unmergedRootSemanticsNode }

    override fun hasPendingWork(): Boolean =
        frameRecomposer.hasPendingWork() || scene.hasPendingMeasureOrLayout || redrawRequestedSinceFrame

    override fun close() {
        scene.close()
        frameRecomposer.close()
    }

    private fun settleStateWrittenWhileLayingOut() {
        Snapshot.sendApplyNotifications()
        scene.measureAndLayout()
        Snapshot.notifyObjectsInitialized()
    }
}
