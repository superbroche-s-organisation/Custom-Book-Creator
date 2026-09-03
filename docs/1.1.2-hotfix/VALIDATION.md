# Validation — 1.1.2-hotfix

Validation date: 2026-09-03. Plugin author: **Superbroche**.

Target compatibility: MCreator 2026.2 with the NeoForge 1.21.1 and NeoForge 26.1.2 generators.

## Result

- Plugin rebuilt successfully against the local MCreator 2026.2 installation, targeting Java 21 bytecode.
- **14 of 14 automated test programs passed against the freshly packaged plugin.**
- Representative generated Java compiled successfully against both real supported Minecraft/NeoForge APIs.
- No plugin compile errors or automated test failures remain in this test run.

## Reproduce the automated suite

```powershell
.\test.ps1 -MCreatorRoot "C:\Path\To\MCreator" -GeneratedSourcesDirectory "build\generated-validation"
```

The script rebuilds the plugin, extracts that ZIP, compiles every Java test in `tests`, and executes each test in a separate JVM. A failed program fails the overall run. The optional directory retains generated source files for external Minecraft/NeoForge compilation.

## Automated coverage

| Test | Verified behavior |
| --- | --- |
| `BinaryContractTest` | MCreator's required `getElementFromGUI(): GeneratableElement` bridge exists in the packaged bytecode; the packaged author, stable plugin ID, and renamed entry point are correct and loadable. |
| `WorkspaceReloadWithImageTest` | An imported image survives two actual MCreator serialization/reload cycles; null category/page/element recovery does not abort deserialization; saved books retain the stable `custombook` type without persisting the Java namespace. |
| `TreeDragDropTest` | 251 dynamic assertions execute the real Swing transfer handler: page/category ordering, inter-category moves, stable IDs/content/images, last-page replacement, invalid/stale/foreign targets, button-target repair, and rollback after injected failures. |
| `EditorSafetyTest` | Listener guards, native save lifecycle, selector recovery, style choices, and media-safety wiring remain present. |
| `MediaImportSafetyTest` | 36 dynamic assertions cover valid PNG/GIF, false formats, truncated files, dimensions, frame count, and decoded-pixel budget. Also passed separately with a 128 MB Java heap, proving oversized fixtures are rejected before large allocation. |
| `ModelNormalizationTest` | Legacy migration, duplicate/missing IDs, invalid properties/resources, model fallbacks, texture normalization, long Unicode preservation, and safe Java text chunks. |
| `RichTextPreviewTest` | 11 dynamic assertions cover Unicode, matched/unmatched rich-text closing tags, and safe text limits in the editor preview. |
| `ScreenRuntimeLogicTest` | Compiles and executes real logic extracted from both screen templates: texture references, formatting/links, Unicode, navigation boundaries, empty categories, and access to 25 categories. |
| `StartingBookBehaviorTest` | Executes both generated login/clone handlers against an isolated observable API fixture: server/client separation, once-only grant, reconnect, full inventory/drop, failure/retry, and clone-marker preservation. |
| `StartingBookTemplateTest` | Version-correct persistent-data API and marker placement in both generators. |
| `TemplateRenderTest` | Renders all item/screen/client/starting-book templates and parses all model JSON variants, with every optional item branch enabled and a 32,767-character non-ASCII text fixture. |
| `UrlOpeningTemplateTest` | Version-correct `Util` import and external-link confirmation code. |
| `V11FeaturesTest` | Version metadata, drag/drop wiring, category-boundary option, starting-book option, and required localization keys. |
| `LocalizationIntegrityTest` | 30 catalogs, 152 matching keys per catalog, no blank values, matching placeholders, and 13 matching help pages per locale. |

## Compilation against real Minecraft/NeoForge APIs

The item, screen, client event subscriber, and starting-book event subscriber emitted by `TemplateRenderTest` were compiled together with two minimal mod-registration fixtures. This is actual Java compilation against game classes, not only textual template inspection.

| Generator | Actual API artifacts | Result |
| --- | --- | --- |
| NeoForge 1.21.1 | NeoForge 21.1.232 merged development artifact, Java target 21 | Exit 0; 6 source files, 15 class files. A deprecation note is non-fatal. |
| NeoForge 26.1.2 | Official Minecraft 26.1.2 client/server files processed using NeoForm's declared InstallerTools 4.0.16 step, official NeoForge 26.1.2.95 binary patches applied using BinaryPatcher 4.0.12, plus the official universal/library artifacts; Java target 25 | Exit 0; 6 source files, 15 class files. |

The usual Gradle artifact-preparation path hit an environment-specific Java HTTP-client loopback failure before `compileJava`. The 26.1.2 artifacts were therefore prepared directly using the official tools/configuration, and the final Java compilation succeeded. No workaround is shipped in the plugin.

## Limits of this validation

- Tests are automated/headless; no physical mouse drag inside a visible MCreator window was performed in the final run.
- A full `runClient` session, visual rendering/performance review, and actual multiplayer/dedicated-server join/respawn session were not completed here. Starting-book logic is dynamically tested, but its fixture is not a running Minecraft server.
- Catalog completeness and placeholders are checked; this is not a human linguistic review of all translations.
- Valid legacy or partially damaged data structures are repaired. Arbitrarily broken JSON syntax or already missing user files cannot be reconstructed by normalization.

## Installation precautions

Back up the workspace first. Close MCreator and replace the old plugin, keeping only one Custom Book Creator ZIP across both its installation and user plugin folders. An old installation-folder copy can mask a newer user-folder copy.

Only the plugin ZIP is installable. The sources ZIP is for development and must not be placed in a `plugins` directory: MCreator attempts to load every ZIP there.
