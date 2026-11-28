# 0028. A live session wakes only when its own scene reports a change

- Status: Accepted on 2026-11-27 by the owner
- Date: 2026-11-27
- Modules: `producer`

## Context

`LiveScreen` hosts each session's screen in its own scene on the one scene thread ([ADR 0021](0021-producer-runs-every-scene-on-one-scene-thread.md)). Today it learns about new work from a dispatcher that reports every task handed to the scene, and then renders and reads whenever the scene's `hasPendingWork()` is true. Two bugs come from that rule ([research.md R20, "Waking and fonts for live sessions"](../../specs/001-consumer-catalog-bridge/research.md#waking-and-fonts-for-live-sessions-241-242)):

- #241: one state change in one session renders and reads every live session (300 of 300 on the reference laptop). Compose 1.12.2 gives every scene its own global write observer, so every write dispatches to every scene and wakes it. Every scene's `Recomposer` also hears every change to a state that any composition of the process read, and holds it as pending until its loop runs. The woken sessions check `hasPendingWork()` in that gap, see it true, and render. Probe P39 counted it: 20 of 20 unrelated sessions rendered, and the true terms were the recomposer's pending changes and its queued loop, never a frame request.
- #242: a screen misses a change it reads only while placing or in a layer when another screen's effect writes it in `Snapshot.withMutableSnapshot`. Compose applies that change inline on the scene thread and asks the screen's scene for a layout through its invalidation callback, which the host ignores, so nothing wakes the screen. P39: 0 of 5 for a placement read and 0 of 5 for a layer read.

Designing the fix found two more misses of the same kind (P40): a state read only in `Modifier.semantics { }`, and a text color read through a `ColorProducer`, which only drawing and the reader read.

## Options

1. Keep the dispatch wake, and let queued work run before checking. Every write still wakes every session (one dispatch and one check each), the result rests on the order of the scene thread's queue, and #242 stays open.
2. Wake on the scene's own signals: the `invalidate` callback of `FrameRecomposer`, the scene's `invalidateLayout` and `invalidateDraw`, the semantics listener's `onSemanticsChange`, and a change to a state the session's last read read, observed by one `SnapshotStateObserver` per session. The dispatcher no longer wakes. Each signal is Compose's own, filtered per scene by Compose: a recomposer asks for a frame only after it finds a composition of its own invalidated, and each owner's snapshot observer invalidates only for states that owner read.
3. One `FrameRecomposer` for every session. One write observer and one recomposer for the process, but a frame then recomposes every invalidated session together, pending work is no longer per session, and the seam changes shape.
4. Filter the recomposer's pending changes by what each scene read. Its pending set is private, so this needs reflection on internal Compose code.

## Decision

Option 2, accepted by the owner on 2026-11-27. A live session renders and reads only when its own scene reports a change or a state its last read read changes. It is the only option that fixes #241 and #242 with one rule, with callbacks Compose already calls, behind the existing compat seam. In a scratch prototype it took the unrelated renders from 20 of 20 to 0 of 20, opening 20 sessions from 10.0 to 2.0 frames each, and fixed every missed-change case (P39, P40); the producer suite passed with it, 189 of 189 (P45).

## Consequences

- Good: a change renders and reads only the sessions it touches. Opening costs about 2 frames per session instead of half the number of sessions open, and memory per live session falls, since repeated reads no longer leave garbage paragraphs. The figures, on a Mac (P43) and on the reference laptop (P47), are in [R20, "4. Constraints and numbers"](../../specs/001-consumer-catalog-bridge/research.md#4-constraints-and-numbers).
- Good: changes read only while placing, in a layer, in semantics or by the reader reach the session with no other event.
- Bad: the host now depends on three Compose callbacks behind the seam (`FrameRecomposer`'s `invalidate`, `invalidateLayout`, `invalidateDraw`) and on the semantics listener; the conformance suite must pin each one on every Compose upgrade.
- Bad: each session adds one apply observer for its reader. Each global write still costs one task per open scene on the scene thread for Compose's own write observer and recomposer loop, now without a render; its cost up to 2,000 sessions is in [R20](../../specs/001-consumer-catalog-bridge/research.md#4-constraints-and-numbers) (P41), and T129 measures 10,000.
- Follow-up: the slices in R20, "Waking and fonts for live sessions", after #227 C1 merges; the laptop package `ssrcompose-241-wake`.
