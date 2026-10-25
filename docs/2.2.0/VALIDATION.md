# V2.2.0 validation record

Author: **Superbroche** | October 25, 2026

## Release packages and real game compilation

| Package | Generator | Successful check for the final release code |
| --- | --- | --- |
| MCreator 2026.2 | Forge 1.20.1 | Forge 47.3.0, Gradle `compileJava` |
| MCreator 2026.2 | NeoForge 1.21.1 | NeoForge 21.1.232, `javac` against patched game and exact loader libraries |
| MCreator 2026.2 | NeoForge 1.21.8 | NeoForge 21.8.53, Gradle `compileJava` |
| MCreator 2026.2 | NeoForge 26.1.2 | NeoForge 26.1.2.95, `javac` against patched game and exact loader libraries |
| MCreator 2026.2 | NeoForge 26.2 | NeoForge 26.2.0.88, Gradle `compileJava` |
| MCreator 2026.2 | Fabric 26.2 | Loader 0.19.3 / API 0.155.2+26.2, Gradle `compileJava` |
| MCreator 2024.4 | Forge 1.20.1, NeoForge 1.21.1 | Native plugin build/generation; shared game templates compiled above |
| MCreator 2022.2 | Forge 1.17.1 | Native Java 17 plugin build; Forge 37.1.1 Gradle `compileJava`, Java 16 target |

Generated code includes large Unicode rich text, title designs, images/GIFs, navigation, player writing, networking, automatic/button audio and starting books. Both sound modes use `SoundSource.RECORDS`. The templates adapt scroll/key/rendering APIs, 26.2 screen ownership, old data storage and the legacy multiline widget. Read-only renders also exclude optional writing dependencies and retain inventory-key closing.

Java targets: plugin bytecode 21 for 2026.2/2024.4 and 17 for 2022.2; game code uses Java 16 for 1.17.1, 17 for 1.20.1, 21 for 1.21.x and 25 for 26.x. Matching compatible workspace generators remain required and are not bundled. The other experimental 1.20.x profiles are excluded from standard release ZIPs.

## Complete regression results

- **All 26 programs pass against the final packaged 2026.2 plugin.**
- **All 25 applicable programs pass against the final packaged 2024.4 plugin.** Fabric-specific starting-book checks apply to the 2026.2 package.
- **Legacy 2022.2 checks pass:** native GUI/save ABI, element registration, JSON serialization/reload and twelve generated Java templates, followed by real Forge compilation.
- **Writing protocol/client:** 73 checks cover per-copy persistence, hand routing, signatures, permissions, stale/replayed sessions, invalid/preset input, Unicode bounds, limits, failed sends, timeouts and world switches.
- Existing rich-text checks (104), title design (258), title GUI integration (31), drag-and-drop (251 dynamic assertions), resource/image workspace reload, external links and starting-book tests pass.
- General editor history covers actual add/delete/movement, sound creation and deletion through the real Delete binding, Undo/Redo, branch invalidation and stable translation keys.
- Executed generated screen methods cover list bounds/scrolling/final-category access, direct list hit regions, absence of the old selection/pager regions, category/page links, inventory remapping, typing protection, pending writes and dirty-draft keep/discard paths.
- Audio checks cover independent automatic and button sounds, page transitions, stop/replay/close/reopen, preservation of unrelated sounds, migration/copy/reload and the actual `RECORDS` source passed to sound instances.
- Border rendering checks every perimeter pixel across eight scales (0.05–1.0), eight offsets and five widths. The active input render path also redraws that border.
- Locale integrity checks all 30 catalogs, seventeen help files per locale and the four actual writing help bindings.

## Native MCreator integration

The actual plugin loader, generator coverage, help loader, base/book generation, element/icon storage and workspace save/reload pass in isolated workspaces for:

- **MCreator 2024.4:** Forge 1.20.1 and NeoForge 1.21.1, each with read-only and writable books.
- **MCreator 2026.2:** NeoForge 1.21.1 and 26.1.2, each with read-only and writable books.

All four writing help entries load their real bodies. The integration fixture explicitly reads the extracted SDK manifest, avoiding unrelated library manifests in the classpath. No user workspace or installed plugin is changed by these tests.

## Issues found and fixed in the release checkup

1. A new clipboard regression reproduced that copying from the white translation preview could store white as the actual book-text color. The preview now keeps the real default color and does not infer persisted colors from its display override. Plain and explicitly colored rich clipboard roundtrips pass; the dialog still displays white text.
2. Disabling/re-enabling that display override now restores/preserves the authored color rather than saving the painted foreground.
3. The release packager was still scoped to V2.1 and a Forge-only 2024.4 archive. It now validates the V2.2 manifest, two-profile 2024.4 package, Java targets, committed source inventory, upload-kit contents and SHA-256 files.

## Archive checks

`package-release.ps1` requires committed clean sources and checks all three plugin manifests/authors, supported profile inventories and catalogs, validation metadata, classfile Java versions and unexpected SDK/cache payloads. The sources ZIP is generated from the release commit and checked against every tracked file. Every upload-kit entry is hashed against its prepared input; SHA256SUMS.txt is included for the artifacts. The enlarged existing 512×512 plugin icon is reused unchanged.

## Remaining limits

**No complete interactive Minecraft, real multiplayer or dedicated-server launch smoke test was performed for this release.** Real API compilation and executable headless components/fixtures establish the checks described above, not exhaustive gameplay, device or visual validation. The user separately confirmed that regenerating code resolves inventory-key closing for old books.

Recommended manual checks include typing/paste/scrolling at different GUI scales, footer placement and signing, sound playback and the Jukebox/Note Blocks slider, translated line wrapping, save/reconnect and server procedures. Imported resources/models and third-party plugin interactions are not exhaustively covered.

The earlier **2024.4b** save failure lacks sufficient reproduction data and remains unreproduced. Native integration uses stable 2024.4. Unlisted Minecraft/loader combinations remain unsupported by these standard ZIPs. Translation scope excludes player answers, default/preset values and signatures; built-in in-game controls remain English.

## Reproduction

```powershell
./test.ps1 -MCreatorRoot <MCreator20262> -GeneratedSourcesDirectory build/release-generated
./test.ps1 -MCreatorRoot <MCreator20244> -TargetMCreator 2024.4
./test-mcreator2022.ps1 -MCreatorRoot <MCreator20222> -GeneratedSourcesDirectory build/release-generated/forge-1.17.1
./test-native-mcreator.ps1 -MCreatorRoot <MCreator20244> -PluginZip dist/CustomBookCreator-MCreator2024.4-v2.2.0.zip
./test-native-mcreator.ps1 -MCreatorRoot <MCreator20262> -PluginZip dist/CustomBookCreator-MCreator2026.2-v2.2.0.zip -Profiles neoforge-1.21.1,neoforge-26.1.2
./package-release.ps1 -OutputDirectory <new-output-folder>
```

For generated-game compilation, use the versions in the matrix. Build tools are ForgeGradle 5.1.77 / Gradle 7.6.4 for 1.17.1, ForgeGradle 6.0.54 / Gradle 8.8 for 1.20.1, ModDevGradle 2.0.141 for 1.21.8, ModDevGradle 2.0.148 for 26.2 and Loom 1.17.21 for Fabric 26.2. Modern checks use Gradle 9.6.1. The local process-only Gradle socket workaround is not included in generated mods or required from users.

Compiler deprecation/Unsafe warnings remain; there are no compilation errors in the checks above. Source/release packages exclude Minecraft binaries, local SDKs and Gradle caches.
