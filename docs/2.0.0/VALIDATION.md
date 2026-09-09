# V2.0.0 validation report

Author: **Superbroche** | September 9, 2026

## Release build

The release was rebuilt against **MCreator 2026.2.33518**, using its bundled JDK and `javac --release 21`. `test.ps1` extracted that freshly packaged plugin and ran all tests against its actual bytecode and resources, in separate headless JVMs.

**19/19 automated regression programs passed.**

| Test | Result / scope |
| --- | --- |
| BinaryContractTest | Required MCreator `getElementFromGUI()` JVM bridge |
| EditorSafetyTest | Editor safety invariants |
| FabricStartingBookTest | Seven experimental Fabric versions; executable API-shaped fixtures |
| LocalizationIntegrityTest | 30 catalogs, 173 keys, 13 help files per locale |
| MediaImportSafetyTest | 36 dynamic assertions |
| ModelNormalizationTest | Safe model defaults and normalization |
| RichTextEditingTest | 144 assertions, including actual GUI font-size callbacks |
| RichTextPreviewTest | 11 dynamic assertions |
| ScreenRuntimeLogicTest | All 21 profiles; extracted generated runtime logic |
| StartingBookBehaviorTest | Existing two NeoForge profiles; isolated API fixtures |
| StartingBookTemplateTest | Existing two NeoForge profiles |
| TemplateRenderTest | All 21 profiles; representative property and title configurations |
| TitleDesignTest | 258 assertions for defaults, migration, layout, serialization, copying and grid geometry |
| TitleEditorIntegrationTest | 27 assertions using actual Swing controls and mouse handlers in a partial headless GUI fixture |
| TreeDragDropTest | 251 dynamic assertions |
| UrlOpeningTemplateTest | Existing two NeoForge profiles |
| V11FeaturesTest | Navigation, locale and release-version checks |
| VisualRichTextEditorTest | 104 assertions for styled documents, selection/typing styles, clipboard, undo, limits and literal translation syntax |
| WorkspaceReloadWithImageTest | MCreator serialization and repeated image-data reload |

Compiler warnings about unchecked operations and test fixtures using `sun.misc.Unsafe` remain. They did not cause test failures.

## Real Minecraft API compilation

Freshly generated V2 title-enabled screen sources compiled against official, Mojang-mapped Minecraft client classes and dependencies for **1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5 and 1.20.6**. Java 17 targets were used through 1.20.4; Java 21 was used for 1.20.5 and 1.20.6.

This validates the shared version-specific screen APIs. It is **not** compilation of every generated mod against each complete Forge, NeoForge or Fabric loader environment. The extensions still require matching installed MCreator-compatible generators.

## Artifact integrity

- Installable file: `CustomBookCreator-MCreator2026.2-v2.0.0.zip`
- Size: **680616 bytes**
- SHA-256: `89bbee694f9b7e613b9c0937dfc2f91976a4ccce911199d7f59a74a7bd6e358f`
- **628 files**, **33 Java classes**, **21 generator definitions**.
- All entry CRCs and recorded sizes checked; no duplicate or unsafe paths.
- Manifest version 2.0.0, author Superbroche, entry point and Java 21 bytecode checked.
- All 19 experimental compatibility catalog entries match packaged definitions.
- No unresolved compatibility placeholders, Java sources, tests or Git metadata in the installable plugin.
- Active plugin sources/templates contain no authored-book translation control or Minecraft language-key lookup.

Release assets also include SHA-256 checksums for the plugin, source archive, website icon, publication text and MCreator upload kit. The source archive is generated from the committed release tree, excluding Git metadata and ignored build outputs.

## Visual and manual scope

Headless editor/preview images from the preceding local test build were visually inspected: text is formatted without visible markup, and side-title rotation, formatting and the circular movement handle are visible. The user subsequently confirmed the updated editor after removing the older duplicate plugin installation. The V2 promotion changes version metadata and publication documentation, not those tested UI behaviors.

A complete fresh V2 `runClient` session, dedicated-server session and multiplayer visual review were **not** performed. Check save/reopen, generated code, screen wrapping at different GUI scales, media, links, navigation, starting-book joins and respawns in a backed-up workspace. No claim of complete in-game validation is made for all loader/version combinations.

New title controls use English fallback outside English and French. Obfuscation is approximated in the editor; the game supplies the animated effect. Body elements are not automatically moved away from title bands. See [GUIDE.md](GUIDE.md) for upgrade instructions and compatibility limits.
