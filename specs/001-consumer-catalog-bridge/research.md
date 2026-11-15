# Research: Consumer Catalog and Bridge

Decisions for the plan, each with its reason and what was turned down. Facts are marked as checked or not checked, as the constitution's principle VII asks.

Two experiments were run on 2026-11-15 before any design, on Kotlin 2.4.21, Compose runtime 1.12.2, Gradle 9.8.2 and JDK 25 with a JVM 17 target. Their code is in `.superpowers/spikes/k2-bridge/` and `.superpowers/spikes/k2-catalog/`, which git ignores. They are throwaway and nothing depends on them.

## R1. The compiler creates the bridge from a catalog file

Decision: on the server side, the compiler plugin reads a saved catalog file and creates top-level declarations: one `@Composable` function per component and one `enum class` per choice list. FIR declares them and IR gives them bodies. Each body is one call to a public function of the producer library.

Checked by experiment:

- FIR declares the enum and the composable functions from a file, and IR fills the bodies.
- The Compose compiler treats the created functions like hand-written ones. Their compiled signatures match a hand-written twin, and they compose in a real composition on a plain JVM with slot nesting and a working callback.
- Code in the same module calls them with a normal import and no extra step. Another module sees them through the first module's metadata.
- A wrong name, a missing parameter, and a `String` given where a choice is expected each fail to compile with the ordinary Kotlin error at the caller's line.
- Editing one source file recompiles only that file and causes no clash with the created declarations.

Alternatives considered: a source generator that writes Kotlin files. Turned down by the owner: the compiler creates the definitions.

## R2. The order of the two compiler plugins is a correctness matter

Decision: the bridge plugin's IR step must run before the Compose compiler's. The build-tool plugins pass `-Xcompiler-plugin-order=<bridge>><compose>` themselves, and the bridge plugin keeps a guard that stops the build with a clear message if it runs second.

Checked by experiment:

- With the right order it works. With the wrong order and no guard, the build passes and the program fails at run time with `NoClassDefFoundError: androidx/compose/runtime/internal/ComposableFunction0`. That is a silent miscompile.
- The order flag controls this in Gradle, both ways.
- The guard turns the wrong order into a compile error.
- The compile-testing library hides every plugin behind one ID, so the flag cannot be tested there. Only list order counts in those tests.

Not checked: that a Gradle compiler plugin integration can add the flag cleanly, that the Android Gradle Plugin keeps it, and how the Kotlin toolchain orders plugins.

## R3. A real enum for a choice list needs a workaround

Decision: a choice list becomes a real `enum class`, as the owner's example shows. The typed FIR API cannot create enum entries, so the plugin adds them through compiler internals. The result has the same bytecode shape as a hand-written enum, and `entries`, `valueOf` and an exhaustive `when` all work.

Checked: all of the above, in the command line compiler and Gradle.

Risk: the workaround uses an unchecked cast and writes into the class's declaration list. It is the part most likely to break in the IDE or in a new Kotlin version. Not checked: any IDE behaviour.

Fallback if it breaks: a class with one constant per choice, created with the typed API. It loses the exhaustive `when` on the server, which a server screen rarely needs, since it gives choices and does not receive them.

## R4. The compiler writes fragments, and a build step merges them into the catalog

Decision: on the app side, the compiler plugin writes one small fragment file per source file that has marked composables. A build-tool step merges the fragments into the catalog file. The merge is also where duplicate names are caught.

Checked by experiment:

- A FIR checker finds `@ConsumerComposable` functions and reads every parameter kind the spec lists, including slots with arguments and callbacks with data. With the Compose compiler present, FIR shows a slot as a composable function type, and a plain lambda as a function type.
- An unsupported parameter fails the build at the parameter's line and column, with the composable, the parameter and the reason. A marked function that is not `@Composable` also fails.
- A modifier parameter is left out.
- Under incremental compilation the plugin sees only the recompiled files, and one build can run several rounds. A catalog written directly by the plugin loses components.
- With fragments plus a merge, the catalog after incremental builds is byte for byte the same as after a clean build. Removing the annotation deletes the fragment. A renamed or deleted file leaves an old fragment that the merge ignores because its source is gone.
- Two clean builds give identical bytes.
- The compiler catches a duplicate name only on a full build. The merge catches it always and names both.

Not checked: a Kotlin Multiplatform module with an Android target (the plugin runs once per compilation, so fragments need a folder per compilation, and a marked `expect` and `actual` pair would look like a duplicate), composables spread over several modules of one app, the build cache and the configuration cache, and the state of fragments after a failed incremental build.

Alternatives considered: the compiler writes the whole catalog. Turned down by the experiment.

## R5. Build-tool plugins on top of the compiler plugin

Decision: the compiler plugin cannot do everything, so two thin build-tool plugins wrap it, one for Gradle and one for the Kotlin toolchain. Their shared logic lives in one plain library, `tooling`, so neither build tool gets a feature the other lacks. They do these things:

- Apply the compiler plugin with its options and the plugin order.
- Merge fragments into the catalog and check it against the saved catalog of the same version.
- Fetch, validate, save and pin catalogs for a server project.
- Declare each saved catalog as a build input. Checked: without that, editing the catalog leaves the build stale.

Rationale: R2 and R4 show the compiler alone is not enough, and the constitution says nothing may work only through Gradle. The owner's ktgd project already has this shape: a compiler plugin, a Gradle plugin and a toolchain plugin with its tasks in a `plugin.yaml`.

This adds two artifacts and one library to the constitution's four deliverables. It is recorded in the plan's Complexity Tracking.

Not checked: the toolchain plugin itself, and how the Kotlin toolchain builds an Android library.

## R6. Fetching at sync

Decision: a `syncCatalogs` step fetches each catalog from its location (a web address or a file path), validates it, saves it in the server project and writes its fingerprint to a lock file. Builds read only the saved copies. When the location's content no longer matches the lock file, the step reports it and changes nothing unless asked.

The step is wired so the IDE's sync triggers it, and it can also be run on its own. A build never fetches.

Not checked: how to make a Gradle task run on IDE sync. A search found only forum answers, and some say sync does not run tasks. A task settles it. If sync cannot trigger it reliably, the step stays a command the developer runs, and a build fails with a clear message when a saved catalog is missing.

Safety: only `https` addresses and file paths are accepted by default. Names in a catalog are validated before anything is created from them.

## R7. Consumer side: one renderer per marked composable

Decision: on the app side, the compiler plugin creates, for each marked composable, a small renderer that takes a placed component and calls the real composable with decoded values, callbacks that send a trigger, and slots that render their content. Because it is created per composable, incremental compilation is safe. The merge step writes an index of name to renderer as a resource, and the consumer library loads it once.

A developer can do the same by hand with `ComponentBindings`.

Not checked: all of it. The experiments covered reading composables (R4) and creating declarations (R1), not this combination. A task builds the smallest version first. If creating the renderer in the compiler proves fragile, the hand-written bindings still work, and the renderer becomes follow-up work.

## R8. Callbacks on the server follow the function type pattern

Decision: a callback parameter of a bridge function has one of the types `RemoteAction0`, `RemoteAction1<A>`, `RemoteAction2<A, B>`, `RemoteAction3<A, B, C>` or `RemoteAction4<A, B, C, D>`, in the producer library. The number matches the arguments of the app's callback, as Kotlin's own `Function0`, `Function1` and so on do. Each is a function interface, so a server developer passes a lambda.

This is the owner's proposal. Two points were agreed with it:

- There is no result type. The app's callback is an ordinary function that returns at once, so it cannot wait for the server. A result from the server, if ever needed, is a different thing and gets its own type then.
- The pattern stops at four arguments. A marked composable with a longer callback is a build error in the app. Raising the limit later breaks nobody.

In the catalog a callback lists its `arguments`, the same field a slot uses.

Not checked: creating a parameter of these types in FIR (the experiment used plain function types, which worked), and how the Compose compiler treats a lambda converted to a function interface compared with a plain function type. The early experiment that covers created types settles both.

## R8a. Optional and nullable parameters

Decision: the catalog says if a parameter can be left out, never what its default is, because only the app knows. In the bridge, a parameter that can be left out is nullable with `null` as its default, and `null` means "not given": nothing is sent and the app's own default applies. A value or choice parameter whose type is nullable in the app is marked `nullable` in the catalog and stays nullable in the bridge.

Not checked: defaults in created functions (the bridge experiment created none), and nullable types in the catalog (the catalog experiment rejected them, by its own choice).

## R8b. Slots that pass arguments

Decision: such a slot is described in the catalog and cannot be filled until remote composables exist. An optional one gets no bridge parameter. A component with a required one cannot be placed: its bridge function is created so the name resolves, and any call to it is a compile error that names the later feature.

Consequence for the first samples: the owner's `PaymentSection` and `CameraScanner` examples both have a required slot with arguments, so the samples of this feature use components with values, callbacks and slots without arguments.

Not checked: how that compile error is produced.

## R8c. Where the catalog comes from in a project with variants

Decision: a catalog belongs to one compilation. Each Android variant has a catalog version, the project's by default, and the check compares the variant being built with the saved file of its version. A flavor with different marked composables sets its own version. The compiler plugin writes fragments in a folder per compilation. A marked `expect` and `actual` pair is one component.

Not checked: all of it, since the catalog experiment used a plain JVM module.

## R9. One bridge catalog, many held catalogs

Decision: the server project holds one saved file per catalog version. The developer names one of them as the bridge catalog, and the bridge is created from it. The others are data: the producer loads them at start and uses the one that matches each session. A session whose catalog differs from the bridge catalog gets what it has, by the fitting rules in data-model.md.

Rationale: versions have no order, so "newest" cannot be worked out. One classpath also cannot hold two bridges with the same names. For the same reason the bridge is created in one module of the server, and other modules depend on it. Not checked: what happens when two modules create the same package.

## R10. Replacements

Decision: the producer library has `OrElse`, a composable that takes content and a replacement. If a component inside the content is missing for the session, the replacement is shown in its place. Without an enclosing `OrElse`, a missing component stops the screen. A screen can also ask `LocalConsumer.current.has("Name")`.

Not checked: the implementation. `has` by a text name is not checked at build time. The compiler plugin can also create a constant per component name to fix that; it is optional and left for the tasks.

## R10a. A callback whose lambda fails

Decision: the producer runs a callback's lambda so that what it writes to the screen's state is kept only when it finishes. If it throws, those writes are thrown away, the screen is not recomposed, the session carries on, and the observer is told. The Compose runtime's snapshots allow this: the lambda runs in a snapshot that is applied on success and dropped on failure.

Not checked: that a dropped snapshot leaves the live composition untouched in every case, for example when the lambda also changed something outside snapshot state. A test in the callbacks story settles the first part. The second is a documented limit: only snapshot state is protected.

## R10b. Callbacks that arrive too often

Decision: a limit per session, 100 per second by default and configurable through the producer's limits. Callbacks over the limit are dropped and reported, and the session carries on.

## R11. Modules and targets

| Module | Kind | Targets | Depends on |
|---|---|---|---|
| `core` | Kotlin Multiplatform library | `jvm`, `android` | nothing in this repo |
| `consumer` | Kotlin Multiplatform library | `android` | `core` |
| `producer` | Kotlin JVM library | JVM | `core` |
| `compiler-plugin` | Kotlin JVM | JVM | `core` (catalog model only) |
| `tooling` | Kotlin JVM library | JVM | `core` |
| `gradle-plugin` | Gradle plugin | JVM | `tooling` |
| `toolchain-plugin` | Kotlin toolchain plugin | JVM | `tooling` |
| `contract-tests` | test-only | `android` host tests | `core`, `consumer`, `producer` |
| `samples/app` | its own build | Android | the published `consumer` and plugins |
| `samples/server` | its own build | JVM | the published `producer` and plugins |

The two samples are separate builds on purpose. The spec's success criterion says the sample server must build with none of the sample app's code available, sharing only the catalog file.

The owner confirmed earlier that `core` has a JVM target and an Android target, and `minSdk` 23.

## R12. Versions

Checked against Maven metadata on 2026-11-15: Kotlin 2.4.21 (2.5.0 is a release candidate), Android Gradle Plugin 9.5.1, Compose Multiplatform 1.12.2, kotlinx.serialization 1.13.0, kotlinx.coroutines 1.11.0. Checked by experiment: `dev.zacsweers.kctfork:core:0.14.0` works with Kotlin 2.4.21 and the Compose compiler. The Kotlin toolchain on the owner's machine is 0.13.0.

Compiler plugin APIs for top-level generation are marked experimental in Kotlin, so each new Kotlin version needs the plugin's tests to pass before it is supported.

## R13. The editor

Developers must turn off `kotlin.k2.only.bundled.compiler.plugins.enabled` in the IDE registry to see created declarations, as the owner said. It is a documented setup step.

Not checked: anything in the IDE. Known risks from reading the code: the enum workaround (R3), the catalog being read once per plugin load so an edit is not seen until reload, and created declarations having no source to navigate to.

## R14. What carries over from the earlier draft

Still valid, and designed the same way: the handshake values and their validation limits, a neutral data model so no wire encoding is baked into the contract, a replaceable codec and connection with no transport in the libraries, one live composition per session with one snapshot observer shared by the process, an observer for reporting, and a load test for the bounds.

Validation limits, all configurable: introduction frame at most 8 KB; density above 0 and at most 10; screen width and height 1 to 100,000 pixels; language tag at most 35 characters; app version, variant, flavor and catalog version at most 64 characters each with no control characters; callback arguments at most 1 megabyte in total; callbacks at most 100 per second per session.

## R15. Decisions that become ADRs

- The compiler creates the bridge from a catalog file, with the plugin order rule (R1, R2).
- Fragments plus a merge step for producing the catalog (R4).
- Build-tool plugins over one shared tooling library (R5).
- Catalogs are fetched at sync and pinned (R6).
- Module layout and targets (R11).
