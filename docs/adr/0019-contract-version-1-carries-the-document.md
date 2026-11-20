# 0019. Contract version 1 carries the server-rendered document

- Status: Accepted
- Date: 2026-11-20
- Modules: `core`

## Context

Contract version 1 ([contracts/messages.md](../../specs/001-consumer-catalog-bridge/contracts/messages.md)) carried screens as a list of placed components. R20 replaces that with a positioned document, adds `ComponentMeasured` and `ViewportChanged`, adds a viewport to `OpenScreen`, adds the font scale and the layout direction to the device context, adds the font version to the introduction, adds `FontVersionChanged` and the font version a `Screen` was laid out with, and adds the refusal kind "font version not supported". The owner accepted these additions in contract version 1 on 2026-11-20. No consumer or producer has been released.

## Options

1. Keep contract version 1 and replace the content model inside it. An older development build fails at its first `Screen` with `AnswerUnreadable`, not at the handshake.
2. Raise the contract version to 2 and refuse version 1. A clean refusal at the handshake, but it spends a version on a draft no released app speaks, and the producer would carry a version it refuses from its first release.

## Decision

Option 1, chosen by the owner on 2026-11-20, as for the content seam on 2026-11-19. Nothing is released, so nothing needs the old shape.

## Consequences

- Good: the first released contract version is the server-rendered one.
- Bad: development builds of either side from before R20 cannot talk to builds after it, and fail at the first screen.
