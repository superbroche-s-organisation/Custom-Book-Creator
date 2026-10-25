# Development snapshot usage guide

## General editor actions

The **Book actions** row above the preview contains **Undo book edit**, **Redo book edit** and **Translations**. General history includes page/category order, user elements, title design and page sound settings. It is local to the open editor, bounded to 64 snapshots and approximately eight million serialized characters, and resets when opening another book. It does not undo imported resource files, general item properties or changes to the workspace's Localization resources. The text toolbar's Undo/Redo affects its current rich-text document.

Click an element in the preview, then press **Delete / Del / Suppr** on the keyboard. This removes the selected user element. The preview must have focus; a text input keeps normal character deletion. Built-in navigation arrows and the page title cannot be deleted with this shortcut. Use general Undo to restore a removed element.

## Per-page audio

1. Import the audio as a workspace sound in MCreator.
2. Select a page in the book tree and select its **Page sound**.
3. Choose **On page open** or **Play / stop button**.
4. Save the book, regenerate code and build the mod.

The page owns its playback instance. Leaving the page or closing the book stops that instance, while other game audio continues. Automatic playback runs once on entry, not on every frame; returning to a page starts it again. Button playback can be stopped or replayed. Audio uses Minecraft's **Music** volume setting and does not loop. **No sound** keeps the page silent. A missing/deleted workspace resource must be repaired in the sound selector/resources.

## Player writing and signing

Add a **Writing zone** and configure its label, mode, character limit, default value and optional choices. Help icons explain the fields:

- **Persistent field key** identifies the zone's saved answer on each book copy. Reordering or moving the zone keeps the same key. Replacing/deleting it changes access to the old saved answer.
- **FREE** accepts player text, **PRESET** accepts one configured value, and **BOTH** lets the player switch between these modes.
- **Default plain text** supplies the initial content before a player saves an answer; it does not overwrite an existing saved answer.
- **Predefined choices** contains one canonical value per line, up to 16. Values must satisfy the zone's character limit and the book's total limit.

In game, click the zone to write directly inside it. Sidebar controls select the mode/next choice and save or cancel. Saving uses the existing server validation; opening a visual editor does not bypass permissions. Escape leaves editing first and confirms discarding unsaved text. A pending server request must complete or time out before cancellation. The inventory key (normally E) closes the book outside editing and remains available while entering text.

**Sign book...** is at the bottom of the category sidebar when signing is enabled and permitted. Signing still uses a confirmation/title dialog and permanently locks that copy. Category pagination reserves room for writing, sound and signing controls. Existing server procedures and writing limits from the [V2.1 guide](../2.1.0/GUIDE.md) still apply.

## Book-content translations

1. Click **Translations** above the preview.
2. Select or enter a Minecraft locale code, such as `fr_fr`, `en_us` or `de_de`.
3. Select an entry on the left. Its source and complete key appear on the right.
4. Enter the translation, or use **Copy source**. Page titles and text zones support visual formatting; plain labels use plain text.
5. Confirm with **OK**, save the book/workspace and regenerate the mod's code/resources.

Entries are also editable in MCreator's **Localization** tab. The full key has the prefix `custombook.<modid>.<book_registry_name>.`, followed by:

| Text | Key suffix |
| --- | --- |
| Book title | `title` |
| Category label | `category.<category_id>` |
| Styled page title | `page.<page_id>` |
| Text zone, button label or writing-zone label | `element.<element_id>` |

The IDs remain stable when pages/elements are moved or renamed. Duplicated elements receive their own IDs. Changing the mod ID or book registry name changes the key prefix. Deleted content can leave unused language keys; the dialog preserves unrelated entries rather than removing them.

Minecraft resolves the current language when drawing the book. A missing entry falls back to the book's original text. Leaving a translation blank removes that locale entry; Minecraft's normal language fallback, including `en_us` where applicable, can still supply it. The dialog's Cancel discards its draft changes. General book Undo does not revert Localization edits.

The translation system covers authored titles, categories, rich body text and labels. It does not translate a player's saved answer, preset choice values, default answer values, signature title/author or the snapshot's built-in English controls. Preset values remain canonical because the server validates them against the configured choices. This is manual per-language localization, not automatic machine translation.

## Choosing a plugin ZIP

| MCreator | Included workspace profiles |
| --- | --- |
| 2026.2 | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |
| 2024.4 | Forge 1.20.1; NeoForge 1.21.1 |
| 2022.2 | Forge 1.17.1 |

The generator must be installed separately and must support that MCreator version. Close MCreator before replacing the plugin, keep only one installed copy and never install a sources archive as a plugin. The experimental 1.20.x ports are excluded from these normal ZIPs.
