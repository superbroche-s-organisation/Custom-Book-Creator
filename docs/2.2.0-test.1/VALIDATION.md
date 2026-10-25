# 2.2.0-test.1 validation record

Author: **Superbroche** | October 25, 2026

## Packages and generated code

| MCreator package | Generator | Successful generated-code check |
| --- | --- | --- |
| 2026.2 | Forge 1.20.1 | Forge 47.3.0, Gradle `compileJava` |
| 2026.2 | NeoForge 1.21.1 | NeoForge 21.1.232, `javac` against the patched game and exact loader dependencies |
| 2026.2 | NeoForge 1.21.8 | NeoForge 21.8.53, Gradle `compileJava` |
| 2026.2 | NeoForge 26.1.2 | NeoForge 26.1.2.95, `javac` against the patched game and exact loader dependencies |
| 2026.2 | NeoForge 26.2 | NeoForge 26.2.0.88, Gradle `compileJava` |
| 2026.2 | Fabric 26.2 | Loader 0.19.3 / API 0.155.2+26.2, Gradle `compileJava` |
| 2024.4 | Forge 1.20.1, NeoForge 1.21.1 | Native plugin compilation and generation; the same game templates are compiled above |
| 2022.2 | Forge 1.17.1 | Native Java 17 plugin build; Forge 37.1.1, Gradle `compileJava` targeting Java 16 |

Representative game code includes both audio trigger modes, writing zones, styled/translated text and titles, optional item features, media, starting books and networking. Read-only template checks also ensure no optional writing dependency leaks into ordinary books. The older 1.17.1 language lookup is adapted to its one-argument API.

The Forge/NeoForge/Fabric matrix remains intentionally limited to these compiled profiles. A matching compatible generator must be installed separately. The experimental 1.20.x adapters stay excluded from normal packages. Deprecation/Unsafe warnings from existing API/test fixtures remain; no compiler errors remain in the checks above.

## Automated checks

- **2026.2: 24 distinct programs passed.** The full 23-program suite passed, then the additional `PageInteractionTest` and updated `BookEditorActionsTest` passed in a focused run against a rebuilt package.
- **2024.4: all 23 applicable programs passed**, including the new interaction test; Fabric-specific starting-book tests apply to the 2026.2 package only.
- **2022.2: native ABI/save bridge, element registration, JSON reload and 12 Java template renders passed**, followed by the real Forge compilation above.
- **Native MCreator 2024.4 integration:** the actual plugin loader and generator statistics expose Custom book for both Forge 1.20.1 and NeoForge 1.21.1. Native base generation, book generation, element/icon storage and workspace save/reload pass with both read-only and writable books. Tests run in isolated installation/workspace folders.
- **General editor history:** actual Swing Add/Delete/geometry edits and toolbar Undo/Redo restore the model and selection. A new edit drops the stale redo branch; normalization during restore preserves redo. Snapshot count is bounded. Existing drag-and-drop coverage has 251 dynamic assertions.
- **Delete shortcut:** the focused preview's actual key binding removes a selected user element, does not register a window-global Delete handler, and protects the page title. Existing text-editing tests remain valid.
- **Page audio:** model validation, page independence, copy/reload, no-sound handling and selector refresh pass. The extracted runtime methods play once on page entry, implement button play/stop/replay, stop on page changes/close and preserve unrelated sound instances.
- **Keyboard and translations:** the generated methods run for all six primary profiles. Inventory remapping works; E/inventory keys do not close while editing; Escape protects pending writes, confirms dirty text and closes the book only after editing ends. Translation lookup keeps source markup, resolves translated markup and follows a changed language instance.
- **Translations:** page/element keys remain stable after renaming/reordering; only body text and page titles expose rich formatting. Plain labels are saved as plain text. Locale integrity covers all 30 catalogs and 17 help files per locale, including the four new writing guides.
- **Writing protocol:** the existing 73 generated server/client checks pass, including per-copy persistence, main/offhand separation, signature locking, permissions, replay/stale-session rejection, invalid input, Unicode bounds, timeouts and failed sends.
- Existing rich-text editing (104 assertions), title design (258 assertions), editor title integration (31 assertions), image/workspace reload, external links and starting-book tests pass.

## Limits and remaining manual checks

**No interactive Minecraft, real multiplayer or dedicated-server launch was performed for this snapshot.** Generated-code compilation and executable headless fixtures establish API/type compatibility and the tested logic, rather than proving visual layout or every live input/audio interaction. Before a public release, manually check typing/paste/scrolling at several GUI scales, server save/reconnect/signing, translated rich-text wrapping, category pagination and imported sound playback.

The exact save failure reported with **2024.4b** in issue #2 lacks a log and has not been reproduced. Native tests use stable MCreator 2024.4 and pass on both bundled generators. The added NeoForge profile addresses missing generator coverage; it is not evidence that a beta-specific save exception was reproduced and fixed. A matching ZIP and generator are still required.

Translations cover authored presentation text, not default/preset answer values, player-written text, signatures or the English built-in controls. General Undo does not revert language-resource edits or imported files. See the [guide](GUIDE.md) for these boundaries.

## Reproduction

```powershell
./test.ps1 -MCreatorRoot <MCreator20262> -GeneratedSourcesDirectory build/generated
./test.ps1 -MCreatorRoot <MCreator20244> -TargetMCreator 2024.4
./test-mcreator2022.ps1 -MCreatorRoot <MCreator20222> -GeneratedSourcesDirectory build/generated/forge-1.17.1
./test-native-mcreator.ps1 -MCreatorRoot <MCreator20244> -PluginZip dist/CustomBookCreator-MCreator2024.4-v2.2.0-test.1.zip
```

`test.ps1 -TestFilter PageInteractionTest,BookEditorActionsTest` runs just those programs after rebuilding. Game compilation uses the dependencies in the table and fixtures for the mod's main class/item registry. Build/validation caches and Minecraft binaries are not included in sources or plugin ZIPs.

The local Gradle environment used the same process-only Unix-domain-socket fallback as V2.1; this does not change the generated mod or user system settings. No GitHub release or issue comment is created by this development update.
