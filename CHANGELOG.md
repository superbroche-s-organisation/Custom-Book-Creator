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
