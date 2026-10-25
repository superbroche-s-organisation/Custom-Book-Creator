# Test.2 validation

Author: **Superbroche** | October 25, 2026

## Results

- **2026.2:** 26 distinct regression programs pass. The complete run passed 25 programs; a misplaced new read-only assertion in `TemplateRenderTest` was corrected and the final focused run passed templates, interaction and localization against a rebuilt package.
- **2024.4:** all 25 applicable programs pass. The Fabric-specific starting-book program belongs to the 2026.2 package.
- **2022.2:** plugin ABI/save bridge, element registration, serialization/reload and all 12 generated Java templates pass. The final legacy package includes the new dropdown, footer controls, border and sound-element code.
- **Native MCreator 2024.4:** the plugin loader exposes the four complete writing help entries and loads their actual bodies. Forge 1.20.1 and NeoForge 1.21.1 both pass native base/book generation, element/icon storage and workspace save/reload with read-only and writable books.

## Generated-code compilation

| Generator | Real API check |
| --- | --- |
| Forge 1.17.1 | Forge 37.1.1, Gradle `compileJava` with Java 16 target |
| Forge 1.20.1 | Forge 47.3.0, Gradle `compileJava` |
| NeoForge 1.21.1 | NeoForge 21.1.232, `javac` with patched game and matching loader libraries |
| NeoForge 1.21.8 | NeoForge 21.8.53, Gradle `compileJava` |
| NeoForge 26.1.2 | NeoForge 26.1.2.95, `javac` with patched game and matching loader libraries |
| NeoForge 26.2 | NeoForge 26.2.0.88, Gradle `compileJava` |
| Fabric 26.2 | Loader 0.19.3 / API 0.155.2+26.2, Gradle `compileJava` |

The 2024.4 package uses the same Forge 1.20.1 and NeoForge 1.21.1 game templates, with its own native plugin adaptation. Minecraft 1.20.1 and 1.17.1 use their three-argument scroll callback; newer profiles use the four-argument callback. Modern key events, GUI rendering APIs, 26.2 screen ownership and legacy text-box wrappers are compiled against their real APIs.

The representative book covers rich text, titles, images/GIFs, starting books, player input, networking and both audio modes. Read-only renders also verify that inventory-key closing is present without optional writing classes. The compatibility matrix remains limited to these validated profiles.

## New regression coverage

- `TranslationPreviewTest` creates the actual translation-editor component and checks white document foreground for dark explicit color tags, plain typing, formatting, read-only/disabled states and Undo/Redo. Original serialized color tags are preserved and view-only white does not become a new stored tag.
- `PageSoundTest` checks one-time migration from the previous page settings, valid/invalid sound names, element selection, independent inspector changes, copy/JSON reload and missing/no-sound handling. Executed generated playback methods cover two automatic sounds and two independent buttons, stop/replay behavior, page changes, close/reopen and preservation of unrelated audio.
- `BookEditorActionsTest` adds actual sound elements through the GUI, undoes/redoes creation, removes them with the actual Delete binding and restores them. Existing movement/history/translation-key checks remain.
- `ScreenRuntimeLogicTest` executes the actual dropdown renderer/scroll/navigation methods: closed state, ten-row viewport, wheel scroll, the final category in a long list, bounds, category/page links and Escape. Old category pager hit regions are absent.
- `PageInteractionTest` runs generated input methods for all primary profiles. It checks remapped inventory keys, typing protection, Escape, dirty draft confirmation, pending-write protection and category changes with keep/discard paths.
- `ScreenBorderTest` executes the actual border renderer and verifies every perimeter pixel across eight scales from 0.05 to 1.0, eight coordinate offsets and five widths. All four edges remain closed despite rounding. The active inline widget's render path redraws that same border afterward.
- `LocalizationIntegrityTest` checks all 30 catalogs, 17 help files per locale and the four actual GUI help paths against packaged-resource paths. The native help check above additionally uses MCreator's real loader.

Existing writing protocol/client checks (73), rich-text checks (104), title design (258), title GUI integration (31), drag-and-drop (251 dynamic assertions), resource/workspace reload, link and starting-book regressions also pass.

## Limits

No complete interactive Minecraft/multiplayer/dedicated-server smoke test was performed by the coding agent. Real API compilation and executed headless components/fixtures check the described behavior, not every visual arrangement or live sound-device interaction. The user separately confirmed that regenerating the workspace fixes E-key closing in previously created books.

Before a public release, test dropdown wheel use, active zone typing/paste/scrolling, footer controls and centered signing at several Minecraft GUI scales, plus sound resources, save/reconnect and signatures on a real server. The exact earlier 2024.4b save report remains unreproduced; native tests use stable 2024.4.

No GitHub release, tag, issue comment or MCreator website publication is created for this snapshot. Source/test-kit archives exclude Minecraft binaries and local build caches.

## Reproduce

```powershell
./test.ps1 -MCreatorRoot <MCreator20262> -GeneratedSourcesDirectory build/generated
./test.ps1 -MCreatorRoot <MCreator20244> -TargetMCreator 2024.4
./test-mcreator2022.ps1 -MCreatorRoot <MCreator20222> -GeneratedSourcesDirectory build/generated/forge-1.17.1
./test-native-mcreator.ps1 -MCreatorRoot <MCreator20244> -PluginZip dist/CustomBookCreator-MCreator2024.4-v2.2.0-test.2.zip
```

To focus on these changes, use `-TestFilter TranslationPreviewTest,PageSoundTest,ScreenRuntimeLogicTest,PageInteractionTest,ScreenBorderTest,LocalizationIntegrityTest,BookEditorActionsTest,TemplateRenderTest`. A generated-source directory requires `TemplateRenderTest` in the filter. Game compilation additionally requires the matching generator/dependencies in the matrix. The local process-only Gradle socket fallback described in previous validation reports is not included in generated mods.
