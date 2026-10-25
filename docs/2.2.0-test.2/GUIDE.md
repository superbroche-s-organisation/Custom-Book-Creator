# Test.2 usage guide

## Install and update existing books

Close MCreator and back up the workspace. Replace the previous Custom Book Creator ZIP with the package matching your MCreator version, keep only one installed copy, then restart MCreator. **Run Regenerate code, then build/run the mod.** Existing generated book Java is not replaced merely by installing a newer plugin ZIP. Regeneration updates old books as well; the existing-book E-key case was confirmed resolved after this step.

| MCreator | Included profiles |
| --- | --- |
| 2026.2 | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |
| 2024.4 | Forge 1.20.1; NeoForge 1.21.1 |
| 2022.2 | Forge 1.17.1 |

Install a matching workspace generator separately. Sources/test-kit archives are not installable plugins. Experimental profiles remain excluded from standard ZIPs.

## Sound/music elements

1. Import an audio resource as a workspace sound in MCreator.
2. Select a book page and click **+ Sound/music**.
3. In the element inspector, select the sound resource and its playback mode.
4. **Page sound (on open)** plays once on entry. Its placeholder is visible in the editor for selection; it is invisible in game.
5. **Play / stop button** creates an in-page control. Set its label and use the preview/position/size fields to move or resize it.
6. Save the element, regenerate code and build the mod.

Each sound element owns its playback instance. Button controls do not stop other elements or unrelated game audio. Page changes and closing the book stop that page's instances. Finished automatic sounds do not restart every frame. Button sounds can be replayed. Playback uses the Music volume setting and does not loop. **No sound** keeps an unconfigured element silent.

Sound elements can be duplicated, removed with Delete and restored through general Undo/Redo. Existing test.1 page sound settings migrate once to an element preserving the sound and mode. The previous global sound row and sidebar sound button are removed.

## Categories, writing and signing

Click the current category at the top of the sidebar to open its dropdown. Scroll the list with the mouse wheel; a scrollbar indicates longer lists. Select an entry to navigate. Escape first closes the open dropdown. The old category page counter/arrows are gone.

Click a writing zone to type directly inside it. **Free text / Choices**, **Next choice**, **Save** and **Cancel** appear in two rows **below the right-hand page**, outside the category pane. The layout reserves footer space so the controls fit on screen. Category navigation remains available while editing, but a dirty draft requires discard confirmation and pending server writes block navigation. Cancelling the confirmation preserves the draft.

**Sign book...** is centered at the bottom of the category pane when permitted. Signing permanently locks that copy and still uses its signature/title confirmation screen. Inventory-key closing respects the player's remapped key and does not interrupt inline typing. Escape exits editing before closing the book, with confirmation for unsaved changes. Zone borders remain closed at fractional GUI scales, including during editing.

The four help icons next to Persistent field key, Input mode, Default plain text and Predefined choices load detailed native help. Existing [writing limits, permissions, presets and signatures](../2.1.0/GUIDE.md) still apply.

## Book-content translations

Use **Translations** above the preview, choose a Minecraft locale code (`fr_fr`, `en_us`, etc.), select an entry, then edit its translation. Both source and translation text display white in this dialog. Original color tags are preserved for the game. Rich styling is supported for body text and page titles; labels stay plain. Copy source and the rich formatting controls remain available where applicable.

Confirm with OK, save the book/workspace, regenerate resources/code and rebuild. Translations use MCreator's Localization resources, with stable keys prefixed by `custombook.<modid>.<book_registry_name>.` and suffixes `title`, `category.<id>`, `page.<id>` or `element.<id>`. Moving/renaming pages and elements keeps their IDs. Sound-button labels are included. Missing translations fall back through Minecraft's language resources and then the original book text.

Player-written answers, default/preset answer values and signature title/author remain canonical text. This is manual localization of authored presentation text. Built-in in-game controls remain English in this snapshot. General Undo does not undo Localization-resource edits. The [test.1 guide](../2.2.0-test.1/GUIDE.md#book-content-translations) describes key management and these boundaries in more detail.

## Editor shortcuts and history

Select a user element in the preview and press **Delete / Del / Suppr** on the keyboard. Text fields retain normal character deletion. Built-in navigation arrows and the page title are protected. General **Undo book edit / Redo book edit** cover categories, pages, elements, geometry, titles and audio; the rich-text editor has its own document history. History remains local to the open editor and bounded as described in the [test.1 guide](../2.2.0-test.1/GUIDE.md#general-editor-actions).
