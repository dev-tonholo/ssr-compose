# 0001. Keep the proof of concept in a folder and build the library from a clean root

- Status: Accepted
- Date: 2026-11-14
- Modules: `poc/protocol`, `poc/server`, `poc/app`

## Context

The idea behind ssr-compose was proven with a proof of concept: a Ktor server that runs composables and an Android app that renders the result. It was built fast to answer go or no-go questions, and it takes shortcuts the library must not take. It depends on AndroidX Remote Compose, sizes documents in raw pixels, and has no handshake, catalog or versioning.

The library needs a different shape: a consumer library, a producer library, a core library and a compiler plugin, built from specs.

## Options

1. Grow the library out of the PoC code in place. Fast start, but every shortcut becomes something to remove, and the PoC's module layout does not match the four deliverables.
2. Delete the PoC and start clean. The cleanest root, but the working reference and its lessons are lost.
3. Move the PoC into `poc/` as its own Gradle project and start the library from an empty root. The reference still builds and runs, and the root has no legacy to work around. The cost is a second Gradle project to keep building.

## Decision

Option 3. The PoC lives in `poc/` as a reference, and the root is a greenfield for the library. Library code never depends on `poc/`.

## Consequences

- Good: the library starts without the PoC's shortcuts, and the PoC stays runnable to check how something behaved.
- Bad: two Gradle projects, and the PoC can go stale as tools move on.
- Follow-up: lessons from the PoC reach the library through specs, not copied code. The PoC's known violations are listed in the rule files under `.ai/rules/`.
