# 0014. One opt-in marker for the library's internal API, in core

- Status: Accepted
- Date: 2026-11-19
- Modules: `core`, `producer`, `consumer`, `compiler-plugin`, `contract-tests`

## Context

Kotlin's `internal` does not cross modules, so `core` makes some types public only because `consumer` and `producer` use them: the messages, the codec, the frame layout, `PlacedComponent` and `ScreenContent` ([data-model.md](../../specs/001-consumer-catalog-bridge/data-model.md#what-is-public-in-core)). Issue #194, approved on 2026-11-19, puts them behind an opt-in marker, "as T048 does in `producer`".

T048 ([research.md R18](../../specs/001-consumer-catalog-bridge/research.md)) adds `@InternalSsrComposeApi` to `producer`, at `RequiresOptIn.Level.WARNING`, for the declarations that only bridges call: `ConsumerComponent`, the list helpers and `UnplaceableBridge`. The compiler plugin attaches `@OptIn` for it to every created bridge, and a hand-written bridge opts in itself. The owner approved that marker and its level.

A probe on 2026-11-19 ([research.md R20](../../specs/001-consumer-catalog-bridge/research.md#r20-the-content-model-seam-194)) showed that a use inside the declaring module needs an opt-in too, and that an `@OptIn` on a public declaration hides a marked type in its signature without a warning.

## Options

1. One marker, `InternalSsrComposeApi`, moved from `producer` to `core` (`dev.tonholo.ssrcompose.core.annotation`), at `WARNING`, on the second group of `core` types and on R18's `producer` declarations. One name, one level, one class id for the compiler plugin, and an `@OptIn` on created code covers any marked type it names. Costs: T048's marker moves one module (imports in `producer`, one class id in `ProducerNames`), and a hand-written bridge that opts in also reaches the wire types without a warning.
2. Two markers: R18's marker stays in `producer`, and `core` gets its own, which could be `ERROR`, since no app or server developer should use wire types at all. It keeps the two audiences apart. Costs: the `core` marker cannot also be named `InternalSsrComposeApi` (two public annotations with one name in two modules break the one definition rule), so either the new one gets a weaker name or R18's approved marker is renamed; two markers to learn, and the compiler plugin must know which one created code needs.
3. No marker in `core`; rely on KDoc and the public list. Costs: nothing tells a developer that the wire types are not for them, against #194 and principle VIII.

How our own modules opt in, for options 1 and 2:

- a. Once per module, in the build file, with `compilerOptions.optIn`, in `core`, `consumer`, `producer` and `contract-tests` only. One line each; it also hides an accidental public exposure, which a grep check covers.
- b. `@file:OptIn` in every file that names a marked type, main and test. Each use shows at the top of its file, but no more than option a shows a public exposure, and it adds a line to each file that names one, 26 files on 3c101dd4.

## Decision

Option 1 with opt-in a, chosen by the owner on 2026-11-19. One marker keeps one rule for everything that is public only for the library and for bridges, and the name already says what it is. Our own modules are the users it is meant for, so they opt in once. Samples, compile-test sources and user builds never opt in module-wide.

## Consequences

- Good: one marker, one level and one class id; created code needs one `@OptIn`.
- Good: app and server developers get a warning on any wire type or bridge entry point they call by hand.
- Bad: a `WARNING` does not fail the build, so a compile test in `compiler-plugin` counts the warnings.
- Bad: module-wide opt-in in our modules can hide a marked type in a public signature; a grep over the public signatures of `consumer`, `producer` and `core-testing` checks it (R20 invariant 9).
- Follow-up: #194 slice 1 moves the marker after T048 PR 1 merges.
