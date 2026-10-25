# Changelog — 2.2.0

Author: **Superbroche** | October 25, 2026

Changes since public V2.1.0, including the four 2.2 test snapshots:

- Independent sound/music elements with automatic or button playback using Jukebox/Note Blocks volume.
- General Undo/Redo and keyboard Delete for page elements.
- Inline player writing with controls below the page, a directly visible scrollable category list and centered signing.
- Inventory-key closing with active-writing and unsaved/pending protections.
- Authored book-content translations, readable white previews and working detailed writing help.
- NeoForge 1.21.1 added to the MCreator 2024.4 package; the other validated profiles remain available.
- Fixed redo normalization, missing writing help paths and scaled text-zone borders.
- Final checkup fixed rich clipboard export leaking the preview's white color into book text, including safe preview-color toggling.
- Release packaging validates all three variants, the updated matrix, Java targets, source inventory, upload-kit files and checksums.

See the [full changelog](docs/2.2.0/CHANGELOG.md), [patch notes](docs/2.2.0/PATCH_NOTES.md), [guide](docs/2.2.0/GUIDE.md) and [validation record](docs/2.2.0/VALIDATION.md).

# Changelog — 2.2.0-test.4 (unreleased)

Author: **Superbroche** | October 25, 2026

- Book sounds now use Minecraft's **Jukebox/Note Blocks** (`RECORDS`) sound category for both automatic page playback and Play/Stop buttons.
- Updated audio help in all 30 interface catalogs and the current guide.
- The existing playback regression checks the actual sound category passed to each created sound instance.

See the [patch notes and checks](docs/2.2.0-test.4/PATCH_NOTES.md). This snapshot is pushed to source without publishing a release.

# Changelog — 2.2.0-test.3 (unreleased)

Author: **Superbroche** | October 25, 2026

- Removed the duplicate current-category selection header and its chevron.
- Categories now appear directly in a single scrollable list, with room for eleven entries above the centered signing action.
- Kept mouse-wheel scrolling, selection highlighting and automatic reveal of linked categories/pages. Escape no longer has an extra category-panel dismissal step.

See the [patch notes and targeted validation](docs/2.2.0-test.3/PATCH_NOTES.md). This test snapshot is pushed to source only; no release is published.

# Changelog — 2.2.0-test.2 (unreleased)

Author: **Superbroche** | October 25, 2026

- Translation source/editing text is always white in the dialog, including explicitly dark source colors. Stored formatting colors remain unchanged.
- Added **+ Sound/music** to create independent sound elements. Each element supports automatic page-entry playback or a movable/resizable Play/Stop button. Previous page-level sound settings migrate once without duplication.
- Removed the global page-audio row and sidebar audio button. Multiple sounds on a page have independent playback instances and stop when leaving it.
- Fixed all four writing help bindings to use complete `custombook/writing/...` resource paths; native MCreator loading is checked.
- Replaced category-list pagination with a scrollable dropdown. Removed its numeric page counter and navigation arrows.
- Centered the signing action at the bottom of the category sidebar, and moved mode/choice/Save/Cancel controls below the right-hand page.
- Category changes during editing protect pending writes and confirm discarding unsaved text.
- Fixed missing right-hand text-zone borders at fractional GUI scales: all four edges are drawn at a minimum of one GUI pixel, including during editing.
- Documented regeneration after plugin replacement so old books receive the updated code and inventory-key closing behavior.

See the [patch notes](docs/2.2.0-test.2/PATCH_NOTES.md), [guide](docs/2.2.0-test.2/GUIDE.md), and [validation](docs/2.2.0-test.2/VALIDATION.md). No release is published.

# Changelog — 2.2.0-test.1 (unreleased)

Author: **Superbroche** | October 25, 2026

- Added a per-page workspace sound, triggered on opening the page or through a Play/Stop button. Playback stops when leaving the page or book.
- Added general book-editor Undo/Redo for page/category organization, elements, layout, titles and page sound settings. Kept the rich-text editor's own Undo/Redo.
- Added the keyboard **Delete / Suppr / Del** shortcut for the selected user element in the focused preview, without capturing text-field deletion.
- Moved player writing directly into the page's writing zone, with mode/choice and Save/Cancel controls in the sidebar.
- Added inventory-key closing, respecting the player's remapped key and protecting active text entry and unsaved/pending edits.
- Moved signing to the bottom of the category sidebar and reserved space through category pagination.
- Added book-content translations through native MCreator language resources and a visual translation editor, with source fallback and stable page/element keys.
- Added detailed help for persistent field keys, input modes, default text and predefined choices, with locale fallbacks.
- Added NeoForge 1.21.1 to the MCreator 2024.4 package and checked native registration, generation, save and reload for both its generators.
- Fixed redo being lost when opening a restored page filled legacy defaults, and adapted language lookup to the Minecraft 1.17.1 API.

See the [patch notes](docs/2.2.0-test.1/PATCH_NOTES.md), [guide](docs/2.2.0-test.1/GUIDE.md), and [validation record](docs/2.2.0-test.1/VALIDATION.md). This is a test snapshot pushed to the repository; no release is published.

# Changelog — 2.1.0

Author: **Superbroche** | September 30, 2026

- Player-writable zones with free text, preset choices or both.
- Per-copy answers, server-side permission procedures, permanent signing and a post-signature procedure hook.
- NeoForge 1.21.8/26.2 and Fabric 26.2 profiles, plus separate MCreator 2024.4 / Forge 1.20.1 and MCreator 2022.2 / Forge 1.17.1 packages.
- Offhand routing, Unicode message bounds, stale/replayed request rejection and Fabric starting-book retry fixes.
- Release archives limited to compiled profiles; unvalidated experimental ports remain in source only.

See the [complete V2.1 changelog](docs/2.1.0/CHANGELOG.md), [patch notes](docs/2.1.0/PATCH_NOTES.md), and [compatibility/validation record](docs/2.1.0/VALIDATION.md).

# Changelog — 2.0.0

Author: **Superbroche** | September 9, 2026

Changes since public version **1.1.2-hotfix**, including the unpublished 1.2.0 test builds.

## Added

- `VisualRichTextEditor`: a Swing styled-document editor for body text and page titles, using native character and paragraph attributes.
- Selection-based size, color, bold, italic, underline, strikethrough and obfuscation, plus URL/internal-page formatting. Character styling without a selection affects subsequent typing.
- Independent left, center and right paragraph alignment within a single text zone.
- Rich/plain/HTML clipboard support, toolbar undo/redo and keyboard shortcuts.
- Per-page `TitleDesign` data with backward-compatible defaults, independently stored rich title content, alignment, four-edge placement, inward offset and title-area height.
- Configurable title separator visibility, color, length and thickness; shared editor/runtime title geometry and rotated side-title rendering with transformed link hitboxes.
- Circular title drag handle compatible with grid snapping, including when the separator is hidden.
- `prepare-compatibility.ps1`, invoked during packaging, plus compatibility templates and a machine-readable catalog for 19 experimental 1.20.x extensions.
- Forge profiles: 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4 and 1.20.6.
- NeoForge profiles: 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5 and 1.20.6.
- Fabric profiles: 1.20 through 1.20.6, using official Mojang mappings and version-specific item, client-interaction and starting-book adapters.
- Fabric once-only starting-book ledger keyed by world, player and book; inventory overflow drops the book and failed handoffs remain retryable.
- Five additional automated regression programs for rich-text editing, the visual editor, title geometry, title GUI integration and Fabric starting books.

## Changed

- Replaced the numeric Book generation spinner with localized named choices mapping to the same saved integers 0–3. Removed its help indicator.
- Replaced exposed rich-text markup with directly formatted editable text. Valid legacy markup is parsed automatically; the compatible internal storage format is retained.
- Title editing now uses the same formatting toolbar as body text. Page-tree labels remain plain text.
- Deep copies and save snapshots preserve independent title content and layout. Page renaming updates the visible title without discarding styling.
- Extended template rendering and runtime-logic validation from two to 21 profiles.
- Added English/French labels for new title controls, with English fallback in the remaining interface catalogs.

## Fixed

- Canonical, source-offset-aware formatting prevents repeated nested size tags, including multiline content, CRLF input and supplementary Unicode characters.
- Spinner arrow changes use the model's current value instead of stale formatted text. Reentrant change/caret callbacks cannot overwrite the requested size mid-edit.
- Toolbar state reads actual adjacent character attributes instead of delayed Swing typing attributes.
- Moving the caret clears stale typing styles instead of reviving them when returning to a previous position.
- Formatting exceeding the 32,767 UTF-16-code-unit save-format limit is rejected without truncating text or closing tags.
- Programmatic editor loads are silent and reset undo history, preventing cross-element undo edits.
- Version-specific generated 1.20.x API usage for screen rendering, resources, item data and loader callbacks.

## Removed

- Authored-book localization and translation-key controls from the intermediate test build. No Minecraft language-key lookup remains in active templates. Existing `[tr=...]` strings remain literal text.
- The plugin's own interface translations have **not** been removed.

## Retained safeguards

The 1.1.2-hotfix workspace image serialization fixes, normalization, MCreator save bridge, media limits, drag-and-drop rollback, stable navigation identifiers, category-end navigation setting and server-only starting-book behavior remain included.

## Limits

- Experimental extensions require separate, matching MCreator-2026.2-compatible generators. They do not upgrade older generators or establish complete loader/version support by themselves.
- Fabric custom JSON and normal item models are supported; OBJ uses the normal-texture fallback.
- Obfuscation is shown as readable shaded monospaced text in the editor; Minecraft renders its animated effect.
- Body elements do not automatically move away from title bands. Adjust their positions if they overlap.
- Previously lost or truncated text cannot be recovered automatically.

See the [validation report](docs/2.0.0/VALIDATION.md) for the exact test scope.

## Previous releases

- [1.1.2-hotfix changelog](docs/1.1.2-hotfix/CHANGELOG.md)
- [Archived V2.0.0 changelog](docs/2.0.0/CHANGELOG.md)
