# Changelog — 2.2.0

Author: **Superbroche** | October 25, 2026

Changes since public V2.1.0, including the four unpublished 2.2 test snapshots.

## Added

- Independent `SOUND` page elements, with automatic playback or in-page Play/Stop buttons, labels, placement and size.
- Per-element sound lifecycle management using Minecraft's `RECORDS` sound source (Jukebox/Note Blocks volume).
- Bounded general editor snapshots for category/page organization, user elements, geometry, rich text/title design and sound settings.
- Preview-focused keyboard Delete/Del/Suppr action, without stealing text-field deletion.
- Visual book translation editing backed by native MCreator language resources and runtime language lookup. Stable category/page/element identifiers form the translation keys.
- Detailed native help for writing keys, input modes, defaults and choices, with complete paths in all 30 help locale trees.
- Native integration checks for plugin registration, generator coverage, writing help and generation/save/reload on MCreator 2024.4.
- Regression coverage for audio, general editor actions, generated input/navigation, white translation previews/clipboard and scaled borders.

## Changed

- Replaced page-level audio controls with sound elements. The previous test-build settings migrate once, preserving resource and trigger.
- Moved player writing into the actual page zone. Moved mode, next choice, Save and Cancel controls below the right-hand page.
- Centered signing at the bottom of the category pane.
- Replaced category pagination with an always-visible scrollable list; removed the duplicate selected-category header/chevron. Eleven entries fit above signing.
- Added configured inventory-key closing for both existing read-only and writable books after regeneration. Active writing protects the key for text entry.
- Extended the MCreator 2024.4 package from Forge 1.20.1 to Forge 1.20.1 plus NeoForge 1.21.1.
- Updated packaging to validate the release matrix, Java targets, committed source inventory and four-file release asset inventory. Supporting documents and website resources remain in the repository.

## Fixed

- Restoring normalized pages could discard the redo branch; restored normalization now replaces the current snapshot without creating a new edit.
- The four writing help icons used incomplete resource paths and showed missing-entry messages.
- Translation previews could render dark source text on a dark background.
- **Release checkup:** rich copy/paste from the white preview could store white as the real book color. View-only foreground inference is now separated from stored styles; plain and explicitly colored clipboard roundtrips retain their real colors.
- Disabling/re-enabling the preview color override now preserves the authored document color.
- Fractional GUI scaling could round a right-hand border edge to zero pixels. Borders now retain at least one GUI pixel on all sides and are redrawn after active input widgets.
- Category changes during editing preserve pending writes and confirm discarding dirty drafts.
- Adapted translation lookup and scroll callback signatures for the older Minecraft APIs.

## Scope

- The validated generator matrix is listed in PATCH_NOTES.md and VALIDATION.md; this release does not claim every loader/version combination from 1.17.1 to 26.2.
- Manual book translations do not translate player answers, default/preset values or signatures. Built-in in-game controls remain English.
- Regeneration is required after replacing the plugin to update previously generated book screens.
- The exact 2024.4b save issue has not been reproduced from the available report; native stable 2024.4 integration is checked.
