# 0021. The producer runs every scene on one scene thread

- Status: Proposed
- Date: 2026-11-21
- Modules: `producer`

## Context

T122 part B hosts each screen in an `ImageComposeScene` of Compose Multiplatform 1.12.2 desktop ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#scenes-and-threads)). Each session drove its scenes from its own thread. Building scenes on several threads at once corrupts process-wide state of Compose UI, and the test "100 idle screens on their own threads, every state changes at once" failed: in 5 runs, 1 `ArrayIndexOutOfBoundsException`, 1 pass and 1 hang with 8 threads spinning in `ObjectIntMap.findKeyIndex`.

Compose UI is built for one UI thread per process. Its source holds state for the whole process with no lock:

- `NodeKind.kt` line 214: `private val classToKindSetMap = mutableObjectIntMapOf<Any>()`, written by `calculateNodeKindSetFrom` the first time a modifier node class is seen.
- `NodeCoordinator.kt` lines 1649 to 1654: one `graphicsLayerScope`, one `tmpLayerPositionalProperties` and one `tmpMatrix` in the companion object, reused by every layer update of every scene.
- `GraphicsLayerModifier.kt` line 825: one `reusableGraphicsLayerScope`, reset and filled when the semantics of a graphics layer are read.

A process-wide lock around scene create, render, read and close fixed the corruption (4 of 4 passes). Composition runs inside `render()`, so a composable that blocks holds the lock and stops every other screen. Each screen also waits for the lock on a `Dispatchers.Default` worker, so a blocked composition parks every Default worker: with 12 cores, 24 waiting screens and one blocked composition, a coroutine launched on `Dispatchers.Default` did not run for 3 seconds (R20 probe P15). That stops all Default work in the server, not only screens.

Composition cannot be split from the node work with public API. The scene owns its recomposer (`FrameRecomposer.skiko.kt` line 88, `private val recomposer = Recomposer(...)`), which composes and applies in one call (line 114, `runRecomposeAndApplyChanges()`). `LayoutNode` is `internal` (`LayoutNode.kt` line 74), so the producer cannot own a composition that builds UI nodes. `SubcomposeLayoutState.createPausedPrecomposition` covers only a first composition, never a recomposition, and promises nothing about threads.

The owner asked for a design that is correct (no corruption and no hang), states its isolation for a blocked composition, and states its throughput against SC-005 and SC-011.

## Options

Throughput is from R20 probe P17: on one thread, a frame of a 50-row screen took 2.2 to 2.5 ms of CPU and a new 50-row screen 6.2 to 7.0 ms of CPU and 20 to 22 ms of wall time. That is about 400 frames or 45 new screens per second for one thread.

1. A process-wide lock, scenes driven from each session's own thread, as T122 part B built it. Correct. A blocked composition stops every screen and, through the parked Default workers, every coroutine on `Dispatchers.Default`. Throughput: one thread's worth, since the lock lets one scene work at a time; 12 threads behind the lock measured the same as one thread (2.3 to 2.5 ms per frame).
2. Option 1 with a watchdog that reports a screen holding the lock too long. Same guarantee and throughput as option 1, and the server developer learns which screen blocked.
3. One scene thread for the whole process. Every scene is created, driven, read and closed on it, and its effects run on it. Correct by confinement, which is how Compose UI runs on Android and desktop. A blocked composition stops every screen of the process, but no other thread waits, so the rest of the server keeps running. Throughput: one thread, the same as options 1 and 2.
4. A small fixed pool of scene threads behind one lock. Same guarantee as option 1 and no more throughput, since the lock lets one thread work at a time. The other threads only wait.
5. Compose on a recomposer the producer owns, then apply under the lock. Not possible with public API, as the context says.
6. Warm the cache: build every modifier node class once on one thread, then run scenes on many threads with no lock. A probe passed 5 of 5 warm runs with no wrong output (P14), against 1 hang in 3 cold runs. Rejected: a node class first seen after warming still writes the map, and the shared layer scope, matrix and positional properties are written by every layer update with no lock, so a race gives wrong layer values with no error. Nothing documents Compose UI as thread safe, so a Compose upgrade can add more such state.
7. Isolated class loaders, one copy of Compose per lane, each lane on its own thread. It works: 4 lanes ran cold at once with no lock and no error, each with its own copy of `NodeKindKt`, and a blocked lane left another lane free (two changes in 12.1 ms, P18). A blocked composition stops only the screens of its lane, 1 in N. Throughput measured 1.6 to 1.7 times one lane with 2 lanes and 1.2 to 1.3 times with 4, since CPU per frame rose from 2.3 ms to 5.2 ms. Each lane costs about 11 MB of metaspace. The cost that decides it: the server's screen code, its state holders and the producer's own API must load once per lane, so a server singleton exists once per lane, and a Compose or producer object passed between lanes fails with a type error.
8. Several producer processes behind one server. Each process has its own Compose, so a blocked composition stops only the screens of its process. 4 processes at once measured 2.1 times one process (P19). As a library feature it needs the server's screen code to run in child processes and a transport between them, which ADR 0015 and rule 9 keep out of the library. As deployment it needs nothing from the library: a server already runs as several instances behind a load balancer, and sessions share nothing.

## Decision

Option 3 with the watchdog of option 2, proposed for the owner's choice:

- One scene thread per process, `ssr-compose-scene`, a daemon thread that starts with the first scene. Every scene step runs on it: create, render, read, close, and the scene's effects. The process-wide lock stays as a guard, so a scene driven from another dispatcher by mistake is still safe; on the scene thread it is never contended.
- A watchdog reports a screen that holds the scene thread longer than `ProducerLimits.sceneTaskLimit` (1 second by default) through `ProducerObserver.onSceneThreadHeld(screen, heldFor)`, once per task. It cannot take the thread back.
- The server developer documentation states the rule Compose already has: composition never blocks. Blocking work goes in an effect that leaves the scene thread, for example `LaunchedEffect` with `withContext(Dispatchers.IO)`. Probe P16: an effect that slept 1.5 seconds that way let another screen show two changes in 21.8 ms; a composition that slept 1.5 seconds held them back for 1,506 ms.
- Throughput and isolation grow by running more producer processes. The library guarantees that nothing is shared between processes, and T129 measures one process.

It is the only correct option that keeps the rest of the server running when a composition blocks, at the same throughput as the lock, with no new mechanism beyond a dispatcher and a watchdog. Option 7 is the only in-process way to limit a blocked composition to part of the screens, and its gain is small for its cost.

## Consequences

- Good: no corruption and no hang, by the same rule Compose UI follows on every platform.
- Good: a blocked composition stops screens only. Sessions, the handshake and any other coroutine of the server keep running, and the server developer is told which screen blocked.
- Good: one place to measure scene work, so T129 reports what one process holds.
- Bad: no isolation between the screens of one process for a composition that blocks. A server developer who blocks in composition stops every screen of that process until it returns.
- Bad: one process uses one core for scenes: about 400 frames of a 50-row screen or 45 new 50-row screens per second, measured on a loaded machine (load average 16 to 25 on 12 cores). A burst of new sessions above that waits, which T129 measures against SC-005. Holding 10,000 open screens (SC-011) costs memory, not scene time, while they are idle.
- Bad: `ProducerLimits` and `ProducerObserver` gain a member each, a public API change.
- Follow-up: T122 drives scenes on the scene thread; the watchdog is its own slice; T037's two concurrency tests change; T129 measures frames per second, new screens per second and the time to first screen at a stated arrival rate on an idle machine.
