# Custom Book Creator 2.2.0-test.1

Author: **Superbroche** | October 25, 2026

Development snapshot following V2.1.0. Sources are pushed to GitHub for testing; this snapshot is not a published release.

## Book editor

- **Undo book edit / Redo book edit** above the preview now cover category/page organization, element creation and deletion, geometry, formatting, titles and page audio. Restoring a page preserves the redo branch. The rich-text editor retains its separate text history.
- Select a user element in the preview and press the keyboard **Delete** key, also labelled **Del** or **Suppr**, to remove it. Deleting characters in a text field remains normal text editing. Built-in navigation arrows and the page title are protected from this shortcut.
- Detailed native help explains the persistent field key, the three input modes, default plain text and predefined choices. English help is available in all 30 locale folders, with French translations.

## In-game interaction

- Players edit a writing zone directly on its book page. Save, Cancel, Free text/Choices and Next choice controls appear in the category sidebar. Server permissions, per-copy storage, limits and permanent signing still apply.
- The configured inventory key closes the book when no zone is being edited. During editing it remains available for typing; Escape exits editing first, asks before discarding changes, and cannot interrupt a pending write.
- **Sign book...** is at the bottom of the category sidebar. Writing books show fewer categories per sidebar page to reserve room for actions.
- Each page can play a workspace sound either automatically when opened or through a **Play sound / Stop sound** button. Audio uses the Music volume slider and stops when the page or book is left. It does not loop or stop unrelated sounds.

## Translations

- The **Translations** dialog edits book titles, category labels, styled page titles, rich body text, button labels and writing-zone labels per Minecraft language.
- Entries use MCreator's Localization resources. The player's current language selects the translation, with original book text as fallback. Moving or renaming a page/element preserves its key.
- Player answers, preset values and signatures retain their canonical saved text; this feature translates authored presentation text. In-game control labels remain English in this snapshot.

## Compatibility and reported issues

- The MCreator **2024.4** ZIP now includes **NeoForge 1.21.1** in addition to Forge 1.20.1. A matching generator is required for Custom book to appear.
- Native 2024.4 plugin loading, generator coverage, book generation, save and reload pass for both profiles. The exact 2024.4b save failure in [issue #2](https://github.com/superbroche-s-organisation/Custom-Book-Creator/issues/2) has not been reproduced from the information supplied; this is not a claim that every beta-specific failure is fixed.
- Page audio implements the feature requested in [issue #3](https://github.com/superbroche-s-organisation/Custom-Book-Creator/issues/3).
- Forge 1.17.1 uses its older language API. The separate MCreator 2022.2 package remains available.

See the [guide](GUIDE.md) and [test evidence and remaining checks](VALIDATION.md). Back up the workspace, close MCreator and replace the previous plugin with the ZIP matching your MCreator version. Regenerate workspace code after updating.
