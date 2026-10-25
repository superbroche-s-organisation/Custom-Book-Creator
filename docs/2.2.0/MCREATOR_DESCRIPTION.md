# Custom Book Creator — V2.2

Build custom in-game books with a visual editor: illustrated manuals, guides, journals, rulebooks and player-written contracts. Arrange categories and pages, style text and titles, add media and audio, and control writing/signing through server procedures.

## Visual book design

- Write formatted body text and page titles in a WYSIWYG editor. Formatting appears directly instead of exposing markup tags.
- Apply size, color, bold, italic, underline, strikethrough and obfuscation. Use independent paragraph alignments, so a centered heading can share a zone with left-aligned body text.
- Place titles at the top, bottom, left or right. Adjust their inward offset, area size and separator color, length, thickness and visibility; move the title handle with optional grid snapping.
- Add independent text zones, imported images, animated GIFs, page/category buttons, writing zones and sound/music elements. Move and resize elements in the page preview or through position/size controls.
- Customize page navigation arrows and image/button appearance. Drag categories/pages to reorder them or move a page into another category while retaining stable internal targets.
- Use general Undo/Redo for book-layout changes, and Delete/Del/Suppr to remove the selected preview element. Rich text has its own document history.

## Page sound and music

Add a Sound/music element and select a workspace sound. Choose automatic playback on entering the page or an in-page Play/Stop button, then position/resize the button and set its label. Multiple sound elements operate independently.

Playback follows Minecraft's **Jukebox/Note Blocks** volume setting. Page/book changes stop their own audio instances, leaving unrelated game sounds alone. Automatic playback runs once per page entry; button playback can be stopped and replayed. Sounds do not loop.

## Player writing, choices and contracts

Create zones for free text, predefined choices or both. Players write directly inside the zone, with mode/choice and Save/Cancel controls below the page. Each physical book copy retains its own answers; writable copies cannot stack.

A server-side boolean procedure controls editing/signing permission. Optional permanent signing records the actual player identity and locks that copy. An optional post-signature procedure can implement your contract rules. Saving is acknowledged only after server confirmation; validation covers held item identity, permissions, session tokens, limits and preset values.

Dirty drafts receive discard confirmation, pending writes block navigation, and the inventory key remains available while typing. Outside editing, the configured inventory key closes the book. Signing is centered below the scrollable category list.

Writing limits remain 64 zones per book, 1,024 UTF-16 units per zone, 4,096 in total and 16 predefined choices per zone. Player answers are plain text. Detailed help explains stable field keys, modes, defaults and choices.

## Book-text translations

Use the Translations dialog to author translations for book titles, category names, styled page titles, body text and button/zone/sound labels. Entries use MCreator's normal Localization resources and are selected by the player's current Minecraft language, with source fallback. Stable page/element keys survive reordering and renaming.

The translation editor displays readable white text while keeping real stored colors, including during rich copy/paste. This is manual localization of authored presentation text; player answers, default/preset values and signature text are not translated. Built-in in-game controls are English.

## Book item properties

Configure the item name, rarity, creative tabs, stack size, enchantability, fire immunity, glow and supported item behavior. Choose an item texture and an imported JSON/OBJ resource model where supported by the generator. Fabric has no bundled OBJ loader and falls back to the item texture for that option.

Enable Starting book to grant a copy once per player per world, without granting again on reconnect or respawn. Choose named book-generation values and optionally stop next-page navigation at the end of each category.

## Compatibility and installation

| MCreator package | Workspace generators |
| --- | --- |
| 2026.2 | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |
| 2024.4 | Forge 1.20.1; NeoForge 1.21.1 |
| 2022.2 | Forge 1.17.1 |

Install the ZIP matching your MCreator version and a compatible workspace generator. These packages extend generators; they do not supply complete generator replacements or downgrade workspaces. Keep one installed plugin copy. English is the editor's source language; its 30 locale catalogs retain fallbacks, with French translations for new controls/help.

Back up the workspace, close MCreator, replace the old ZIP and restart. **Regenerate code and rebuild** so existing books receive the updated screen and sound category. Never install the sources archive or MCreator upload kit as a plugin. Server and clients need the same regenerated mod.

Automated regression tests, native MCreator integration and actual generated-code compilation cover the release matrix. No complete interactive gameplay, multiplayer or dedicated-server launch test is claimed. Exact evidence and remaining manual checks are supplied in VALIDATION.md. Other experimental ports remain excluded from standard release ZIPs.

Author: **Superbroche**. Sources and release: [Custom Book Creator on GitHub](https://github.com/superbroche-s-organisation/Custom-Book-Creator).
