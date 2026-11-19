# Feature Specification: Consumer Catalog and Bridge

**Feature Branch**: `001-consumer-catalog-bridge`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "The catalog is the composables from the app that server-built UI can interact with. It is produced from annotated composables as a plain descriptor file, not as Kotlin types. The server project points at that file, fetches it at sync, and the compiler creates bridge composables with the same names and parameters. The server can then place the app's composable exactly where it was defined on the server and allow the interaction. The server may live in another repository."

## Clarifications

### Session 2026-11-15

These answers were given by the owner while an earlier draft of this feature was being planned. They are carried over, except where an answer is marked as replaced.

- Q: What is the catalog? → A: The composables from the app that server-built UI can interact with.
- Q: Is the catalog written by hand? → A: No. It is produced: annotations on the app's composables and the compiler generate it. It is a simple file of descriptors, not Kotlin types.
- Q: The server can be in another repository, so how does it know the catalog? → A: The server project is given the location of the catalog file, a plain URL is enough. The file is fetched during the build tool's sync step, never during compilation. The compiler then creates the bridge definitions from it. (A catalog is now a manifest and parts, see the design revision below.)
- Q: Does the generated code contain the app's code? → A: No. It is only a bridge. The app's components exist only inside the consumer.
- Q: When an app's component has a slot that takes arguments, who supplies them? → A: The app's component, since it is the one that calls the slot. The server only consumes the values and fills server-defined content with them.
- Q: When an app connects, does it send its catalog? → A: No, only the catalog version. The producer must always know the consumer's catalog. The app may not know a new screen, but the producer always knows the app's catalog. (Replaced in part by the design revision below: the producer holds no catalog; it knows each catalog through the bridge it was built with.)
- Q: An older app has a component with fewer parameters than the screen gives. What happens? → A: The producer fits what it sends to that app's version. It drops what the older version does not have, and treats the component as missing only when the older version needs something the screen did not give. (Replaced by the design revision below: screens are written per catalog version, and nothing is adapted for an older app.)
- Q: A screen uses a component the app does not have and no replacement was defined. What does the user lose? → A: The whole screen. A screen with a silent hole can mislead. (Replaced by the design revision below: there are no replacements, and each catalog version has its own screens.)
- Q: How large should one producer go? → A: Up to 1,000 components per catalog, 50 parameters per component, 10,000 connected apps per producer, and 10 catalog versions served at once. (The bounds on components and parameters are replaced by the design revision below. 10,000 connected apps and 10 catalog versions still hold.)
- Q: May a server developer trust the app's own version, variant and flavor to decide who sees what? → A: Left open. Nothing proves those values, and each server developer decides.
- Q: Are modifiers part of this? → A: No. Modifiers are out of scope for now.
- Q: How does a callback with more than one argument look on the server? → A: It follows the pattern of Kotlin's function types: one type per number of arguments, with no result type.

### Session 2026-11-15, design revision

The owner approved these changes later the same day, after the design was planned. They replace the answers marked above, and the earlier design where one held catalog was picked to create the only bridge.

- Q: Can the catalog slow the build or break build caching? → A: It must not. The build time must not suffer, and build caching must keep working. So a catalog is a bundle: one manifest, and one part for each app module that has marked composables. Each part is produced and cached with its own module.
- Q: Is there a hard limit on the number of components or parameters? → A: No. "1000 components with 50 parameters each could be just a mid size catalog": three real apps have 621, 751 and 1,085 composables, and the last one is "just an MVP". A cap on the size of each file protects the reader instead, and the library is tested at 20,000 components with about 80,000 parameters. The limit of four callback arguments stays.
- Q: Is the catalog needed while the app or the server runs? → A: No. "We don't need the catalog at the runtime, as we will use it only during the build time to generate the types."
- Q: How does one server keep apps of older catalogs working? → A: Like a versioned endpoint. An app of catalog version 1 gets the screens written for version 1, and an app of version 2 gets the screens written for version 2. An app of a version with no screens is refused. The server chooses when to stop serving each version. Every held catalog has its own bridge, and nothing is adapted for an older app.
- Q: How does the producer read the contract version before it decodes the introduction? → A: The app's first frame starts with the contract version, in a fixed form that every codec shares. No other frame has it.

## User Scenarios & Testing *(mandatory)*

Three people are served by this feature:

- The app developer, who marks which of the app's composables a server may use.
- The server developer, who writes screens in a project that may live in another repository.
- The end user of the app, who must never see a broken or unknown composable because the app and the server disagree.

### User Story 1 - A server screen places an app composable (Priority: P1)

An app developer marks a composable as usable by the server. Building the app produces a catalog that describes it: its name, its parameters, and any choice types they use. The server developer points the server project at that catalog. After a sync, the server developer can call the composable by the same name with the same parameters, as if it were part of the server project, even though none of the app's code is there. The app connects, introduces itself, and shows the screen. The composable is drawn by the app's own code, with the values the server gave.

**Why this priority**: It is the smallest slice that proves the whole chain: mark, produce, share, bridge, connect, render. It puts something on a real device, and every other story builds on it.

**Independent Test**: Mark one composable in a sample app, build it, point a sample server in a separate build at the produced catalog, write one screen that calls it, run both, and see the composable on a device with the server's values.

**Acceptance Scenarios**:

1. **Given** a composable marked for the server, **When** the app is built, **Then** a catalog exists that describes the composable's name, its parameters and the choice types they use.
2. **Given** a server project pointed at a catalog, **When** the project is synced, **Then** the server developer can call each catalog composable by its own name and parameters, and the server project holds none of the app's code.
3. **Given** a server screen that calls a catalog composable, **When** the server developer gives a wrong name, a missing parameter or a value of the wrong type, **Then** the server does not build.
4. **Given** an app and a server built this way, **When** the app connects and asks for the screen, **Then** the app shows the composable, drawn by the app's own code, with the values the server gave.
5. **Given** a connecting app, **When** it introduces itself, **Then** the producer receives the device density, orientation, screen size, active theme, active language, the app's version, variant and flavor, the catalog version, and the contract version the app speaks, and no catalog.
6. **Given** an app composable with a parameter the catalog cannot describe, **When** the app is built, **Then** the build fails with a message that names the composable, the parameter and the reason.
7. **Given** a saved catalog and a location whose manifest changed since, **When** the server project is synced, **Then** the difference is reported, the saved copy is kept, and the build still uses it.
8. **Given** a saved catalog and a location that cannot be reached, **When** the server project is synced and built, **Then** the developer is told, and the build gives the same result as before.
9. **Given** no saved catalog and a location that cannot be reached, **When** the server project is synced, **Then** the sync fails with a message that names the location.
10. **Given** an app with marked composables in two modules, **When** the app is built, **Then** the catalog has one manifest and one part for each of the two modules, and together they describe every marked composable.
11. **Given** a manifest that names a part with a folder, a parent folder or a scheme in its file name, **When** the server project is synced, **Then** the catalog is rejected, nothing is saved, and the message names the manifest and the part.

---

### User Story 2 - A tap in the app runs a lambda on the server (Priority: P2)

An app composable has a callback, such as "pay was clicked". On the server, that callback is a lambda the server developer writes. When the user taps, the lambda runs on the server, the server's state changes, and the screen in the app updates. A callback can also carry data from the app, such as the result of a scan.

**Why this priority**: The owner defined the catalog as what the server can interact with, not only show. A component whose callbacks cannot be used is not the catalog that was asked for.

**Independent Test**: A sample screen with a counter and an app composable that has a click callback. Tap it on a device and see the count change, with the counting done on the server.

**Acceptance Scenarios**:

1. **Given** a screen that gives a lambda to an app composable's callback, **When** the user triggers that callback in the app, **Then** the lambda runs on the server.
2. **Given** a lambda that changes state the screen holds, **When** it runs, **Then** the app shows the updated screen without the user doing anything else.
3. **Given** a callback that carries arguments, **When** the app triggers it, **Then** the server lambda receives them, in the same order.
4. **Given** a callback the screen no longer has, because the screen changed, **When** a late trigger for it arrives, **Then** nothing runs and the session carries on.
5. **Given** two apps connected to the same screen, **When** one triggers a callback, **Then** only that app's screen state changes.

---

### User Story 3 - Server content sits inside an app composable (Priority: P3)

An app composable has a slot: a place where it shows content given to it. The server developer fills that slot on the server with other catalog composables. In the app, that content appears exactly where the composable puts its slot, drawn by the app's own code, and it can be nested.

**Why this priority**: This is what makes it server-side rendering and not a list of widgets: the server decides the structure, including what goes inside the app's own containers.

**Independent Test**: A sample app container with a slot, and a server screen that puts two other catalog composables inside it. See them inside the container on a device.

**Acceptance Scenarios**:

1. **Given** an app composable with a slot, **When** a screen fills the slot with other catalog composables, **Then** the app shows them inside that composable, where it places its slot.
2. **Given** slots inside slots, **When** the screen is shown, **Then** the nesting in the app matches the nesting written on the server.
3. **Given** a slot the screen leaves empty, **When** the composable allows that, **Then** the composable is shown with an empty slot.
4. **Given** an app composable with a required slot that passes arguments to its content, **When** a server developer tries to place that composable, **Then** the server does not build, and the message names the composable and the slot and says that slots with arguments are not supported yet.

---

### User Story 4 - Apps that did not update keep working (Priority: P4)

People can stop an app from updating. So apps of several releases are connected to the same producer, each with the catalog it shipped with. The server project holds the catalog of every release it serves, and each one has its own bridge. The server developer writes screens for each catalog version and registers them under that version. Each app gets the screens written for its own catalog version, so it only ever receives what its catalog has. When the server developer stops serving a catalog version, its apps are refused clearly, and the user is told why and what to do.

**Why this priority**: Without this, every server release risks breaking installed apps. It comes after the first three stories because there is nothing to keep compatible until they exist.

**Independent Test**: Run one producer with screens for two catalog versions against two app builds made from those versions, and against one app whose catalog version the producer does not serve.

**Acceptance Scenarios**:

1. **Given** two apps with different catalog versions connected at once, **When** each asks for the same screen name, **Then** each gets the screen registered under that name for its own catalog version.
2. **Given** a composable that a newer catalog removed, **When** a screen registered for an older catalog version places it through that version's bridge, **Then** apps of the older version show it.
3. **Given** a screen name registered for one catalog version only, **When** an app of another catalog version asks for it, **Then** the app is told the screen cannot be shown, and the session stays accepted.
4. **Given** a screen registered for one catalog version that places a composable from the bridge of another catalog version, **When** an app asks for it, **Then** the screen is not sent, the app is told the screen cannot be shown, and the server developer gets a report that names the composable, the screen and both catalog versions.
5. **Given** an app whose catalog version or contract version the producer does not serve, **When** it connects, **Then** it is refused with a reason, gets no screen content, and the user sees why and what to do.
6. **Given** a new app release whose catalog version has no screens on the producer yet, **When** that app connects, **Then** it is refused in the same clear way until the producer serves its catalog version.

---

### User Story 5 - A screen adapts to the device and follows its changes (Priority: P5)

A server developer writes a screen that reads what the app said about itself: density, orientation, screen size, theme, language and app details. When one of these changes while the screen is open, the screen follows without losing what the user was doing.

**Why this priority**: It is what makes the introduction useful beyond a compatibility check. It can wait until the bridge itself is safe.

**Independent Test**: Open a screen that changes with orientation and theme, rotate the device, switch the theme, and see the screen follow while a value the user changed stays.

**Acceptance Scenarios**:

1. **Given** a screen that arranges itself by orientation, **When** an app in landscape asks for it, **Then** it gets the landscape arrangement.
2. **Given** an open screen, **When** the device rotates, the theme changes or the language changes, **Then** the app tells the producer and the screen updates to match.
3. **Given** an open screen that holds something the user did, **When** a device value changes, **Then** the screen updates and what the user did is still there.

---

### Edge Cases

- Two composables marked for the server have the same name, in one module or in two. The app build fails and names both places. (FR-006, FR-063)
- Two parts describe the same choice list differently. The app build fails and names both parts. (FR-063)
- Two modules would give parts with the same name. The app build fails and names both modules. (FR-065)
- A marked composable takes a modifier. Modifiers are out of scope: the parameter is left out of the catalog and the composable gets its default. (FR-007)
- The manifest at the given location changed since it was last saved, without the server developer asking for it. The saved copy is still used, and the difference is reported at the next sync. (FR-016)
- The location cannot be reached during a sync. The saved copy is used and the developer is told. With no saved copy, the sync fails with a clear message. (FR-015)
- A fetched manifest or part is not valid, does not match its fingerprint, or holds names that cannot be used as names in code. The catalog is rejected and nothing is created from it. (FR-017)
- A manifest gives a part a file name that is not the fixed name built from the part's name: one with a folder, a parent folder or a scheme in it, the manifest's own file name, an empty name, a dot, or a file name another entry of the manifest already uses. The manifest is rejected. (FR-066)
- A fetched manifest or part is larger than the size cap. It is rejected before it is read. (FR-068)
- The app developer changes a marked composable, or moves it to another module, but does not raise the catalog version. The app build fails and says the catalog changed. (FR-010)
- The app developer sets a catalog version that has no saved catalog yet. The app build fails and says how to save one. (FR-010)
- A callback's arguments are far larger than any real ones. The producer refuses them without slowing other connected apps. (FR-043, FR-057)
- The app triggers a callback many times in a moment. Each one runs in the order it was triggered. (FR-040, FR-061)
- An app introduces itself with a value that makes no sense, or with something far larger than a real introduction. It is refused, and other apps are not affected. (FR-056, FR-057)
- The app asks for a screen before it was accepted, or for a name that has no screen for its catalog version. (FR-031, FR-052)
- A screen for one catalog version places a composable from the bridge of another. The screen is not sent and the server developer is told. (FR-073)
- A device value changes while the introduction is still being answered. The app reports the newest values once it is accepted. (FR-054)
- An app composable has an optional slot that passes arguments. The server can place the composable and cannot fill that slot. (FR-036)
- An app has no marked composable. Its catalog is valid and has no parts, its bridge has no composables, and the app is accepted when screens are registered for its catalog version. (FR-002, FR-046)
- The app receives something it cannot draw, although it should never happen. It does not crash and does not show a developer placeholder. (FR-037)

## Requirements *(mandatory)*

### Functional Requirements

Marking composables and producing the catalog:

- **FR-001**: An app developer MUST be able to mark a composable in the app as usable by the server, in the place where the composable is written. Only a top-level composable function with no type parameters can be marked.
- **FR-002**: Building the app MUST produce a catalog that describes every marked composable. A catalog is a bundle: one manifest, and one part for each app module that has marked composables. The manifest names the catalog version and lists every part with a fingerprint of its content. The app developer never writes the catalog by hand.
- **FR-003**: The catalog MUST describe, for each composable: its name, and each parameter's name, kind, and whether it can be left out. A value or choice parameter also says if it can be empty. "Can be left out" means the app's composable has its own default for the parameter, so the server may give nothing. "Can be empty" means the app's composable accepts no value for it, so the server may give "no value" on purpose. The two are independent: a parameter can be one, both or neither. A component's name is its identity: renaming a marked composable changes the catalog and needs a new catalog version.
- **FR-004**: A parameter MUST be one of these kinds: a plain value (text, whole number, decimal number, yes or no), a choice from a fixed list, a list of one kind, a callback with no arguments, a callback with up to four arguments, a slot, or a slot whose content receives arguments. A marked composable with a callback of more than four arguments MUST fail the app build with a message that says so. A callback argument is a plain value, a choice, or a block of bytes.
- **FR-005**: Each part MUST describe every fixed list of choices that its components use, with its name and its choices, so a part can be read on its own.
- **FR-006**: The app build MUST fail, with a message that names the composable, the parameter and the reason, when a marked composable has a parameter the catalog cannot describe, and when two marked composables share a name. The build MUST fail in the same way when a marked function is a member, an extension or has type parameters.
- **FR-007**: A modifier parameter MUST be left out of the catalog. The composable is drawn with that parameter's default.
- **FR-008**: The manifest and every part MUST be plain, readable text that gives the same bytes for the same composables, so a change to the catalog shows up as a small, reviewable difference.
- **FR-009**: A catalog MUST have a version the app developer sets. Catalog versions are labels with no order. An app with several build variants has a catalog version per variant, the project's by default, and variants that share a version MUST have the same marked composables.
- **FR-010**: The app build MUST fail when the catalog it produces no longer matches the saved catalog of the same version, byte for byte, so a catalog cannot change without a new version. Moving a marked composable to another module is such a change. When no saved catalog exists yet for that version, the build MUST fail and say how to save one. It MUST NOT create one by itself, so a new catalog version is always a deliberate step.
- **FR-011**: The catalog format MUST be able to say, for each slot, what content it accepts: anything, only consumer composables (the app's own), or only producer composables (server-defined). In this feature all slot content is consumer composables, and the restriction is not checked yet.
- **FR-063**: Marked composables MAY live in several modules of one app. Each module with marked composables gives its own part, and the catalog covers all of them. A component name MUST be unique across the whole catalog, and a choice list used by two parts MUST be the same in both. Otherwise the app build fails and names both places.
- **FR-065**: A part's name MUST come from the module's path in the build, and the app developer MUST be able to set another one. The path is made lowercase, every run of characters other than ASCII letters and digits becomes one hyphen, and hyphens at the start and the end are dropped, so a module at `feature/home` or `feature_home` gives `feature-home`. The root module of the build gives `root`. A part name holds only lowercase letters, digits and hyphens, is at most 64 characters long, and is not a name that some systems keep for devices, such as `con` or `nul`, so every part can be saved as a file on every system. A name that breaks this, and two modules whose parts would have the same name, MUST fail the app build, with a message that names the module or both modules and says how to set a name.

Sharing the catalog with the server project:

- **FR-012**: The app's code and the server's code MUST be able to live in different repositories. The catalog, its manifest and its parts, MUST be the only thing the two builds share. This project's own libraries, which both builds use, are not part of what they share with each other.
- **FR-013**: A server developer MUST be able to give the server project the location of each catalog it uses. A catalog's location is the address of its manifest. A plain `https` address MUST be enough, and a file path MUST work too.
- **FR-014**: The catalog MUST be fetched during the build tool's sync step, and MUST NOT be fetched while code is being compiled. The sync step is a step of the build tool that a developer or a build server runs on purpose, and that an editor's sync also triggers where the build tool allows.
- **FR-015**: A fetched catalog MUST be saved inside the server project, with the location it came from and a fingerprint of its manifest. Builds MUST use the saved copy, MUST work with no network, and MUST give the same result for the same saved copy.
- **FR-016**: When the manifest at a location no longer matches the saved fingerprint, the sync MUST say so and MUST NOT replace the saved copy unless the developer asks for it.
- **FR-017**: A fetched manifest and every fetched part MUST be checked before anything is created from them. A manifest or part that is not valid, a part that does not match its fingerprint in the manifest, or a name that cannot be used as a name in code MUST be rejected, and nothing of that catalog is saved. A name can be used in code when it starts with a letter or an underscore, holds only letters, digits and underscores, is not a reserved word of the language, and is at most 64 characters long. This holds for every component, parameter, choice list and choice.
- **FR-018**: A server project MUST be able to hold the catalogs of several app releases at once.
- **FR-019**: The same steps MUST be available from every supported build tool. Nothing here may work with only one of them.
- **FR-066**: The parts of a catalog MUST be found next to its manifest, by the file name the manifest gives for each part. That file name MUST be a fixed name built from the part's name, so it is always a plain file name: no folder separator, no parent folder and no scheme. It is never empty, a dot, the manifest's own file name, or a file name another entry of the same manifest already uses. A manifest that breaks any of this MUST be rejected.
- **FR-067**: A sync MUST fetch the manifest first and check it, then fetch only the parts the server project does not already hold for that catalog version, and check each one against its fingerprint in the manifest before saving it.
- **FR-068**: A catalog MUST have no hard limit on the number of components or parameters. To protect the reader, the manifest and each part MUST be refused, before they are read, when they are larger than a size cap: 16 MiB each by default, and configurable.

The bridge on the server:

- **FR-020**: After a sync, every held catalog MUST have its own bridge. A server developer MUST be able to call each composable of a held catalog through that catalog's bridge, by its own name and with its own parameters, as if it were written in the server project. The server developer chooses, for each held catalog, where in the server project its bridge appears, so the bridges of two catalogs never clash. A composable that a newer catalog removed or renamed still exists in the bridge of every older catalog that has it.
- **FR-021**: The bridge MUST hold none of the app's code. It only stands for the app's composable.
- **FR-022**: The server MUST NOT build when a screen calls a catalog composable with a wrong name, without a required parameter, or with a value of the wrong kind.
- **FR-023**: On the server, a callback parameter MUST be given as a lambda the server developer writes, and a slot parameter MUST be given as content the server developer writes.
- **FR-024**: A server developer MUST be able to do by hand everything the build creates for the bridge, so the libraries can be used and tested without it. The same holds on the app side: an app developer MUST be able to connect each marked composable to what the server sends by hand.
- **FR-025**: The setup a developer's editor needs to show the bridge MUST be documented.
- **FR-064**: The documentation for app developers and the documentation for server developers MUST both state the release order: a producer that serves a new catalog version is released before, or together with, the app that ships it.

Connecting and introducing:

- **FR-026**: When an app first connects, it MUST introduce itself before anything else is exchanged.
- **FR-027**: The introduction MUST carry: device density, device orientation, screen size, active theme (dark or light), active language, the app's version, variant and flavor, the catalog version, and the contract version the app speaks. It MUST NOT carry the catalog.
- **FR-028**: The consumer library MUST collect the device values by itself. The app developer supplies only the app's version, variant and flavor.
- **FR-029**: The producer MUST answer every introduction with an acceptance that states the contract version it will speak, or with a refusal.
- **FR-030**: A refusal MUST carry a reason the app can show the user, and a kind: the contract version is too old, the contract version is too new, the catalog version is not known or no longer supported, or the introduction is not valid.
- **FR-031**: The producer MUST NOT send screen content to an app it has not accepted.
- **FR-032**: The app MUST show the user a message when it is refused, saying why in one sentence and what the user can do. The app developer MUST be able to replace it with the app's own presentation.

Showing a screen:

- **FR-033**: A screen MUST have a name the server developer gives it, and MUST be registered for one catalog version. The same name can be registered for several catalog versions, each with its own screen. The app asks for a screen by name, and the producer serves the screen registered under that name for the session's catalog version. A session has at most one open screen, and asking for another replaces it.
- **FR-034**: The app MUST draw each composable the server placed with the app's own code for it, in the place and order the server put it, with the values the server gave. When a screen places more than one composable at its top level, they are shown top to bottom. This arrangement is provisional until layout has its own feature.
- **FR-035**: Content the server put in a slot MUST appear where the app's composable places that slot, and slots inside slots MUST keep their nesting.
- **FR-036**: In this feature, a slot can be filled only with the app's own catalog composables, and a slot whose content receives arguments cannot be filled at all. When such a slot can be left out, the server has no way to give it and the app uses its own default. When it cannot be left out, the composable cannot be placed by the server, and trying to MUST fail the server build with a message that names the composable and the slot, and says that slots with arguments are not supported yet. The compiler plugin reports it on every call and every reference in each module where it runs, and the build tool plugin MUST run it in every module that can call a bridge. A call that still gets past the compiler fails with the same message when the bridge is composed, so a component that cannot be placed is never placed without its slot.
- **FR-037**: If the app receives something it cannot draw, it MUST NOT crash and MUST NOT show a developer placeholder. It tells the user the screen cannot be shown and reports the mismatch.
- **FR-062**: When the server does not give a parameter that can be left out, nothing is sent for it and the app's composable uses its own default. This holds for values, callbacks and slots. The catalog never holds the default, since only the app knows it.

Callbacks:

- **FR-038**: When the user triggers a callback of a placed composable, the lambda the server developer gave for it MUST run on the server, in that app's session.
- **FR-039**: A callback with arguments MUST deliver each of them to the server lambda, in order, with the kinds the composable declared. A callback never gives a result back to the app, since the app's callback returns at once and cannot wait for the server.
- **FR-040**: Callbacks from one app MUST run in the order they were triggered.
- **FR-041**: When a lambda changes what the screen holds, the app MUST get the updated screen without asking.
- **FR-042**: A trigger for a callback the screen no longer has MUST be ignored and MUST NOT end the session.
- **FR-043**: Callback arguments from an app are untrusted. The producer MUST limit their size and refuse what goes over the limit. It MUST also refuse, and report, arguments whose number or kinds do not match what the bridge declared for that callback, and arguments that cannot be turned into the types the server lambda takes, such as a choice the server does not know, a number too large for its type, or a list with one wrong element. A refused callback runs nothing, and the session carries on.
- **FR-060**: When a lambda fails while it runs for a callback, the changes it made MUST be thrown away, the screen MUST stay as it was, the failure MUST be reported to the server developer, and the session MUST carry on. The user is not shown an error for it.
- **FR-061**: The producer MUST limit how often one session's callbacks run, 100 per second by default and configurable. Callbacks over the limit are dropped and reported.

Versions and compatibility:

- **FR-044**: The contract between app and producer MUST have a version. Contract versions are whole numbers in order.
- **FR-045**: A producer MUST be able to serve apps with different contract versions and different catalog versions at the same time, each in a form it understands.
- **FR-046**: The producer serves a catalog version when at least one screen is registered for it. It MUST refuse an app whose catalog version it does not serve, with the kind "catalog version not known or no longer supported". A server developer stops serving a catalog version by removing its registration.
- **FR-047**: The producer MUST NOT send a composable that is not in the catalog of the connected app's version.
- **FR-052**: When the app asks for a name that has no screen registered for its catalog version, or a screen fails for any other reason, the producer MUST tell the app the screen cannot be shown and keep the session accepted. The app MUST be able to ask again or ask for another screen.
- **FR-073**: Every composable a screen places MUST carry the catalog version of the bridge it came from. When a screen places a composable from the bridge of a catalog version other than the session's, the producer MUST NOT send that screen. It tells the app the screen cannot be shown, keeps the session accepted, and reports the composable, the screen and both catalog versions to the server developer.

Removed: FR-048, FR-049, FR-050 and FR-051. Screens are now written per catalog version, so the producer no longer adapts a screen to an older catalog, no longer shows a stand-in for a composable an app lacks, and no longer answers whether an app has a composable.

Device context:

- **FR-053**: A server developer MUST be able to read every value of the introduction inside a screen and build the screen from it.
- **FR-054**: When density, orientation, screen size, theme or language changes during a session, the app MUST tell the producer without the user doing anything, and the producer MUST update the open screen in the same session, keeping what the screen holds. Callbacks and device changes from one app are handled one at a time, in the order the producer receives them.
- **FR-055**: The app's version, variant, flavor, catalog version and contract version MUST NOT change during a session.

Safety and reporting:

- **FR-056**: The producer MUST treat every value of an introduction as untrusted, validate all of it before using any of it, and limit its size.
- **FR-057**: Anything invalid or oversized from one app MUST NOT affect another connected app, and what one app says or does MUST NOT reach another app.
- **FR-058**: A server developer MUST be able to observe each refusal, each screen not sent because it placed a composable from another catalog version's bridge, each ignored callback and each screen that could not be shown. The same holds for each callback whose lambda failed, each callback dropped for arriving too often, and each callback whose arguments were refused. A refused callback is reported apart from a callback whose lambda failed.
- **FR-059**: A reason or message that reaches the user MUST NOT reveal internal details.

Build time and build performance:

- **FR-069**: The catalog MUST be needed only while building. The app and the producer MUST NOT need the catalog, or the code that reads it, while they run.
- **FR-070**: An app build or a server build in which nothing changed MUST do no catalog work.
- **FR-071**: A change in one app module MUST produce again, of the catalog, only that module's part and the manifest. The parts of the other modules are reused as they are.
- **FR-072**: What the catalog steps of a build produce MUST hold no absolute path and no timestamp, so a build cache shared between machines and checkouts can reuse it.

### Key Entities

- **Marked composable**: a composable in the app that the app developer made usable by the server.
- **Component**: a marked composable as it appears in a catalog. The word is used only for a catalog entry. What the developer writes and what the user sees is a composable.
- **Catalog**: the description of every marked composable of one app release, with a version. Produced by the app's build, as a bundle of one manifest and its parts. It is the only thing the app's build and the server's build share, and it is used only while building.
- **Manifest**: the entry point of a catalog. It names the catalog version and lists each part with its name, its file name and a fingerprint of its content. The fingerprint of the manifest is the fingerprint of the whole catalog.
- **Part**: the components of one app module, with every choice list they use. A part can be read on its own.
- **Parameter**: one input of a marked composable. It has a name, a kind, and whether it can be left out.
- **Choice list**: a fixed set of named choices that a parameter can take.
- **Callback**: a parameter through which the app tells the server that something happened, with or without data.
- **Slot**: a parameter that holds content the app's composable shows inside itself. It says what content it accepts.
- **Catalog location**: the address of a catalog's manifest. Its parts are found next to it.
- **Saved catalog**: the copy of a catalog inside the server project, with the location it came from and the fingerprint of its manifest.
- **Held catalog**: a saved catalog in the server project. It has its own bridge.
- **Bridge**: what stands for the composables of one held catalog in the server project, so a server developer can call them by name. It holds none of the app's code. Each held catalog has its own bridge, in a place the server developer names.
- **Served catalog version**: a catalog version that has screens registered on the producer. Apps of any other catalog version are refused.
- **Introduction**: what an app says about itself when it first connects: the device context, the app identity, the catalog version and the contract version.
- **Device context**: density, orientation, screen size, theme and language. These can change during a session.
- **App identity**: the app's version, variant and flavor. Fixed for a session and not proven.
- **Contract version**: the version of what the two sides say to each other.
- **Answer**: an acceptance, or a refusal with a kind and a reason.
- **Screen**: what the app asks for by name and the producer builds for that app, as placed composables with their values, their slot content and their callbacks. It is registered for one catalog version and uses that version's bridge.
- **Session**: one accepted connection between one app and the producer. It has at most one open screen, and holds that screen's state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer who knows Compose and the build tool, and has never used the library, can mark one composable, point a separate server project at the produced catalog, write one screen that calls it, and see it on a device in under 30 minutes by following the library's quickstart guide and nothing else.
- **SC-002**: Across every pairing of served app release and producer release, a user sees an unknown or broken composable zero times.
- **SC-003**: A sample server builds and runs with none of the sample app's code available to it, sharing only the catalog.
- **SC-004**: A wrong name, a missing required parameter and a value of the wrong kind each stop the server from building, in every case of the test set. The test set has, for each kind of parameter in FR-004, one screen with a wrong composable name, one that leaves out that parameter when it is required, and one that gives it a value of another kind.
- **SC-005**: An app is accepted and shows its first screen in under 1 second on a local network.
- **SC-006**: After a tap, the updated screen is visible in under 300 milliseconds on a local network.
- **SC-007**: When a device rotates or its theme or language changes, the open screen matches within 1 second, and nothing the user did on that screen is lost.
- **SC-008**: One producer serves at least 3 contract versions and 10 catalog versions at the same time, with every connected app shown a correct screen. Three contract versions is a test target, since only one exists today.
- **SC-009**: The same saved catalog gives the same build result with and without a network.
- **SC-010**: While one app sends invalid or oversized introductions and callback arguments, 1,000 other connected apps see no error, and their screens arrive no more than 100 milliseconds later than without it.
- **SC-011**: A producer holds 10,000 sessions at once, over 10 served catalog versions, without mixing up any two apps.
- **SC-012**: Every refused app shows the user a reason. No refused app shows a blank screen, a frozen screen or a crash.
- **SC-013**: With a catalog of 20,000 components and about 80,000 parameters, spread over many app modules, a second build of the app or of the server with nothing changed does no catalog work: no part, manifest or other catalog output is produced again.
- **SC-014**: With the same catalog, a change to one marked composable in one app module produces again, of the catalog, exactly one part and the manifest. Every other part is reused.
- **SC-015**: The same commit, built from two checkouts in different folders on two machines that share a build cache, gives catalog outputs that are the same byte for byte, and the second build takes every one of them from the cache.
- **SC-016**: With the catalog of 20,000 components held, the catalog adds at most 2 seconds to a clean compile of a server module that calls 20 of its composables, compared with the same module without a catalog. The owner kept this figure after the scale gate measured a prototype. The real plugin meets it. In a clean compile of a 3,000 file module, the plugin's own work took at most 1,139 ms with one held catalog and 1,040 ms with ten over six rounds, measured in process on a warm JVM. Clean wall clock time on the test machine is too noisy to show the cost, so [research.md R16](research.md#what-holds-sc-016) rests the claim on those direct numbers, the edited file compiles and the allocation.

## Assumptions

- The catalog is produced and the bridge is created by the Kotlin compiler plugin, as the owner decided. The spec states what must happen. How the compiler does it belongs to the plan.
- This feature covers the consumer catalog only: the app's composables used by the server. The other direction, server-defined content used by the app under its own name, is a later feature.
- Slots that pass arguments to their content need server-defined content with places for those values. That is the remote composables feature. Here the catalog can describe such a slot, and the server build refuses to fill it.
- Until remote composables exist, all slot content is built from the app's own catalog composables.
- The two sides are named consumer and producer everywhere, as the owner decided. A slot can be marked as accepting only consumer composables (`@ConsumerComposable`) or only producer composables (`@ProducerComposable`), and an unmarked slot accepts either. The catalog describes this now so the format does not have to change later. Checking it is built with the remote composables feature, since in this feature all slot content is the app's own.
- What a screen holds lives as long as its session. When the connection ends it is gone, and an app that connects again starts fresh. Keeping it across connections, and what the user sees when a tap cannot reach the server, belong to the later connection feature.
- A catalog shows the names of an app's components and their parameters to anyone who can read its location. Where it is hosted, and who can read it, is the app team's choice.
- Each release of the library names the version of the Kotlin language tools it supports. A new version is supported once the compiler plugin's tests pass on it.
- Timings in the success criteria are measured on an emulator or a mid-range phone connected to a server on the same machine or local network. Build timings are measured on one stated machine, with the commands written down.
- The first fetch of a catalog trusts its location: nothing proves that the catalog is the app team's. From then on the saved copy and its fingerprint are part of the server project, so the server developer reviews the first saved catalog, and every later change to it, like any other change.
- Where the catalog is hosted is the app team's choice. This feature fetches from a location and does not provide hosting.
- A new app release with a new catalog version needs a producer that serves that catalog version before users get the app. So the producer is released first, or together with the app.
- Screens for two catalog versions repeat their layout, because the two bridges are different sets of functions. State and logic can be shared between them as plain Kotlin. This is a known cost of keeping older apps working without adapting screens for them, and the documentation says so plainly.
- Upper bounds: a catalog has no hard limit on components or parameters. The library is tested at 20,000 components with about 80,000 parameters, which is well above three real apps the owner measured, with 621, 751 and 1,085 composables. The manifest and each part are capped at 16 MiB by default. One producer holds 10,000 sessions and serves 10 catalog versions at once, confirmed by the owner. The size limit for a callback's arguments is an estimate: 1 megabyte by default, and configurable.
- How the two sides are connected, how the connection recovers and how it is encrypted belong to the later connection feature. This feature assumes a two-way connection that delivers what is sent in order and without loss, or ends, and uses a simple one for its tests and samples.
- The library's own messages to the user are in English in this feature. An app developer replaces them to use another language.
- Who the user is, and whether they may see a screen, is outside this feature. Nothing proves the app identity, and each server developer decides how far to trust it.
- Developers need one setting in their editor to see what the compiler creates. That is a documented setup step, not something this feature removes.
- The proof of concept in `poc/` is a reference for behaviour only. Nothing in this feature depends on it.
