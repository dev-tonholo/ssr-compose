# Feature Specification: Consumer Catalog and Bridge

**Feature Branch**: `001-consumer-catalog-bridge`

**Created**: 2026-11-15

**Status**: Draft

**Input**: User description: "The catalog is the composables from the app that server-built UI can interact with. It is produced from annotated composables as a plain descriptor file, not as Kotlin types. The server project points at that file, fetches it at sync, and the compiler creates bridge composables with the same names and parameters. The server can then place the app's composable exactly where it was defined on the server and allow the interaction. The server may live in another repository."

## Clarifications

### Session 2026-11-15

These answers were given by the owner while an earlier draft of this feature was being planned. They are carried over.

- Q: What is the catalog? → A: The composables from the app that server-built UI can interact with.
- Q: Is the catalog written by hand? → A: No. It is produced: annotations on the app's composables and the compiler generate it. It is a simple file of descriptors, not Kotlin types.
- Q: The server can be in another repository, so how does it know the catalog? → A: The server project is given the location of the catalog file, a plain URL is enough. The file is fetched during the build tool's sync step, never during compilation. The compiler then creates the bridge definitions from it.
- Q: Does the generated code contain the app's code? → A: No. It is only a bridge. The app's components exist only inside the consumer.
- Q: When an app's component has a slot that takes arguments, who supplies them? → A: The app's component, since it is the one that calls the slot. The server only consumes the values and fills server-defined content with them.
- Q: When an app connects, does it send its catalog? → A: No, only the catalog version. The producer must always know the consumer's catalog. The app may not know a new screen, but the producer always knows the app's catalog.
- Q: An older app has a component with fewer parameters than the screen gives. What happens? → A: The producer fits what it sends to that app's version. It drops what the older version does not have, and treats the component as missing only when the older version needs something the screen did not give.
- Q: A screen uses a component the app does not have and no replacement was defined. What does the user lose? → A: The whole screen. A screen with a silent hole can mislead.
- Q: How large should one producer go? → A: Up to 1,000 components per catalog, 50 parameters per component, 10,000 connected apps per producer, and 10 catalog versions served at once.
- Q: May a server developer trust the app's own version, variant and flavor to decide who sees what? → A: Left open. Nothing proves those values, and each server developer decides.
- Q: Are modifiers part of this? → A: No. Modifiers are out of scope for now.
- Q: How does a callback with more than one argument look on the server? → A: It follows the pattern of Kotlin's function types: one type per number of arguments, with no result type.

## User Scenarios & Testing *(mandatory)*

Three people are served by this feature:

- The app developer, who marks which of the app's composables a server may use.
- The server developer, who writes screens in a project that may live in another repository.
- The end user of the app, who must never see a broken or unknown composable because the app and the server disagree.

### User Story 1 - A server screen places an app composable (Priority: P1)

An app developer marks a composable as usable by the server. Building the app produces a catalog file that describes it: its name, its parameters, and any choice types they use. The server developer points the server project at that file. After a sync, the server developer can call the composable by the same name with the same parameters, as if it were part of the server project, even though none of the app's code is there. The app connects, introduces itself, and shows the screen. The composable is drawn by the app's own code, with the values the server gave.

**Why this priority**: It is the smallest slice that proves the whole chain: mark, produce, share, bridge, connect, render. It puts something on a real device, and every other story builds on it.

**Independent Test**: Mark one composable in a sample app, build it, point a sample server in a separate build at the produced file, write one screen that calls it, run both, and see the composable on a device with the server's values.

**Acceptance Scenarios**:

1. **Given** a composable marked for the server, **When** the app is built, **Then** a catalog file exists that describes the composable's name, its parameters and the choice types they use.
2. **Given** a server project pointed at a catalog file, **When** the project is synced, **Then** the server developer can call each catalog composable by its own name and parameters, and the server project holds none of the app's code.
3. **Given** a server screen that calls a catalog composable, **When** the server developer gives a wrong name, a missing parameter or a value of the wrong type, **Then** the server does not build.
4. **Given** an app and a server built this way, **When** the app connects and asks for the screen, **Then** the app shows the composable, drawn by the app's own code, with the values the server gave.
5. **Given** a connecting app, **When** it introduces itself, **Then** the producer receives the device density, orientation, screen size, active theme, active language, the app's version, variant and flavor, the catalog version, and the contract version the app speaks, and no catalog.
6. **Given** an app composable with a parameter the catalog cannot describe, **When** the app is built, **Then** the build fails with a message that names the composable, the parameter and the reason.
7. **Given** a saved catalog and a location whose file changed since, **When** the server project is synced, **Then** the difference is reported, the saved copy is kept, and the build still uses it.
8. **Given** a saved catalog and a location that cannot be reached, **When** the server project is synced and built, **Then** the developer is told, and the build gives the same result as before.
9. **Given** no saved catalog and a location that cannot be reached, **When** the server project is synced, **Then** the sync fails with a message that names the location.

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

People can stop an app from updating. So apps of several releases are connected to the same producer, each with the catalog it shipped with. The producer holds the catalog file of every release it supports, tells the apps apart, and gives each one only what it has. When the producer no longer supports an app, it refuses clearly, and the user is told why and what to do.

**Why this priority**: Without this, every server release risks breaking installed apps. It comes after the first three stories because there is nothing to keep compatible until they exist.

**Independent Test**: Run one producer against two app builds made from different catalog versions, and against one app the producer no longer supports.

**Acceptance Scenarios**:

1. **Given** two apps with different catalog versions connected at once, **When** each asks for the same screen, **Then** each gets a screen built only from what its own catalog has.
2. **Given** a screen that uses a composable an older app does not have, **When** the server developer defined a replacement, **Then** the older app shows the replacement and nothing about the missing composable.
3. **Given** the same case with no replacement, **When** the screen is produced, **Then** the older app is told the screen cannot be shown, no partial screen is sent, and the server reports the composable, the screen and the catalog version.
4. **Given** an older app whose version of a composable has fewer parameters, **When** the screen is produced, **Then** that app gets the composable with only the parameters its catalog has.
5. **Given** an app whose catalog version or contract version the producer does not support, **When** it connects, **Then** it is refused with a reason, gets no screen content, and the user sees why and what to do.
6. **Given** a new app release whose catalog the server project has not synced yet, **When** that app connects, **Then** it is refused in the same clear way until the producer knows its catalog.

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

- Two composables marked for the server have the same name. The app build fails and names both. (FR-006)
- A marked composable takes a modifier. Modifiers are out of scope: the parameter is left out of the catalog and the composable gets its default. (FR-007)
- The catalog file at the given location changed since it was last saved, without the server developer asking for it. The saved copy is still used, and the difference is reported at the next sync. (FR-016)
- The location cannot be reached during a sync. The saved copy is used and the developer is told. With no saved copy, the sync fails with a clear message. (FR-015)
- The fetched file is not a catalog, or holds names that cannot be used as names in code. It is rejected and nothing is created from it. (FR-017)
- The app developer changes a marked composable but does not raise the catalog version. The app build fails and says the catalog changed. (FR-010)
- The app developer sets a catalog version that has no saved catalog yet. The app build fails and says how to save one. (FR-010)
- A callback's arguments are far larger than any real ones. The producer refuses them without slowing other connected apps. (FR-043, FR-057)
- The app triggers a callback many times in a moment. Each one runs in the order it was triggered. (FR-040, FR-061)
- An app introduces itself with a value that makes no sense, or with something far larger than a real introduction. It is refused, and other apps are not affected. (FR-056, FR-057)
- The app asks for a screen before it was accepted, or for a name the producer does not have. (FR-031, FR-052)
- A device value changes while the introduction is still being answered. The app reports the newest values once it is accepted. (FR-054)
- An app composable has an optional slot that passes arguments. The server can place the composable and cannot fill that slot. (FR-036)
- An app has no marked composable. Its catalog is valid and empty, the app is accepted, and every composable is missing for it. (FR-002, FR-049)
- A catalog goes over a stated bound, such as component 1,001 or parameter 51. The app build fails and names the bound. (FR-006)
- The app receives something it cannot draw, although it should never happen. It does not crash and does not show a developer placeholder. (FR-037)

## Requirements *(mandatory)*

### Functional Requirements

Marking composables and producing the catalog:

- **FR-001**: An app developer MUST be able to mark a composable in the app as usable by the server, in the place where the composable is written. Only a top-level composable function with no type parameters can be marked.
- **FR-002**: Building the app MUST produce a catalog file that describes every marked composable. The app developer never writes this file by hand.
- **FR-003**: The catalog file MUST describe, for each composable: its name, and each parameter's name, kind, and whether it can be left out. A value or choice parameter also says if it can be empty. "Can be left out" means the app's composable has its own default for the parameter, so the server may give nothing. "Can be empty" means the app's composable accepts no value for it, so the server may give "no value" on purpose. The two are independent: a parameter can be one, both or neither. A component's name is its identity: renaming a marked composable changes the catalog and needs a new catalog version.
- **FR-004**: A parameter MUST be one of these kinds: a plain value (text, whole number, decimal number, yes or no), a choice from a fixed list, a list of one kind, a callback with no arguments, a callback with up to four arguments, a slot, or a slot whose content receives arguments. A marked composable with a callback of more than four arguments MUST fail the app build with a message that says so. A callback argument is a plain value, a choice, or a block of bytes.
- **FR-005**: The catalog file MUST describe each fixed list of choices that a parameter uses, with its name and its choices.
- **FR-006**: The app build MUST fail, with a message that names the composable, the parameter and the reason, when a marked composable has a parameter the catalog cannot describe, and when two marked composables share a name. The build MUST fail in the same way when a marked function is a member, an extension or has type parameters, and when a catalog goes over a stated bound.
- **FR-007**: A modifier parameter MUST be left out of the catalog. The composable is drawn with that parameter's default.
- **FR-008**: The catalog file MUST be plain, readable text that produces the same file for the same composables, so a change to the catalog shows up as a small, reviewable difference.
- **FR-009**: A catalog MUST have a version the app developer sets. Catalog versions are labels with no order. An app with several build variants has a catalog version per variant, the project's by default, and variants that share a version MUST have the same marked composables.
- **FR-010**: The app build MUST fail when the marked composables no longer match the saved catalog of the same version, so a catalog cannot change without a new version. When no saved catalog exists yet for that version, the build MUST fail and say how to save one. It MUST NOT create one by itself, so a new catalog version is always a deliberate step.
- **FR-011**: The catalog format MUST be able to say, for each slot, what content it accepts: anything, only consumer composables (the app's own), or only producer composables (server-defined). In this feature all slot content is consumer composables, and the restriction is not checked yet.
- **FR-063**: Marked composables MAY live in several modules of one app. The catalog covers all of them, and a name used in two modules MUST fail the app build.

Sharing the catalog with the server project:

- **FR-012**: The app's code and the server's code MUST be able to live in different repositories. The catalog file MUST be the only thing the two builds share. This project's own libraries, which both builds use, are not part of what they share with each other.
- **FR-013**: A server developer MUST be able to give the server project the location of each catalog file it uses. A plain `https` address MUST be enough, and a file path MUST work too.
- **FR-014**: The catalog MUST be fetched during the build tool's sync step, and MUST NOT be fetched while code is being compiled. The sync step is a step of the build tool that a developer or a build server runs on purpose, and that an editor's sync also triggers where the build tool allows.
- **FR-015**: A fetched catalog MUST be saved inside the server project with a fingerprint of its content. Builds MUST use the saved copy, MUST work with no network, and MUST give the same result for the same saved copy.
- **FR-016**: When the catalog at a location no longer matches the saved fingerprint, the sync MUST say so and MUST NOT replace the saved copy unless the developer asks for it.
- **FR-017**: A fetched catalog MUST be checked before anything is created from it. A file that is not a valid catalog, or that holds names that cannot be used as names in code, MUST be rejected. A name can be used in code when it starts with a letter or an underscore, holds only letters, digits and underscores, is not a reserved word of the language, and is at most 64 characters long. This holds for every component, parameter, choice list and choice.
- **FR-018**: A server project MUST be able to hold the catalogs of several app releases at once, to say which ones it supports, and to say which one the bridge is created from.
- **FR-019**: The same steps MUST be available from every supported build tool. Nothing here may work with only one of them.

The bridge on the server:

- **FR-020**: After a sync, a server developer MUST be able to call each composable of one held catalog by its own name and with its own parameters, as if it were written in the server project. The server developer names which held catalog that is, since catalog versions have no order. The server developer chooses where in the server project the bridge appears. A composable that exists only in another held catalog has no bridge and cannot be named in a screen. So a composable that was removed or renamed in the bridge catalog cannot be shown to apps of an older catalog, not even as a replacement, and a screen that needs it cannot be shown to them. An app team that wants older apps to keep a composable keeps it, under the same name, until the catalog versions that need it are no longer supported. The documentation for app developers MUST say so.
- **FR-021**: The bridge MUST hold none of the app's code. It only stands for the app's composable.
- **FR-022**: The server MUST NOT build when a screen calls a catalog composable with a wrong name, without a required parameter, or with a value of the wrong kind.
- **FR-023**: On the server, a callback parameter MUST be given as a lambda the server developer writes, and a slot parameter MUST be given as content the server developer writes.
- **FR-024**: A server developer MUST be able to do by hand everything the build creates for the bridge, so the libraries can be used and tested without it. The same holds on the app side: an app developer MUST be able to connect each marked composable to what the server sends by hand.
- **FR-025**: The setup a developer's editor needs to show the bridge MUST be documented.
- **FR-064**: The documentation for app developers and the documentation for server developers MUST both state the release order: a producer that has synced a new catalog version is released before, or together with, the app that ships it.

Connecting and introducing:

- **FR-026**: When an app first connects, it MUST introduce itself before anything else is exchanged.
- **FR-027**: The introduction MUST carry: device density, device orientation, screen size, active theme (dark or light), active language, the app's version, variant and flavor, the catalog version, and the contract version the app speaks. It MUST NOT carry the catalog.
- **FR-028**: The consumer library MUST collect the device values by itself. The app developer supplies only the app's version, variant and flavor.
- **FR-029**: The producer MUST answer every introduction with an acceptance that states the contract version it will speak, or with a refusal.
- **FR-030**: A refusal MUST carry a reason the app can show the user, and a kind: the contract version is too old, the contract version is too new, the catalog version is not known or no longer supported, or the introduction is not valid.
- **FR-031**: The producer MUST NOT send screen content to an app it has not accepted.
- **FR-032**: The app MUST show the user a message when it is refused, saying why in one sentence and what the user can do. The app developer MUST be able to replace it with the app's own presentation.

Showing a screen:

- **FR-033**: A screen MUST have a name the server developer gives it, and the app asks for a screen by that name. A session has at most one open screen, and asking for another replaces it.
- **FR-034**: The app MUST draw each composable the server placed with the app's own code for it, in the place and order the server put it, with the values the server gave. When a screen places more than one composable at its top level, they are shown top to bottom. This arrangement is provisional until layout has its own feature.
- **FR-035**: Content the server put in a slot MUST appear where the app's composable places that slot, and slots inside slots MUST keep their nesting.
- **FR-036**: In this feature, a slot can be filled only with the app's own catalog composables, and a slot whose content receives arguments cannot be filled at all. When such a slot can be left out, the server has no way to give it and the app uses its own default. When it cannot be left out, the composable cannot be placed by the server, and trying to MUST fail the server build with a message that names the composable and the slot, and says that slots with arguments are not supported yet.
- **FR-037**: If the app receives something it cannot draw, it MUST NOT crash and MUST NOT show a developer placeholder. It tells the user the screen cannot be shown and reports the mismatch.
- **FR-062**: When the server does not give a parameter that can be left out, nothing is sent for it and the app's composable uses its own default. This holds for values, callbacks and slots. The catalog never holds the default, since only the app knows it.

Callbacks:

- **FR-038**: When the user triggers a callback of a placed composable, the lambda the server developer gave for it MUST run on the server, in that app's session.
- **FR-039**: A callback with arguments MUST deliver each of them to the server lambda, in order, with the kinds the composable declared. A callback never gives a result back to the app, since the app's callback returns at once and cannot wait for the server.
- **FR-040**: Callbacks from one app MUST run in the order they were triggered.
- **FR-041**: When a lambda changes what the screen holds, the app MUST get the updated screen without asking.
- **FR-042**: A trigger for a callback the screen no longer has MUST be ignored and MUST NOT end the session.
- **FR-043**: Callback arguments from an app are untrusted. The producer MUST limit their size and refuse what goes over the limit.
- **FR-060**: When a lambda fails while it runs for a callback, the changes it made MUST be thrown away, the screen MUST stay as it was, the failure MUST be reported to the server developer, and the session MUST carry on. The user is not shown an error for it.
- **FR-061**: The producer MUST limit how often one session's callbacks run, 100 per second by default and configurable. Callbacks over the limit are dropped and reported.

Versions and compatibility:

- **FR-044**: The contract between app and producer MUST have a version. Contract versions are whole numbers in order.
- **FR-045**: A producer MUST be able to serve apps with different contract versions and different catalog versions at the same time, each in a form it understands.
- **FR-046**: The producer MUST refuse an app whose catalog version it does not hold or does not support.
- **FR-047**: The producer MUST NOT send a composable that is not in the catalog of the connected app's version.
- **FR-048**: The producer MUST fit what it sends to the connected app's catalog version: it leaves out parameters that version does not have. An app MUST never receive a parameter its catalog did not describe.
- **FR-049**: When the connected app's version of a composable needs a parameter the screen did not give, or has a parameter of a different kind, the composable MUST be treated as missing for that app. The same holds when the screen gives a choice that the connected app's version of the list does not have.
- **FR-050**: A server developer MUST be able to define what is shown when a composable is missing for a connected app, and to ask inside a screen whether the connected app has a composable.
- **FR-051**: When a composable is missing and no replacement is defined, the producer MUST stop that screen, report the composable, the screen and the catalog version, and tell the app the screen cannot be shown. It MUST NOT send the rest of the screen.
- **FR-052**: When the app asks for a name the producer does not have, or a screen fails for any other reason, the producer MUST tell the app the screen cannot be shown and keep the session accepted. The app MUST be able to ask again or ask for another screen.

Device context:

- **FR-053**: A server developer MUST be able to read every value of the introduction inside a screen and build the screen from it.
- **FR-054**: When density, orientation, screen size, theme or language changes during a session, the app MUST tell the producer without the user doing anything, and the producer MUST update the open screen in the same session, keeping what the screen holds. Callbacks and device changes from one app are handled one at a time, in the order the producer receives them.
- **FR-055**: The app's version, variant, flavor, catalog version and contract version MUST NOT change during a session.

Safety and reporting:

- **FR-056**: The producer MUST treat every value of an introduction as untrusted, validate all of it before using any of it, and limit its size.
- **FR-057**: Anything invalid or oversized from one app MUST NOT affect another connected app, and what one app says or does MUST NOT reach another app.
- **FR-058**: A server developer MUST be able to observe each refusal, each missing composable, each parameter left out, each ignored callback and each screen that could not be shown. The same holds for each callback whose lambda failed and each callback dropped for arriving too often.
- **FR-059**: A reason or message that reaches the user MUST NOT reveal internal details.

### Key Entities

- **Marked composable**: a composable in the app that the app developer made usable by the server.
- **Component**: a marked composable as it appears in a catalog. The word is used only for a catalog entry. What the developer writes and what the user sees is a composable.
- **Catalog**: the description of every marked composable of one app release, with a version. Produced by the app's build.
- **Catalog file**: the catalog as plain text. It is the only thing the app's build and the server's build share.
- **Parameter**: one input of a marked composable. It has a name, a kind, and whether it can be left out.
- **Choice list**: a fixed set of named choices that a parameter can take.
- **Callback**: a parameter through which the app tells the server that something happened, with or without data.
- **Slot**: a parameter that holds content the app's composable shows inside itself. It says what content it accepts.
- **Catalog location**: where a server project fetches a catalog file from.
- **Saved catalog**: the copy of a catalog file inside the server project, with a fingerprint of its content.
- **Held catalog**: a saved catalog in the server project. The producer knows what it describes.
- **Supported catalog**: a held catalog whose apps the producer accepts. Every held catalog is supported unless the server developer says otherwise. An app with a catalog that is held but not supported is refused.
- **Bridge catalog**: the one held catalog the bridge is created from.
- **Bridge**: what stands for an app composable in the server project, so a server developer can call it by name. It holds none of the app's code.
- **Introduction**: what an app says about itself when it first connects: the device context, the app identity, the catalog version and the contract version.
- **Device context**: density, orientation, screen size, theme and language. These can change during a session.
- **App identity**: the app's version, variant and flavor. Fixed for a session and not proven.
- **Contract version**: the version of what the two sides say to each other.
- **Answer**: an acceptance, or a refusal with a kind and a reason.
- **Screen**: what the app asks for by name and the producer builds for that app, as placed composables with their values, their slot content and their callbacks.
- **Replacement**: what a server developer says to show when a composable is missing for a connected app.
- **Session**: one accepted connection between one app and the producer. It has at most one open screen, and holds that screen's state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer who knows Compose and the build tool, and has never used the library, can mark one composable, point a separate server project at the produced catalog, write one screen that calls it, and see it on a device in under 30 minutes by following the library's quickstart guide and nothing else.
- **SC-002**: Across every pairing of supported app release and producer release, a user sees an unknown or broken composable zero times.
- **SC-003**: A sample server builds and runs with none of the sample app's code available to it, sharing only the catalog file.
- **SC-004**: A wrong name, a missing required parameter and a value of the wrong kind each stop the server from building, in every case of the test set. The test set has, for each kind of parameter in FR-004, one screen with a wrong composable name, one that leaves out that parameter when it is required, and one that gives it a value of another kind.
- **SC-005**: With a catalog of 200 components, an app is accepted and shows its first screen in under 1 second on a local network.
- **SC-006**: After a tap, the updated screen is visible in under 300 milliseconds on a local network.
- **SC-007**: When a device rotates or its theme or language changes, the open screen matches within 1 second, and nothing the user did on that screen is lost.
- **SC-008**: One producer serves at least 3 contract versions and 10 catalog versions at the same time, with every connected app shown a correct screen. Three contract versions is a test target, since only one exists today.
- **SC-009**: The same saved catalog gives the same build result with and without a network.
- **SC-010**: While one app sends invalid or oversized introductions and callback arguments, 1,000 other connected apps see no error, and their screens arrive no more than 100 milliseconds later than without it.
- **SC-011**: A producer holds 10,000 sessions at once, over 10 catalog versions of up to 1,000 components with up to 50 parameters each, without mixing up any two apps.
- **SC-012**: Every refused app shows the user a reason. No refused app shows a blank screen, a frozen screen or a crash.

## Assumptions

- The catalog is produced and the bridge is created by the Kotlin compiler plugin, as the owner decided. The spec states what must happen. How the compiler does it belongs to the plan.
- This feature covers the consumer catalog only: the app's composables used by the server. The other direction, server-defined content used by the app under its own name, is a later feature.
- Slots that pass arguments to their content need server-defined content with places for those values. That is the remote composables feature. Here the catalog can describe such a slot, and the server build refuses to fill it.
- Until remote composables exist, a replacement and all slot content are built from the app's own catalog composables.
- The two sides are named consumer and producer everywhere, as the owner decided. A slot can be marked as accepting only consumer composables (`@ConsumerComposable`) or only producer composables (`@ProducerComposable`), and an unmarked slot accepts either. The catalog describes this now so the format does not have to change later. Checking it is built with the remote composables feature, since in this feature all slot content is the app's own.
- What a screen holds lives as long as its session. When the connection ends it is gone, and an app that connects again starts fresh. Keeping it across connections, and what the user sees when a tap cannot reach the server, belong to the later connection feature.
- A catalog file shows the names of an app's components and their parameters to anyone who can read its location. Where it is hosted, and who can read it, is the app team's choice.
- Each release of the library names the version of the Kotlin language tools it supports. A new version is supported once the compiler plugin's tests pass on it.
- Timings in the success criteria are measured on an emulator or a mid-range phone connected to a server on the same machine or local network.
- The first fetch of a catalog trusts its location: nothing proves that the file is the app team's. From then on the saved copy and its fingerprint are part of the server project, so the server developer reviews the first saved file, and every later change to it, like any other change.
- Where the catalog file is hosted is the app team's choice. This feature fetches from a location and does not provide hosting.
- A new app release with a new catalog version needs a producer that has synced that catalog before users get the app. So the producer is released first, or together with the app.
- Upper bounds, confirmed by the owner: 1,000 components per catalog, 50 parameters per component, 10,000 sessions on one producer, 10 catalog versions served at once. The size limit for a callback's arguments is an estimate: 1 megabyte by default, and configurable.
- How the two sides are connected, how the connection recovers and how it is encrypted belong to the later connection feature. This feature assumes a two-way connection that delivers what is sent in order and without loss, or ends, and uses a simple one for its tests and samples.
- The library's own messages to the user are in English in this feature. An app developer replaces them to use another language.
- Who the user is, and whether they may see a screen, is outside this feature. Nothing proves the app identity, and each server developer decides how far to trust it.
- Developers need one setting in their editor to see what the compiler creates. That is a documented setup step, not something this feature removes.
- The proof of concept in `poc/` is a reference for behaviour only. Nothing in this feature depends on it.
