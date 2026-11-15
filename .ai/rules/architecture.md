# Architecture

Deliverables, module boundaries and shared contracts. The source is principles I, II, III, IV, VIII and IX of the [constitution](../../.specify/memory/constitution.md).

## Rules

1. The project ships four things: a consumer library, a producer library, a core library, and a Kotlin compiler plugin. The root build has their module shapes (`core`, `consumer`, `producer`, `compiler-plugin`), with one trivial class each and no feature code yet. Do not add feature code to one without a spec.
2. Everything both sides agree on has one definition, in the core library: document format, catalog, handshake, messages and action bindings.
3. The consumer library never depends on the producer library, and the producer library never depends on the consumer library. Both depend on core only.
4. The core library does not depend on Android, a server framework or a transport.
5. The producer library runs on a plain JVM. It never needs Android classes, stubs or an emulator.
6. Shipped artifacts never depend on AndroidX Remote Compose. The document format is ours.
7. The producer never sends a reference to a component that is not in the consumer's catalog.
8. The contract is versioned. A producer keeps serving every consumer version it still supports, and refuses the others with a clear error.
9. No public API exposes a transport type, or a third-party type that is alpha, experimental or restricted.
10. Keep public API small. Implementations are internal, behind interfaces or factories. Inject dependencies through constructors and depend on abstractions.
11. A public API change is its own piece of work. Never mix it into a cleanup.
12. Library code never depends on `poc/`. Carry a lesson over through a spec, not by copying code.
13. A library or architecture choice gets an ADR. See [the ADR blueprint](../blueprints/adr.md).

## Known violations (do not copy them)

The proof of concept breaks several of these on purpose. It proved the idea and is kept as a reference only.

- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/tree/RemoteFragment.kt` and `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/presentation/ui/components/FragmentPlayer.kt` depend on AndroidX Remote Compose, including restricted API.
- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/screen/CounterBanner.kt` sizes a document in raw pixels.
- `poc/protocol/src/main/kotlin/dev/tonholo/ssrcompose/protocol/ScreenService.kt` has no version, no handshake and no catalog.
- `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server/tree/Reference.kt` needs a hand-written server stub and ID per shared component.
- `poc/protocol/build.gradle.kts` puts kotlinx.rpc, a transport, in the shared contract.

## Check

- A new module's build file does not name the other side's library.
- `grep -rn 'androidx.compose.remote' --include='*.kt' --include='*.kts' --include='*.toml' .` only finds files under `poc/`.
- Each new library or architecture choice has a file in `docs/adr/` and a row in its index.

Templates: [adr](../blueprints/adr.md). Agent: [reviewer](../agents/reviewer.md).
