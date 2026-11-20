# 0016. App components are measured on the device and reported to the producer

- Status: Accepted
- Date: 2026-11-20
- Modules: `core`, `producer`, `consumer`

## Context

With [ADR 0015](0015-producer-renders-with-compose-ui-foundation.md) the producer measures and places everything a screen shows. An app component, a hole in the server's content, is drawn by the app's own code, so only the device knows its size. Its slots hold server content again, and that content needs to know how much space the app component gives each slot.

A probe on 2026-11-20 showed that a producer `Layout` whose height comes from state lays out again when that state changes, and moves the text below it from 33 to 333 px.

## Options

1. The producer assigns each app component a fixed box, and the component must fit it. No round trip, but a component that sizes itself to its content is clipped or stretched, and its native look suffers.
2. The producer gives each app component its maximum constraints. The consumer measures the component within them and reports its size, and the space each slot gets, in a `ComponentMeasured` message. The producer feeds the sizes into the hole's `Layout`, lays out the slot content in the reported space, and sends the screen again. Cost: one round trip each time a hole's size changes.
3. The app developer declares a size for each component in the catalog. Static, and it cannot follow content or font scale.

## Decision

Option 2, chosen by the owner on 2026-11-20. It keeps the app's components at their own size and still leaves every placement to the producer.

## Consequences

- Good: an app component keeps its own measured size, and so its native look, theme and accessibility.
- Good: slot content is laid out by the producer in the space the app gives the slot, so two-way referencing works.
- Bad: a screen with app components needs one round trip before it is shown, and one more each time a hole's size changes. The consumer does not show a screen until the producer sent it with every hole measured.
- Bad: a component inside a slot needs one more round per level of nesting, and the screen stays pending until every component has a reported size. A component whose size depends on its slot content, and the other way round, can keep changing; once all are measured, the producer stops after 4 more rounds for one change and sends the latest reported sizes (R20), so no screen shows a component with no reported size.
- Follow-up: `ComponentMeasured` in the messages contract, and a measuring round limit in `ProducerLimits`.
