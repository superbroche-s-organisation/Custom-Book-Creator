# Validation — 1.2.0-test.2

Author: **Superbroche**. Local test artifact; no GitHub publication.

## Results

The final `test.ps1` run rebuilt the plugin against MCreator 2026.2, then compiled and ran tests against that exact packaged ZIP. **19/19 test programs passed.**

- VisualRichTextEditorTest: **104 assertions** using real Swing documents, native text styles, selection formatting, typing styles, clipboard formats, undo/redo, length limits, and withdrawn translation syntax.
- RichTextEditingTest: **144 assertions**, including repeated font-size changes through the actual GUI callbacks, multiline text, independent styles, and caret controls.
- TitleDesignTest: **258 assertions** covering legacy migration, null-safe defaults, rich-title serialization, independent copies, all four layouts, bounds and grid-aware handle geometry.
- TitleEditorIntegrationTest: **27 assertions**, exercising title/body switching, model synchronization, actual mouse-handler drags on all four edges, grid on/off behavior, bar visibility, copying and reloading. The GUI shell is initialized as a headless test fixture rather than a running MCreator window.
- TemplateRenderTest and ScreenRuntimeLogicTest: all **21 profiles** passed generation and extracted runtime-logic tests.
- LocalizationIntegrityTest: **30 catalogs**, **173 keys**, **13 help files** per locale. This checks plugin-interface resources, not book-content localization.
- BinaryContractTest, EditorSafetyTest, FabricStartingBookTest, MediaImportSafetyTest, ModelNormalizationTest, RichTextPreviewTest, StartingBookBehaviorTest, StartingBookTemplateTest, TreeDragDropTest, UrlOpeningTemplateTest, V11FeaturesTest and WorkspaceReloadWithImageTest passed.

The generated title-enabled screen code was additionally compiled against real Minecraft client classes and dependencies for **1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5 and 1.20.6**, with Java 17/21 targets as appropriate. This checks screen API compatibility, not complete mod builds for every loader.

Headless editor and page-preview images were generated and visually inspected. They show formatted text without markup and a rotated, independently styled side title with its drag handle.

Warnings about unchecked operations and test fixtures using sun.misc.Unsafe remain; no final test failures occurred.

## Artifact integrity

- File: `CustomBookCreator-MCreator2026.2-v1.2.0-test.2.zip`
- Size: **680600 bytes**
- SHA-256: `d02dcde749b7dfa705b6eec9322d2cb1abcf1add15e46291c5311301ed946709`
- **628 files**, **33 Java classes**, **21 generator definitions**.
- Every ZIP entry's CRC and recorded size checked; no duplicate or unsafe paths.
- Manifest version, plugin entry point and author checked; all classes target Java bytecode 21.
- All 19 experimental compatibility catalog entries match packaged definitions.
- No unresolved compatibility placeholders, Java source files, tests or Git metadata in the plugin archive.
- No translation-key control or Minecraft I18n lookup remains in the active plugin sources/templates. Old key tokens are preserved as literal text.

## Remaining manual scope

No full MCreator application session, runClient session or dedicated-server visual/multiplayer review was performed. Validate the build on a backed-up workspace using [TESTING.md](TESTING.md), including regeneration, save/reopen, title layout at different Minecraft GUI scales, media and links.

The 1.20.x extensions remain experimental and require matching MCreator-compatible generators. The editor approximates obfuscation with shaded monospaced text; Minecraft supplies the actual effect. New title controls use English fallback outside English and French. Body elements are not automatically moved to avoid title bands.

Git HEAD remains unchanged. No commit, push, release or tag update was performed.
