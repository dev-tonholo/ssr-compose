# ssr-compose Constitution

## Core Principles

### I. Real SSR, Not SDR

The project delivers server-side rendering for Compose. It does not deliver server-driven
rendering (SDR), where a server only arranges components the client already has.

- Composable logic MUST run on the producer: state, effects, lambdas, conditions and loops.
- The consumer MUST be able to show a screen without having the code that produced it.
- The producer MUST be able to render content the consumer has no component for.
- The producer MUST also be able to use the consumer's own components, so native look, theme and
  accessibility are kept where the consumer has them.
- A feature that only works when every piece of the screen is in the consumer's catalog does not
  meet this principle.

Rationale: a closed catalog is what A2UI and classic server-driven UI already give. The proof of
concept in `poc/` showed that both directions can live in one screen.

### II. Four Deliverables, One Contract

The project ships four things: a library for the consumer app, a library for the producer server,
a core library with the code both of them share, and a Kotlin (K2) compiler plugin that lets the
two sides talk to each other.

- Everything both sides must agree on (document format, catalog, handshake, messages, action
  bindings) MUST have exactly one definition, and it lives in the core library.
- The consumer library MUST NOT depend on the producer library, and the producer library MUST NOT
  depend on the consumer library. Both depend on the core library only.
- The core library MUST NOT depend on Android, on a server framework, or on a transport.
- The compiler plugin MUST generate the bindings between producer and consumer whenever they can be
  generated, action IDs first of all. Users MUST NOT have to write or keep IDs in sync by hand.
- Anything the plugin generates MUST also be possible to write by hand, so the libraries stay
  usable and testable without the plugin.

Rationale: in the PoC the server-side stub for each shared component and its ID were written by
hand, and a mismatch only showed up at run time as a placeholder on the screen.

### III. Our Own Document Format

The producer describes content the consumer does not have as a document, and the consumer renders
it. The format is ours.

- The format is inspired by AndroidX Remote Compose. Shipped artifacts MUST NOT depend on AndroidX
  Remote Compose.
- A document MUST be producible on a plain JVM with no Android classes, stubs or emulators.
- The format MUST carry a version, and a consumer MUST reject a document it cannot read with a
  clear error.
- The format MUST support interactions that need no round trip to the producer.
- Sizes in a document MUST be density independent. Raw pixels are not acceptable.
- A document MUST be validated by the consumer before it is rendered. A broken document MUST NOT
  crash the app.

Rationale: the PoC leaned on an alpha, restricted API as a stand-in. It proved the idea, and it
also showed the costs: names that change between releases, raw pixel sizes, and behaviour we could
not explain.

### IV. Catalog and Handshake Before Rendering

The consumer tells the producer what it is and what it has before any screen is produced.

- The consumer project MUST have a catalog of its components that the producer may use, with a
  catalog version.
- At first connection the consumer MUST send: device density, device orientation, screen size,
  active theme (dark or light), active language, the app's version, variant and flavor, and the
  catalog version.
- The producer MUST NOT send a reference to a component that is not in the consumer's catalog. It
  renders that content as a document, or fails with a clear error.
- The producer MUST use the handshake to size and adapt what it produces, so a screen does not
  break the app that receives it.
- Every spec that touches the handshake MUST say what happens when one of these values changes
  during a session, such as a rotation or a theme change.

Rationale: without the catalog and the app version, the producer guesses what the consumer can
show. The PoC had no handshake, and an unknown component or mismatched props became a placeholder.

### V. Real-Time, Self-Recovering, Secure Connection

- The connection between consumer and producer MUST be real time in both directions.
- The consumer MUST detect a dead connection without user action and MUST recover from it
  automatically.
- Connection errors MUST be handled without a crash or a frozen screen. The user MUST get a way to
  retry, and the consumer app MUST be able to supply its own error composable.
- A secure connection is required. Traffic MUST be encrypted in transit. Cleartext is only allowed
  in local development builds.
- End-to-end encryption is the goal. A design that does not reach it MUST say why and what it
  protects instead.
- The transport MUST be replaceable. No public API may expose a transport type. The PoC used a
  WebSocket with kotlinx.rpc; another option such as protocol buffers is chosen only if
  measurements show it is more reliable, more stable or faster.

Rationale: in the PoC a killed server left the stream hanging forever until a ping and a watcher
were added, and the app kept showing a stale screen with no hint that anything was wrong.

### VI. Test-First (NON-NEGOTIABLE)

- Tests are written first, seen failing, and only then is the code written.
- Test names follow `given X, when Y, then Z`, and test bodies have Arrange, Act and Assert parts.
- Tests use hand-written fakes. Mocking libraries are not used.
- Anything both sides agree on MUST have contract tests that run the producer's output through the
  consumer's reader.
- The compiler plugin MUST be tested with unit tests that compile sample sources.
- Rendering MUST be verified on a real device or emulator while the work is being done, by looking
  at the result. A passing build is not proof that a screen is right.

Rationale: the PoC's two real defects (a one-way toggle and an unnoticed dead server) passed every
automated test and were only found on a device.

### VII. Evidence Over Assumption

- A claim about what a tool, a library or our own code does MUST be checked before it is relied
  on or written down. What was not checked MUST be labelled as not checked.
- Risky questions are answered first, with the smallest experiment that can answer them.
- Input size is never assumed to be small. Specs and plans MUST name a realistic upper bound for
  sessions, document size and update rate, and designs MUST hold at that bound.
- Choices about speed or reliability MUST rest on measurements.

Rationale: the PoC was run as a series of go or no-go checks, and several early beliefs turned out
wrong only when tested.

### VIII. Small Public API, Simple Design

- Public API is kept small. Implementations are internal, behind interfaces or factories.
- A public API change is its own piece of work. It is never mixed into a cleanup.
- Third-party types that are alpha, experimental or restricted MUST NOT appear in our public API.
- The simplest design that meets the spec wins. Nothing is built for a need that is not in a spec.
- Extra complexity MUST be justified in writing in the plan that adds it.

Rationale: several artifacts that depend on one contract are hard to change once others use them.

### IX. Versioned Producer, Old Apps Keep Working

People can stop an app from updating, so apps in use will always be older than the producer.

- The producer API and everything in the shared contract MUST be versioned.
- The consumer MUST tell the producer which contract version it speaks, along with the app and
  catalog versions from the handshake.
- A producer MUST keep serving every consumer version it still supports, in a form that version
  understands.
- When a producer no longer supports a consumer version, it MUST refuse with a clear error the
  consumer can show. It MUST NOT send content that version cannot read.
- A breaking change to the contract MUST come with a new version and a stated period during which
  the old one is still served.
- Compatibility MUST be tested: the current producer is run against each supported older consumer
  version.

Rationale: the producer changes on the server at any time, and the app on a phone does not. Without
versions, a server release breaks every app that has not updated.

## Technology and Platform Constraints

- Language: Kotlin with the K2 compiler, on the newest stable version the toolchain allows.
- Producer library: runs on a plain JVM server. It MUST NOT need Android.
- Consumer library: a Kotlin Multiplatform library that renders with Compose, with Android as its
  only target for now. Everything that does not need Android lives in common code, and public API
  MUST NOT expose Android-only types where a common one exists. This keeps the API composable
  friendly and ready for more targets.
- Build: this repository builds with Gradle and a version catalog. Every dependency and plugin
  version lives in the catalog. Shared build logic lives in convention plugins named
  `dev.tonholo.ssrcompose.conventions.<concern>`.
- Consumption: the libraries and the compiler plugin MUST be consumable from a Gradle project and
  from the Kotlin toolchain. Nothing a user needs may work only through Gradle.
- Static analysis: detekt close to its defaults. Format-only changes go in their own commits.
- Code rules: no `!!`, no wildcard imports, no fully qualified names in code, named arguments
  where possible, no magic numbers, constructor injection, coroutines for concurrency.
- Comments: code explains itself. Comments are only for short API docs, formulas, attributions, and
  logic that truly cannot be simplified.
- The `poc/` folder is a reference. Library code MUST NOT depend on it, and it is not held to the
  library's standards. Lessons from it are carried over through specs, not by copying code blindly.

## Development Workflow

- Work goes through Spec Kit: specify, plan, tasks, then checklists. Clarify and analyze are used
  when a spec needs them.
- Each feature is sliced so something visible runs on a device early, not only at the end.
- Commits are small and use Conventional Commits, on one line, with no footer and no co-author or
  session trailer.
- History stays linear. Branches are rebased, not merged with merge commits.
- The one who reviews a piece of work is not the one who built it.
- Plans and specs written by the Superpowers skills are never committed.
- Everything a person reads (docs, commit messages, PR text) is written in plain, simple English,
  with no em dashes and no bold lead-in labels.

## Governance

- This constitution comes before any other practice in this repository. When a spec, a plan or a
  habit disagrees with it, the constitution wins or it is amended first.
- An amendment is a change to this file with a short reason. It needs the owner's approval.
- Versions follow semantic versioning: MAJOR when a principle is removed or redefined in a way that
  breaks earlier work, MINOR when a principle or section is added or widened, PATCH for wording.
- Every plan MUST include a check against these principles, and every review MUST confirm it.
- A deviation is allowed only when the plan names the principle, the reason, and the simpler option
  that was turned down.
- Day-to-day guidance for people and agents lives in `README.md` and the agent instruction files.
  They follow this constitution and never override it.

**Version**: 1.0.0 | **Ratified**: 2026-11-14 | **Last Amended**: 2026-11-15
