# ssr-compose agent guidelines

This is the entry point for every AI agent working in this repository. Read it first. Then read the rule files that match your task.

## Before you start (required)

1. Load the `android-cli` skill before the first device step. Use `android input` for taps, typing, swipes and keys, also where that skill shows `adb shell input`.
2. Read the [constitution](../.specify/memory/constitution.md). It comes before every other file here.
3. Read this file, then the rules for your task.

## Project

ssr-compose is server-side rendering for Jetpack Compose. A producer server runs composable logic and a consumer app shows the result. It is used by app and server developers who want screens to ship from the server.

Main stack:

- Kotlin with the K2 compiler. Style is in [rules/code-style.md](rules/code-style.md).
- Compose for rendering. The consumer library is Kotlin Multiplatform with Android as its only target. See [rules/ui-compose.md](rules/ui-compose.md).
- Four deliverables: a consumer library, a producer library, a core library and a Kotlin compiler plugin. See [rules/architecture.md](rules/architecture.md).
- Gradle with a version catalog and convention plugins. The libraries must also be consumable from the Kotlin toolchain.
- Features go through Spec Kit: specify, plan, tasks, then checklists.
- Layout decision for this repo: [ADR 0001](../docs/adr/0001-keep-the-poc-in-a-folder.md).

State of the product: design stage. The library modules do not exist yet. The only code is the proof of concept in `poc/`, which is a reference and is not held to the library's rules.

## Module map

The root has no Gradle build yet. The proof of concept is its own Gradle project in `poc/`. List its modules with `./poc/gradlew -p poc projects`.

| Folder | CLI name | Type | Purpose | May depend on |
|--------|----------|------|---------|---------------|
| `poc/protocol` | `:protocol` | Kotlin JVM library | Node tree, service interface and component contracts shared by both sides | nothing in this repo |
| `poc/server` | `:server` | Kotlin JVM application | Ktor server that runs composables and serves sessions | `:protocol` |
| `poc/app` | `:app` | Android application | App that renders trees and plays fragments | `:protocol` |
| `poc/build-logic` | included build | Gradle convention plugins | Shared build setup for the PoC | nothing in this repo |

Dependencies point to the shared contract only: the two sides never depend on each other. Details and known violations are in [rules/architecture.md](rules/architecture.md).

PoC versions live in `poc/gradle/libs.versions.toml`, and its shared build config in `poc/build-logic/src/main/kotlin/`.

## Commands

Run all commands from the repo root.

| Task | Command |
|------|---------|
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

## Verification before you call work done

1. `./poc/gradlew -p poc build detekt` passes when you touched `poc/`.
2. Tests pass. New behavior has a test that failed before the change.
3. `python3 .ai/scripts/sync-agents.py --check` passes.
4. You checked your diff against every file in `rules/`. Use the reviewer agent for a second look.
5. Changes to the connection, the handshake, documents or sessions also pass the [security-reviewer](agents/security-reviewer.md) agent.
6. Rendering changes were looked at on a running app, on a device or emulator.
7. Commits are small and follow [rules/git.md](rules/git.md).

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

- [agents/security-reviewer.md](agents/security-reviewer.md)

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
