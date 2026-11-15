# Tools and skills registry

What to use when you work in a module or area. Open the links for details.

Always: the [constitution](../.specify/memory/constitution.md), [code-style](rules/code-style.md), [git](rules/git.md), [documentation](rules/documentation.md), and the [build-and-verify](skills/build-and-verify/SKILL.md) skill.

## Per module

| Module | Rules | Skills | Agents | Blueprints | Main libraries | Commands |
|---|---|---|---|---|---|---|
| `poc/protocol` | [architecture](rules/architecture.md), [testing](rules/testing.md) | [add-dependency](skills/add-dependency/SKILL.md) | [security-reviewer](agents/security-reviewer.md) | [unit-test](blueprints/unit-test.md) | kotlinx.serialization, kotlinx.rpc | `./poc/gradlew -p poc :protocol:test` |
| `poc/server` | [architecture](rules/architecture.md), [testing](rules/testing.md), [security](rules/security.md) | [add-dependency](skills/add-dependency/SKILL.md) | [security-reviewer](agents/security-reviewer.md) | [unit-test](blueprints/unit-test.md) | Compose runtime, Ktor, kotlinx.rpc, AndroidX Remote Compose creation | `./poc/gradlew -p poc :server:test` |
| `poc/app` | [ui-compose](rules/ui-compose.md), [testing](rules/testing.md), [security](rules/security.md) | [add-dependency](skills/add-dependency/SKILL.md) | [security-reviewer](agents/security-reviewer.md) | [unit-test](blueprints/unit-test.md) | Compose, Material 3, Ktor client, kotlinx.rpc, AndroidX Remote Compose player | `./poc/gradlew -p poc :app:testDebugUnitTest` |
| `poc/build-logic` | [code-style](rules/code-style.md) | [add-dependency](skills/add-dependency/SKILL.md) | - | - | Gradle convention plugins, detekt | `./poc/gradlew -p poc build detekt` |

## Per area

| Area | Where | Rules | Skills | Agents | Blueprints | Libraries | Commands |
|---|---|---|---|---|---|---|---|
| Build | `poc/build-logic`, `poc/gradle/libs.versions.toml` | [code-style](rules/code-style.md) | [add-dependency](skills/add-dependency/SKILL.md) | - | - | Gradle, detekt | `./poc/gradlew -p poc build detekt` |
| UI | `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/presentation` | [ui-compose](rules/ui-compose.md) | [build-and-verify](skills/build-and-verify/SKILL.md) | - | [unit-test](blueprints/unit-test.md) | Compose, Material 3 | `ANDROID_SERIAL=<serial> ./poc/gradlew -p poc :app:connectedDebugAndroidTest` |
| Networking | `poc/protocol`, `poc/app/src/main/kotlin/dev/tonholo/ssrcompose/app/data`, `poc/server/src/main/kotlin/dev/tonholo/ssrcompose/server` | [security](rules/security.md), [architecture](rules/architecture.md) | [build-and-verify](skills/build-and-verify/SKILL.md) | [security-reviewer](agents/security-reviewer.md) | - | Ktor, kotlinx.rpc | `./poc/gradlew -p poc :server:test` |
| Security | connection, handshake, documents, sessions | [security](rules/security.md) | - | [security-reviewer](agents/security-reviewer.md) | - | - | - |
| Specs | `.specify/`, `specs/` | [documentation](rules/documentation.md) | [speckit-specify](skills/speckit-specify/SKILL.md), [speckit-plan](skills/speckit-plan/SKILL.md), [speckit-tasks](skills/speckit-tasks/SKILL.md), [speckit-checklist](skills/speckit-checklist/SKILL.md) | - | - | Spec Kit | - |
| Docs | `README.md`, `docs/adr/`, `CHANGELOG.md` | [documentation](rules/documentation.md), [git](rules/git.md) | - | - | [adr](blueprints/adr.md) | - | `python3 .ai/scripts/check-links.py` |
