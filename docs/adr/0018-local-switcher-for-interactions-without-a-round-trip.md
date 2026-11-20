# 0018. A local switcher is the minimal interaction with no round trip

- Status: Accepted
- Date: 2026-11-20
- Modules: `core`, `producer`, `consumer`

## Context

Principle III says the document format must support interactions that need no round trip to the producer. With documents in Feature 1 the owner chose on 2026-11-20 to add a minimal local state now, not later. The proof of concept showed one: a toggle that switched between two looks on the device, with no message to the server.

## Options

1. Defer local interactions to a later item, under a new contract version. Turned down by the owner.
2. A switcher node: the producer composes and lays out every variant of a region, and sends them all; a click marked as local picks the variant on the device. Small and fully measured on the producer. It leaves open what happens to the content around the switcher when variants differ in size; the review of 2026-11-20 found that gap, and the owner chose how to close it (Decision).
3. A general local state model: local variables, expressions and bindings evaluated on the device. Powerful, but it is a small language to design, validate and secure.

## Decision

Option 2, accepted by the owner on 2026-11-20, within the owner's choice of a minimal local state node. Server developers write `LocalSwitcher(variants = 2) { variant -> ... }` and mark a click with `Modifier.switchTo(variant)`. Scrolling inside a scroll container is also local, with no round trip.

Sizing, chosen by the owner on 2026-11-20 after the review: a fixed box sized to the largest variant at once, then the producer catches up. A `Switcher` node's box is the size of its `selected` variant, the one the producer laid the screen out with, and `largestWidth` and `largestHeight` give the size of its largest variant. A tap on a `Local` click shows the chosen variant at once, in a fixed box of the largest variant's size anchored at the switcher's top-left corner and drawn above the nodes after it, and sends `Switched(switcher, variant)`. The producer makes that variant the selected one, lays the screen out again and sends a new revision in which the switcher's box and the content around it are placed for that variant. Until that revision arrives, a larger variant can cover the content below it and a smaller one leaves a gap: the device moves nothing on its own. The device keeps its choice by switcher id across updates, also when a revision still names the older selected variant.

The options for sizing were: (a) a box always the size of the largest variant, so nothing around it ever moves and collapsing leaves empty space; (b) each variant carries the new boxes of every node it moves, laid out by the producer once per variant, which cannot combine two switchers that move the same nodes without one layout per combination of variants; (c) the fixed box at once, then `Switched` and a new layout. The owner chose (c).

So "expand and collapse" means: the expanded or collapsed variant shows at once, and the content around it moves into place one round trip later, when the producer's new revision arrives (under 300 ms on a local network, SC-006). The consumer never re-flows anything.

## Consequences

- Good: the PoC toggle works with no server code running on a tap.
- Good: every variant is measured by the producer, so nothing on the device decides a layout.
- Bad: an app component inside a variant is refused in this version, since a variant the device does not show is never measured.
- Bad: every variant is sent, so a switcher with many large variants makes the document larger. A switcher holds at most 16 variants.
- Bad: the device's chosen variant is kept across updates only while the switcher keeps its node id.
- Bad: for one round trip after a switch, a larger variant covers the content below it, or a smaller one leaves a gap.
- Bad: each switch costs one `Switched` message and one layout on the producer. `Switched` counts against the trigger limit (100 per second per session by default). Over that limit it is not dropped like a trigger: the producer keeps the latest variant of each switcher and applies it when the next second starts, so the producer and the device never stay out of step.
- Follow-up: the switcher slice of R20.
