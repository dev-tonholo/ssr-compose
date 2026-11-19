# Architecture

Deliverables, module boundaries and shared contracts. The source is principles I, II, III, IV, VIII and IX of the [constitution](../../.specify/memory/constitution.md).

## Rules

1. The project ships four things: a consumer library, a producer library, a core library, and a Kotlin compiler plugin. The root build has their module shapes (`core`, `consumer`, `producer`, `compiler-plugin`), plus the published test-support module `core-testing`. `core` and `core-testing` hold the messages, the handshake types and the in-memory connection; `consumer`, `producer` and `compiler-plugin` still have one trivial class each and no feature code yet. Do not add feature code to a module without a spec.
2. Everything both sides agree on has one definition, in the core library: document format, catalog, handshake, messages and action bindings.
3. The consumer library never depends on the producer library, and the producer library never depends on the consumer library. Both depend on core only.
4. The core library does not depend on Android, a server framework or a transport. It never depends on `core-testing`; other modules add `core-testing` as a test dependency only.
5. The producer library runs on a plain JVM. It never needs Android classes, stubs or an emulator.
6. Shipped artifacts never depend on AndroidX Remote Compose. The document format is ours.
7. The producer never sends a reference to a component that is not in the consumer's catalog.
8. The contract is versioned. A producer keeps serving every consumer version it still supports, and refuses the others with a clear error.
9. No public API exposes a transport type, or a third-party type that is alpha, experimental or restricted.
10. Keep public API small. Implementations are internal, behind interfaces or factories. Inject dependencies through constructors and depend on abstractions. Every published module builds in Kotlin explicit API mode, turned on by the publishing convention plugin, so every public declaration says `public` and its type.
11. A public API change is its own piece of work. Never mix it into a cleanup.
12. Library code never depends on `poc/`. Carry a lesson over through a spec, not by copying code.
13. A library or architecture choice gets an ADR. See [the ADR blueprint](../blueprints/adr.md).
14. The core is server-side rendering, never server-driven rendering (principle I). The wire content model (`ScreenContent`, `PlacedComponent.slots` and any screen or slot payload) never hardens a screen or slot that holds only consumer catalog components. The consumer library never owns screen arrangement in its public API. A top-to-bottom fallback stays inside an internal renderer and is marked temporary until server-rendered documents own layout.
15. The zero-catalog path and two-way referencing stay open in every feature. An app with no `@ConsumerComposable` (an empty catalog and an empty renderer index) still connects and renders server screens. Consumer slots that accept `any` or `@ProducerComposable` content stay able to receive server-rendered content. `producer` handles every node kind, so it never filters for catalog components only.

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
- No public KDoc or contract says the app arranges screen components or that structure comes only from consumer composables. `ScreenContent` and `PlacedComponent.slots` are not a bare `List<PlacedComponent>` in a final contract version. `SsrScreen.kt` imports no layout container such as `Column` or `Row`.
- Renderer loading and screen registration have a test for an app with no `@ConsumerComposable`. Generated renderers reach slot content through `PlacedComponentScope.slot`, never by looping over `List<PlacedComponent>`. `PlacedNodeContent` uses an exhaustive `when` over `PlacedNode`, not `filterIsInstance<PlacedNode.Component>()`.

Templates: [adr](../blueprints/adr.md). Agent: [reviewer](../agents/reviewer.md).
