# 0010. Place per-module choice enums in a bridge subpackage

- Status: Accepted
- Date: 2026-11-19
- Modules: catalog-model, compiler-plugin, gradle-plugin, toolchain-plugin

## Context

Each server module creates its own internal bridge functions for the catalog components it names, with no separate bridge module ([research.md R9](../../specs/001-consumer-catalog-bridge/research.md#r9-one-bridge-per-held-catalog)). Bridge functions stay apart across modules because each function lives in a facade class named after the compilation's module name, which callers never write in imports.

Choice parameters need real `enum class` declarations ([research.md R3](../../specs/001-consumer-catalog-bridge/research.md#r3-a-real-enum-for-a-choice-list-needs-a-workaround)). An enum's JVM binary name is its package and class name, so putting enums directly in `bridgePackage` causes two modules holding the same catalog to write the same `.class` file path. The probe in [research.md R16](../../specs/001-consumer-catalog-bridge/research.md#enum-classes-of-one-catalog-in-two-modules) proved that identical enums still differ in bytes by module name in Kotlin metadata, failing merged jars with `DuplicatesStrategy.FAIL`, and different entries pass compilation and crash at run time with `NoSuchFieldError`.

The owner decided on 2026-11-18 that each server module gets its own enum classes, in a package or an outer class named after the module. Three constraints shape the choice:

- Generated classes must stay within Java SE 17 class file limits: 65,534 usable constant pool slots, 32,767 byte safe method ceiling, and 255 byte file and folder names.
- A server module's test compilation compiles against `main` as a friend module and must share `main`'s enum types so test code can pass values between screens in `main` and direct bridge calls in `test`.
- Developers type imports by hand because IntelliJ does not offer auto-import for compiler-plugin declarations (KTIJ-27574, KTIJ-29912), so the enum location must be human-readable and predictable.

## Options

1. Top-level `internal enum class` in a module-named subpackage `<bridgePackage>.<moduleSegment>.<ChoiceName>`, where `<moduleSegment>` is derived from the module path (`:feature:checkout` gives `feature_checkout`, `:` gives `root`) and overridable with `producer { moduleName = ... }`.
   - Pros: Each enum is an independent top-level `.class` file (`<ChoiceName>.class`, at most 198 bytes) inside a folder (`<moduleSegment>`, at most 64 bytes), so neither competes for the 255 byte file name limit and there is no outer class constant pool limit. Test compilations reuse enums compiled by `main` through `session.dependenciesSymbolProvider` (checked against `friendDependencies`) and generate only the extra enums `test` needs in the same subpackage. Uses `createTopLevelClass(..., ClassKind.ENUM_CLASS)`, already proven in R3. Never collides with PascalCase bridge function names in `bridgePackage`.
   - Cons: The import includes `<moduleSegment>`, and `import <bridgePackage>.*` does not pull in choice enums (a second import `<bridgePackage>.<moduleSegment>.<ChoiceName>` is needed). Two modules whose paths clean to the same segment (`:my-app` and `:my_app`) must override `moduleName` on one of them.
2. Nested `internal enum class` inside a module-named outer class or object `<bridgePackage>.<ModuleOuter>.<ChoiceName>`.
   - Pros: `import <bridgePackage>.*` makes `<ModuleOuter>.<ChoiceName>` reachable without a second import.
   - Cons: A test compilation cannot add a nested enum to `main`'s compiled `<ModuleOuter>.class` without shadowing it and breaking `NestHost`/`NestMembers` or losing `main`'s nested enums. `<ModuleOuter>$<ChoiceName>.class` shares a single 255 byte file name budget, leaving at most 56 bytes for `<ModuleOuter>` when `<ChoiceName>` has 64 three-byte characters. One outer class holding many choice lists risks `InnerClasses` and constant pool exhaustion. `<ModuleOuter>` can collide with a PascalCase component name in `bridgePackage`. Requires untested nested enum FIR generation on top of R3.
3. Module-named subpackage with a SHA-256 hash suffix plus a top-level `internal typealias <ChoiceName>` in `<bridgePackage>` so imports stay `<bridgePackage>.<ChoiceName>`.
   - Pros: Developers would write `import <bridgePackage>.<ChoiceName>` with no module segment.
   - Cons: Ruled out by Kotlin 2.4.21 compiler source. `FrontendFilesForPluginsGenerationPipelinePhase.kt` places every generated top-level `FirClassLikeDeclaration` (including a `FirTypeAlias`) in a synthetic file `<package>/<ClassName>.kt` and ignores `fileNameForPluginGeneratedCallable`. A top-level typealias therefore emits `<bridgePackage>/<ChoiceName>Kt.class`, reintroducing the exact cross-module `.class` collision this decision avoids.
4. Derive `<moduleSegment>` directly from Kotlin's `CommonConfigurationKeys.MODULE_NAME` instead of a dedicated `bridgeModule` option from the module path.
   - Pros: Needs no new compiler plugin option or Gradle setting.
   - Cons: Ruled out by Kotlin 2.4.21 Gradle plugin source (`klibUtils.kt` and `kotlinCompilations.kt`). Gradle sets `-module-name` to `"$group:$projectName"` for `main` and `"$group:$projectName_test"` for `test`, which is unreadable in imports, changes when `group` or checkout folder name changes, and gives `main` and `test` incompatible enum types.

## Decision

Option 1. Every choice list used by a server module becomes a top-level `internal enum class <ChoiceName>` in `<bridgePackage>.<moduleSegment>`, declaring all entries of that choice list in catalog declaration order. The build tool derives `<moduleSegment>` from the module path with the same normalization rule as `PartName` (using `_` instead of `-`), passes it as the `bridgeModule` compiler option to both `main` and `test` compilations, and lets the developer override it with `producer { moduleName = ... }`. Test compilations reuse enums already compiled in friend modules via `session.dependenciesSymbolProvider`, and any non-friend classpath collision fails compilation with a clear message.

## Consequences

- Good: Two modules holding the same catalog, even with different entries for a choice list, produce disjoint `.class` paths and cannot fail with `NoSuchFieldError` at run time.
- Good: `main` and `test` write the same import and share one `Class<*>` for every enum `main` compiled, while `test` can still generate enums that `main` did not use.
- Good: Enum class files stay within every JVM 17 limit, with choice lists capped at 2,000 entries to keep `<clinit>` under 32,767 bytes.
- Bad: Server code imports choice enums from `<bridgePackage>.<moduleSegment>.<ChoiceName>` instead of `<bridgePackage>.<ChoiceName>`.
- Bad: Public functions in a server module cannot expose generated enum types in their signatures because the enums are `internal`; helpers that accept or return a choice enum must be `internal`.
- Follow-up: T048 implements the subpackage enum generation, `bridgeModule` option, friend reuse and JVM limit tests, and records real plugin measurements in [research.md R16](../../specs/001-consumer-catalog-bridge/research.md#r16-scale-and-build-performance). T101 passes `bridgeModule` from the Kotlin toolchain plugin using the same relative-path rule.
