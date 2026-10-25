# Custom Book Creator V2.2.0

Author: **Superbroche** | October 25, 2026

V2.2 improves the book editor and in-game interaction, adds per-page audio and book-text translations, and extends the MCreator 2024.4 package to NeoForge 1.21.1.

## New features and improvements

- **Sound/music page elements:** add automatic page-entry audio or movable/resizable Play/Stop buttons. Each element has its own playback instance. Sounds use Minecraft's **Jukebox/Note Blocks** volume setting and stop when leaving their page or book.
- **General Undo/Redo:** restore changes to categories, pages, elements, positions, sizes, titles and audio. The text editor keeps its separate document history.
- **Keyboard Delete:** select a user element in the preview and press Delete/Del/Suppr to remove it. Text fields keep normal character deletion; built-in arrows and the page title are protected.
- **Inline player writing:** type directly in the book's writing zone. Mode/choice and Save/Cancel controls appear below the right-hand page, leaving the category pane available.
- **Scrollable categories:** one directly visible list replaces category pagination and the duplicate selection header. The selected entry is highlighted; long lists support mouse-wheel scrolling and a scrollbar.
- **Centered signing:** Sign book is centered at the bottom of the category pane when signing is allowed.
- **Inventory-key closing:** the player's configured inventory key closes the book outside writing. Typing, pending writes and unsaved drafts remain protected. Category changes confirm before discarding a dirty draft.
- **Book-content translations:** edit authored titles, category labels, page titles, body text and button/zone/sound labels through a visual dialog and MCreator's Localization resources. Minecraft selects the current language, with source fallback and stable page/element keys.
- **Readable translation editing:** text displays white in the translation dialog while retaining its real stored colors. The final checkup also fixed rich copy/paste leaking that white preview color into the book text.
- **Detailed native help:** persistent field keys, input modes, default text and preset choices now load their complete help content.
- **Closed text-zone borders:** all four edges remain visible at fractional GUI scales, including while editing.

## Compatibility

| Plugin package | Included workspace profiles |
| --- | --- |
| MCreator 2026.2 | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |
| MCreator 2024.4 | Forge 1.20.1; **NeoForge 1.21.1 added** |
| MCreator 2022.2 | Forge 1.17.1 |

A matching compatible workspace generator must be installed separately. Other experimental profiles remain excluded from standard release ZIPs. Native stable MCreator 2024.4 registration/generation/save/reload is checked; the earlier beta-specific 2024.4b save report remains unreproduced.

## Upgrade

Back up the workspace, close MCreator and replace the old plugin ZIP with the package matching your MCreator version. Keep only one installed copy, restart MCreator, use **Regenerate code**, then rebuild the mod. Existing books receive the new behavior after regeneration. Earlier test-build page-level sound settings migrate once to sound elements.

Release downloads contain only the three MCreator plugin ZIPs and the sources ZIP. The sources archive is **not an installable plugin**. Guides, validation, website description and icon remain in the repository.

## Validation and limits

The release is checked with automated regression programs, native MCreator loading/storage/generation tests and real generated-code compilation across the listed Minecraft profiles. See the [validation record](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.2.0/docs/2.2.0/VALIDATION.md) for exact coverage.

No full interactive Minecraft, real multiplayer or dedicated-server smoke test is claimed. Translations cover authored presentation text; player answers, default/preset values and signatures remain canonical. Built-in in-game controls are English. JSON/OBJ model behavior follows the installed generator; Fabric has no bundled OBJ loader.

This release follows **V2.1.0**. Full details are in the [changelog](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.2.0/docs/2.2.0/CHANGELOG.md) and [guide](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.2.0/docs/2.2.0/GUIDE.md).
