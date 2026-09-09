# Custom Book Creator 1.2.0-test.2

Author: **Superbroche**. Unpublished local test build for MCreator 2026.2.

## Install safely

Back up the workspace and close MCreator. Replace the installed Custom Book Creator plugin with `CustomBookCreator-MCreator2026.2-v1.2.0-test.2.zip`, keeping only one installed copy across both plugin directories. Reopen a copy of the workspace and regenerate its code. Never install a sources archive as a plugin.

## Book generation

Book Properties now offers a named list: **Original**, **Copy of original**, **Copy of copy**, and **Tattered**. These map to the existing Minecraft generation values 0–3, so old books retain their selection. The generation help indicator has been removed.

## Visual text editing

Text zones show formatted text instead of the internal markup. Use the toolbar to apply font size, color, bold, italic, underline, strikethrough, obfuscation, links and paragraph alignment. Existing valid formatting tags are loaded into the visual editor automatically; the internal save format remains compatible.

- With a selection, character styles change only that selection.
- Without a selection, character styles apply to text typed next.
- Alignment affects the selected paragraphs, or the paragraph containing the caret.
- Bold, italic, underline, strikethrough and obfuscation can be toggled on and off.
- Undo/redo and formatted copy/paste are available. Plain clipboard text does not contain internal tags.
- Obfuscated text is represented by a shaded monospaced style in the editor, retaining readable original characters; Minecraft renders its obfuscation effect in game.
- Formatting that would exceed the save-format limit is rejected without truncating text.

## Page titles

Select a page and click **Edit page title**, or click the circular title handle in the preview. The same visual editor and formatting toolbar now edit the title. Selecting a body element returns to that element without changing the title.

The title layout controls provide:

- **Top**, **Bottom**, **Left** or **Right** placement. Side titles rotate along the page edge.
- The title area's size and its inward distance from the edge.
- Title bar visibility, color, length and thickness.
- A circular movement handle that moves the title closer to or farther from the selected edge. **Snap to grid** applies to this handle, even when the title bar is hidden.

Title formatting and layout are independent for each page and are preserved when saving, reloading and copying categories. The page-tree label stays plain text. Body elements keep their existing positions; reposition them if they overlap a larger title band.

## Book-content localization withdrawn

The Translation key button and Minecraft language-key lookup introduced in test.1 have been removed. Existing `[tr=...]` text is preserved literally rather than silently deleted or resolved. Replace it with the desired text before testing. The plugin's own MCreator interface language catalogs remain included; only localization of authored book content has been withdrawn.

## Compatibility and remaining checks

The two existing NeoForge profiles and the 19 experimental 1.20.x extensions from test.1 remain included. These extensions require matching installed generators compatible with MCreator 2026.2; they are not complete generators. Not every loader/version combination has been validated as a complete workspace. See the [1.20.x compatibility matrix](../1.2.0-test.1/TESTING.md#120x-compatibility); the older guide's translation instructions no longer apply.

Test on a backed-up workspace:

1. Open an existing formatted book, edit selected text repeatedly, save and reopen. Verify that styles remain intact and no internal formatting tags appear in the editor.
2. Center a heading while keeping the following paragraph left aligned. Test typing new styled text, undo/redo, and copy/paste.
3. Configure titles on all four edges, change bar appearance, and drag the handle with and without grid snapping.
4. Switch between pages and between title/body editing, then save, restart MCreator and check every title again.
5. Regenerate code and run the game to review wrapping, title rotations, images, navigation and links at your GUI scale.

Automated headless tests do not replace a complete in-game visual or multiplayer check.
