# Validation — 1.2.0-test.1

Author: **Superbroche**. Local test build; not published to GitHub.

## Final automated run

On 2026-09-09, `test.ps1` rebuilt the plugin against the installed MCreator 2026.2 classes, packaged it, then compiled and ran the tests against that exact ZIP. The plugin targets Java bytecode 21.

**16/16 test programs passed:**

- BinaryContractTest: MCreator's required getElementFromGUI bridge method.
- EditorSafetyTest: editor safety regressions.
- FabricStartingBookTest: seven generated 1.20.x variants compiled and executed against API-shaped, observable test doubles; first join, repeat join, respawn UUID, saved-ledger reload, another world, inventory overflow and failed-handoff retries.
- LocalizationIntegrityTest: 30 locale catalogs, 156 keys and 13 help files per locale.
- MediaImportSafetyTest: 36 dynamic assertions.
- ModelNormalizationTest: book model normalization.
- RichTextEditingTest: 135 assertions, including the reported multiline font-size case, repeated updates, independent selection styles, paragraph alignment, translation tokens, Unicode, CRLF document offsets and actual Swing spinner/caret callbacks.
- RichTextPreviewTest: 11 dynamic assertions.
- ScreenRuntimeLogicTest: generated screen logic exercised for all 21 profiles, including translated rich text, language changes and mixed paragraph alignment. These are extracted-code fixtures, not running Minecraft clients.
- StartingBookBehaviorTest: the two existing NeoForge starting-book variants.
- StartingBookTemplateTest: the two existing NeoForge starting-book templates.
- TemplateRenderTest: all 21 profiles rendered successfully from the packaged templates.
- TreeDragDropTest: 251 dynamic assertions.
- UrlOpeningTemplateTest: existing NeoForge URL-opening regressions.
- V11FeaturesTest: existing feature and current manifest checks.
- WorkspaceReloadWithImageTest: workspace serialization/reload regression with media.

Compiler warnings about unchecked operations and test fixtures using sun.misc.Unsafe remain; no test failure occurred.

## Real Minecraft API compilation

The generated shared screen variant was separately compiled successfully against the real Mojang-mapped client classes and libraries for **1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5 and 1.20.6**. Java release 17 was used through 1.20.4 and release 21 for 1.20.5–1.20.6.

Client archives, mappings and dependencies came from official Minecraft version metadata with SHA-1 verification. The seven representative sources were taken from the Fabric profile output because the screen code is shared across the three loader families for a given Minecraft version.

This checks screen API compatibility, **not** compilation of complete generated mods against every Forge, NeoForge and Fabric API. The Fabric starting-book tests use test doubles, not real dedicated servers.

## Packaged artifact

- File: `CustomBookCreator-MCreator2026.2-v1.2.0-test.1.zip`
- Size: **644825 bytes**
- SHA-256: `e9b2b2424b4c44321be7378b70d3996a0a4bc19f1a2127e51fa332dbb457a53a`
- 619 files, including 24 Java classes and 21 generator definitions.
- Root plugin manifest, entry point, version and author checked.
- All entries read and checked against their recorded CRC and size; duplicate/unsafe paths rejected.
- All packaged classes checked for Java 21 bytecode.
- All 19 experimental catalog entries checked against their generator definitions.
- No unresolved compatibility-template tokens, Java source files, tests or Git metadata in the plugin ZIP.

## Limits and manual tests

The 19 new profiles are **experimental extensions** requiring matching, MCreator-compatible workspace generators. They are not a replacement for those generators, and do not establish working MCreator 2026.2 workspaces for every 1.20.x loader/version combination.

No complete runClient, dedicated-server session or mouse-driven visual editor review was performed for this build. Follow the [testing guide](TESTING.md) on a backed-up workspace, including save/reopen, regenerated code, images/GIFs, links, mixed alignment, language switching and starting-book behavior.

Fabric OBJ models fall back to the normal item texture. Localization requires author-supplied translations. Already-truncated text from older builds cannot be recovered automatically. The new editor translation controls use English fallback outside English and French.

No commit, push, release or tag update was performed for this test build.
