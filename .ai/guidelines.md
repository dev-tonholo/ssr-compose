# ssr-compose agent guidelines

This is the entry point for every AI agent working in this repository. Read it first. Then read the rule files that match your task.

## Before you start (required)

1. For any request that changes code, tests, docs, config, assets, work items, branches or reviews: load .ai/agents/engineering-manager.md and act as the engineering-manager. Questions and exploration run without the crew. If you were dispatched as a crew role (your brief contains a "Crew role:" line, or you are an Orca worker with Task and Dispatch IDs), skip this line and do only your role.
2. Load the `android-cli` skill before the first device step. Use `android input` for taps, typing, swipes and keys, also where that skill shows `adb shell input`.
3. Read the [constitution](../.specify/memory/constitution.md). It comes before every other file here.
4. Read this file, then the rules for your task.

## Project

ssr-compose is server-side rendering for Jetpack Compose. A producer server runs composable logic and a consumer app shows the result. It is used by app and server developers who want screens to ship from the server.

Main stack:

- Kotlin with the K2 compiler. Style is in [rules/code-style.md](rules/code-style.md).
- Compose for rendering. The consumer library is Kotlin Multiplatform with Android as its only target. See [rules/ui-compose.md](rules/ui-compose.md).
- Four deliverables: a consumer library, a producer library, a core library and a Kotlin compiler plugin. See [rules/architecture.md](rules/architecture.md).
- Gradle with a version catalog and convention plugins. The libraries must also be consumable from the Kotlin toolchain.
- Features go through Spec Kit: specify, plan, tasks, then checklists.
- Layout decision for this repo: [ADR 0001](../docs/adr/0001-keep-the-poc-in-a-folder.md).

State of the product: design stage. The root Gradle build and the library module shapes exist, with one trivial class each and one trivial contract test, and no feature code yet. The only working code is the proof of concept in `poc/`, which is a reference and is not held to the library's rules.

## Module map

The root is a Gradle build with the library modules and the included build `build-logic`. List the root modules with `./gradlew projects`. The proof of concept is its own, separate Gradle project in `poc/`. List its modules with `./poc/gradlew -p poc projects`.

| Folder | CLI name | Type | Purpose | May depend on |
|--------|----------|------|---------|---------------|
| `core` | `:core` | Kotlin Multiplatform library (`jvm`, `android`) | Document format, catalog, handshake and messages shared by both sides | nothing in this repo |
| `core-testing` | `:core-testing` | Kotlin Multiplatform library (`jvm`, `android`) | Published test support for `core`: `InMemoryConnection` and its `pair()` | `:core` |
| `consumer` | `:consumer` | Kotlin Multiplatform library (`android` only) | Consumer library that renders screens | `:core` |
| `producer` | `:producer` | Kotlin JVM library | Producer library that runs composables | `:core` |
| `compiler-plugin` | `:compiler-plugin` | Kotlin JVM | Kotlin compiler plugin | `:core` |
| `tooling` | `:tooling` | Kotlin JVM library | Shared logic for the build-tool plugins | `:core` |
| `gradle-plugin` | `:gradle-plugin` | Gradle plugin | Thin Gradle wrapper over `:tooling` | `:tooling` |
| `contract-tests` | `:contract-tests` | Test-only module (Android host tests) | Runs the producer's output through the consumer's reader | `:core`, `:core-testing`, `:consumer`, `:producer` |
| `build-logic` | included build | Gradle convention plugins | Shared build setup, named `dev.tonholo.ssrcompose.conventions.<concern>` | nothing in this repo |
| `poc/protocol` | `:protocol` | Kotlin JVM library | Node tree, service interface and component contracts shared by both sides | nothing in this repo |
| `poc/server` | `:server` | Kotlin JVM application | Ktor server that runs composables and serves sessions | `:protocol` |
| `poc/app` | `:app` | Android application | App that renders trees and plays fragments | `:protocol` |
| `poc/build-logic` | included build | Gradle convention plugins | Shared build setup for the PoC | nothing in this repo |

Dependencies point to the shared contract only: the two sides never depend on each other. Details and known violations are in [rules/architecture.md](rules/architecture.md).

Root versions live in `gradle/libs.versions.toml`, and the shared build config in `build-logic/src/main/kotlin/`. PoC versions live in `poc/gradle/libs.versions.toml`, and its shared build config in `poc/build-logic/src/main/kotlin/`.

## Commands

Run all commands from the repo root.

| Task | Command |
|------|---------|
| Build and test the root modules | `./gradlew build` |
| Run detekt on the root modules (config in `config/detekt/detekt.yml`; `./gradlew build` runs it too) | `./gradlew detekt` |
| List the root modules | `./gradlew projects` |
| Run the contract tests (Android host tests) | `./gradlew :contract-tests:testAndroidHostTest` |
| Build and test the PoC, with detekt | `./poc/gradlew -p poc build detekt` |
| Run one PoC module's tests | `./poc/gradlew -p poc :server:test` |
| Run the PoC server (port 8787) | `./poc/gradlew -p poc :server:run` |
| Build the PoC app | `./poc/gradlew -p poc :app:assembleDebug` |
| List devices | `adb devices` |
| Forward the server port to a device | `adb -s <serial> reverse tcp:8787 tcp:8787` |
| Install and launch the PoC app | `android run --device=<serial> --apks=poc/app/build/outputs/apk/debug/app-debug.apk --activity=dev.tonholo.ssrcompose.app.MainActivity` (Not verified: not run with the `android` CLI when this file was written) |
| Run the PoC UI tests | `ANDROID_SERIAL=<serial> ./poc/gradlew -p poc :app:connectedDebugAndroidTest` (needs a device) |
| Check agent links | `python3 .ai/scripts/check-links.py` |

More detail: [skills/build-and-verify/SKILL.md](skills/build-and-verify/SKILL.md).

## Crew

- Crew version: 5
- Seats: kept in `.crew/seats`, one `<role>:<driver|passenger>` line per role (no line means driver), ignored by git, so each person keeps their own. A passenger engineering-manager waits for the owner's go before each dispatch; a passenger role is dispatched only after the owner's go. Switch at any time by saying "drive", "passenger", or "<role> passenger".
- Roles: `engineering-manager` (main session playbook), `worker`, `reviewer`, `qa`, `advisor`. Role files live in `.ai/agents/`; each tool's copy is generated by `python3 .ai/scripts/sync-agents.py`.
- Work items and reviews: GitHub issues and pull requests.
- Lessons: `.crew/lessons.md`.
- Workspaces: `.crew/worktrees/`, one per item, always ignored by git. Inside Orca, Orca's own workspaces are used instead.
- `.crew/` is ignored by git.
- Preferred tool per role inside Orca: none.
- Shell calls: every role runs one plain command per call. No `;`, `&&`, `||`, `$?`, `echo` markers or pipes, and no `cd <dir> &&`; use `git -C <dir>` or the tool's own path argument instead. Run repo scripts by relative path only (`python3 .ai/scripts/<name>.py`); they check the folder they run in, so script checks inside a workspace belong to the worker, and the reviewer reads the worker's reported output and the diff. In git format strings use plain placeholders such as `%B`, `%an` and `%ae`, never `%(...)`. Permission checks reject anything else, and a denial costs a retry.
- Owner's tree: never run `git stash`, `git reset --hard`, `git clean` or `git checkout .` in the owner's working tree. Work in a workspace instead. Before moving, merging or deleting files, search the repo's tests and scripts for those paths and run the tests that read them.
- Check the crew folder: `python3 .ai/scripts/check-crew.py`. Check role files: `python3 .ai/scripts/lint-roles.py .ai/agents`.

### QA targets

| Target | How to run it | Evidence |
|---|---|---|
| PoC server | `./poc/gradlew -p poc :server:run`, then `lsof -ti tcp:8787 -sTCP:LISTEN` to confirm it listens | The process ID on port 8787 and the output of `./poc/gradlew -p poc :server:test` |
| PoC Android app | With the server running: `adb devices`, `adb -s <serial> reverse tcp:8787 tcp:8787`, `./poc/gradlew -p poc :app:assembleDebug`, then `android run --device=<serial> --apks=poc/app/build/outputs/apk/debug/app-debug.apk --activity=dev.tonholo.ssrcompose.app.MainActivity` (Not verified: not run with the `android` CLI when this file was written) | A screenshot from `android screen capture --device=<serial> -o <file>.png` and the session log from `adb -s <serial> logcat -d -s SsrSession:D` |
| Library modules (consumer, producer, core, compiler plugin) | `./gradlew build` | Exit status 0, and the contract test result file `contract-tests/build/test-results/testAndroidHostTest/TEST-dev.tonholo.ssrcompose.contracttests.BuildExperimentTest.xml` showing `tests="1"` and `failures="0"` |

A target with no known way to run it is listed with "none found"; QA returns `INCOMPLETE` for it.

### Specialists

Roles beyond the core crew, each with its own scope and knowledge skill. The engineering-manager dispatches a specialist only for work inside its scope.

None yet.

## Host and VCS mapping

Roles use the actions on the left. This table says how each one is done in this repo.

| Action | GitHub |
|---|---|
| Read a work item | `gh issue view <id>` |
| Create a work item | `gh issue create` |
| Update item status | issue state and labels |
| Record blockers | native "blocked by" links |
| Open a review request | `gh pr create` |
| Record role status | PR labels `agents: <role> required`, `in-progress`, `done`, or the repo's own status labels |
| Link review to item | `Closes #<id>` in the PR body |
| Close the item after merge | verify the issue closed |
| Create a branch | `git switch -c <type>/<issue-number>/<brief-description>` |
| Create a workspace | `git worktree add <workspaces>/<id>-<slug> -b <branch>` |
| Commit | `git commit` |

## Model tiers

Each role names a tier (`max`, `top`, `mid` or `small`) and an effort. `.ai/agents.json` maps each tier to a model per tool; edit it there and run `python3 .ai/scripts/sync-agents.py`. `max` is for the advisor only; a tool with no `max` entry uses its `top` model.

The engineering-manager sets the worker and qa tier from the task size:

| Size | Looks like | worker | qa |
|---|---|---|---|
| trivial | mechanical, one file, no logic change | small, low | small, low |
| small | one behavior, a few files, tests and commands spelled out | small, low | small, low |
| medium | several files or modules, follows an existing pattern | mid, medium | small, low |
| complex | new pattern, crosses modules, or touches security, data or concurrency | mid, high | mid, medium |

The reviewer is never below `mid`. A failed run is retried once, one tier up but never above `top`, then stops.

## Budget

Per work item:

- at most 1 retry per brief, one tier up
- at most 12 dispatches in total
- at most 3 workers at the same time

Going over stops the run with a report to the owner.

## Stop points

The engineering-manager stops and asks the owner before:

- approving a design
- merging
- pushing, publishing, or anything else that leaves this machine
- deleting anything that is not a crew workspace
- changing `.ai/guidelines.md`, `.ai/rules/`, or moving a lesson into them

## Verification before you call work done

1. `./poc/gradlew -p poc build detekt` passes when you touched `poc/`.
2. `./gradlew build` passes when you touched anything outside `poc/`.
3. Tests pass. New behavior has a test that failed before the change.
4. `python3 .ai/scripts/sync-agents.py --check` passes.
5. You checked your diff against every file in `rules/`. Use the [reviewer](agents/reviewer.md) agent for a second look.
6. Changes to the connection, the handshake, documents or sessions were checked against every rule in [rules/security.md](rules/security.md).
7. Rendering changes were looked at on a running app, on a device or emulator.
8. Commits are small and follow [rules/git.md](rules/git.md).

If a step cannot run, say so in your report. Do not claim it passed. A command that did not pass when this file was written carries the note `Not verified: <reason>`.

## Index

Rules:

- [rules/code-style.md](rules/code-style.md): Kotlin style.
- [rules/architecture.md](rules/architecture.md): deliverables, boundaries and shared contracts.
- [rules/testing.md](rules/testing.md): tests first, naming and where tests live.
- [rules/security.md](rules/security.md): connection, documents, sessions and secrets.
- [rules/git.md](rules/git.md): commits, branches and what is never committed.
- [rules/documentation.md](rules/documentation.md): comments, writing style and where docs live.
- [rules/ui-compose.md](rules/ui-compose.md): Compose rules and the visual check.

Skills:

- [skills/build-and-verify/SKILL.md](skills/build-and-verify/SKILL.md)
- [skills/add-dependency/SKILL.md](skills/add-dependency/SKILL.md)
- [skills/speckit-constitution/SKILL.md](skills/speckit-constitution/SKILL.md)
- [skills/speckit-specify/SKILL.md](skills/speckit-specify/SKILL.md)
- [skills/speckit-clarify/SKILL.md](skills/speckit-clarify/SKILL.md)
- [skills/speckit-plan/SKILL.md](skills/speckit-plan/SKILL.md)
- [skills/speckit-tasks/SKILL.md](skills/speckit-tasks/SKILL.md)
- [skills/speckit-checklist/SKILL.md](skills/speckit-checklist/SKILL.md)
- [skills/speckit-analyze/SKILL.md](skills/speckit-analyze/SKILL.md)
- [skills/speckit-implement/SKILL.md](skills/speckit-implement/SKILL.md)
- [skills/speckit-converge/SKILL.md](skills/speckit-converge/SKILL.md)

Agents:

- [agents/engineering-manager.md](agents/engineering-manager.md)
- [agents/worker.md](agents/worker.md)
- [agents/reviewer.md](agents/reviewer.md)
- [agents/qa.md](agents/qa.md)
- [agents/advisor.md](agents/advisor.md)

Blueprints:

- [blueprints/unit-test.md](blueprints/unit-test.md)
- [blueprints/adr.md](blueprints/adr.md)

Where to look per module or area: [registry.md](registry.md).

## Tool wiring

Edit files in `.ai/` only.

| Agent | Instructions | Project skills | Project agents |
|---|---|---|---|
| Claude Code | `CLAUDE.md` imports `AGENTS.md` | `.claude/skills` (symlink to `.ai/skills`) | `.claude/agents/*.md`, generated |

Never edit a generated agent copy. Change `.ai/agents/<name>.md` or `.ai/agents.json`, then run `python3 .ai/scripts/sync-agents.py`.
