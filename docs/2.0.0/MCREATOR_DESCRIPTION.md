# Custom Book Creator V2.0.0

**A visual guidebook and interactive manual editor for MCreator 2026.2, by Superbroche.**

Custom Book Creator adds a dedicated Custom Book mod element for building in-game guides, tutorials, encyclopedias, lore books, quest introductions and interactive manuals. Each book is generated as a Minecraft item with its own reader screen, organized into categories and pages.

V2 introduces directly formatted text editing, independent paragraph alignment and fully customizable page titles. You can build your book visually without writing a custom screen by hand.

## Visual page layout

Create pages from independently positioned text areas, PNG images, animated GIFs, buttons and navigation controls. Select elements in the live preview, drag them into place, resize them with handles or set their coordinates and dimensions numerically. Use a visible grid, configurable grid step and optional snapping to keep layouts consistent. Duplicate or delete elements and customize each page independently.

## Word-style visual text editing

The text editor displays the formatting itself rather than exposing internal tags such as `[size=12]` or `[b]`. Select just the characters you want to change and apply font size, custom color, bold, italic, underline, strikethrough or obfuscation. Without a selection, character-style changes affect text typed next.

Paragraphs have independent left, center or right alignment. A centered heading and left-aligned body can share the same text area. The editor also supports clickable web links, internal page links, formatted copy/paste, plain-text copying without internal tags, and undo/redo.

Existing valid formatting is loaded into the visual editor automatically. V2 corrects the repeated-size-tag problem caused by successive font-size changes. Obfuscation remains readable as shaded monospaced text while editing; Minecraft renders its animated effect in game.

## Fully customizable page titles

Click **Edit page title** to use the same rich-text toolbar for a page's title. Change size, colors, styles and alignment independently on each page.

Titles can appear at the top, bottom, left or right edge. Side titles rotate along the edge. Adjust the title area, its inward distance from the edge, and a separator bar with configurable visibility, color, length and thickness. A circular movement handle lets you bring the title closer to or farther from the edge, with optional grid snapping even when the separator is hidden.

Title appearance is saved with the page and preserved when copying categories. Existing body elements retain their coordinates, giving you control over how they fit around the title.

## Categories, pages and navigation

Organize a book into named categories and pages. Add, rename, copy or remove content; drag categories to reorder them, drag pages to reorder them, or move pages between categories. Stable identifiers keep internal destinations attached to the same pages after a move.

The reader includes category navigation and page numbering. Books with more than eleven categories use sidebar pagination. Enable **Stop on the last page of each category** to hide and disable the next-page arrow at that category's end. Leave it disabled to continue into the following category.

Previous/next arrows are editable independently on every page. Customize their position, size, container style, symbol, colors and optional texture. Custom navigation images can use GUI, item or block textures. Arrows appear only when the corresponding navigation action is available.

## Images and animations

Import PNG images or animated GIFs into the workspace. GIF frames are extracted for rendering, with a configurable frame delay. Position and scale media independently, and choose side/corner resizing behavior. The preview shows imported media while you work.

Imports include format, dimension, frame-count and decoded-pixel checks to reject invalid or excessive media. Workspace save/reload regression checks cover embedded image data.

## Interactive buttons and links

Create buttons that open a category or a specific page. Set the label, position, size, background, border and text colors. Choose Classic, Flat, Outline or Transparent styling, and optionally combine it with a GUI, item or block texture.

Image placement is separate from the button style: disable the image, show it as a left-side icon or use it as the background. Rich-text links can target another book page or open a website through Minecraft's URL confirmation screen.

## Book item properties and visuals

Configure the in-game item name, book title and author, creative inventory tabs, stack size, enchantability, fire immunity, piglin currency behavior and block-drop tool property. Rarity offers Common, Uncommon, Rare and Epic, with help explaining their associated colors.

Book generation is now a named list: Original, Copy of original, Copy of copy and Tattered. Existing numeric values remain compatible with saved books.

The Visuals page uses native MCreator selectors for the item texture and model, plus an enchanted-glint option. Use the normal item appearance, an imported JSON model or, on compatible Forge/NeoForge generators, an imported OBJ model. Fabric has no native Forge OBJ loader, so an OBJ selection falls back to the normal item texture.

## Optional once-only starting book

Enable Starting book to give each player one copy when they first join the world. Receipt is recorded per player and book so reconnection, respawn and later sessions do not grant another copy. If the inventory is full, the book is dropped for the player. Failed handoffs remain retryable rather than being marked as successfully received.

This is useful for onboarding guides, mod manuals and server introductions. The grant runs server-side; experimental Fabric profiles use a persistent per-world ledger.

## Interface languages

English is the source language. The plugin includes the 30 interface-language catalogs available in MCreator 2026.2, covering existing labels, dialogs and help resources. New V2 title controls have English and French translations and use English fallback in the other locales.

This describes the plugin interface, not authored book content. Book-content translation keys and automatic language-key lookup are not included in V2. Tokens entered in an intermediate test build remain literal text.

## Compatibility

- Required editor: **MCreator 2026.2**.
- Existing generator profiles: **NeoForge 1.21.1** and **NeoForge 26.1.2**.
- Plugin bytecode: **Java 21**. Generated game code follows the selected generator's Java requirements.

V2 also packages **19 experimental Minecraft 1.20.x generator extensions**:

| Loader | Included experimental versions |
| --- | --- |
| Forge | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.6 |
| NeoForge | 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |
| Fabric | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |

**These are not complete workspace generators.** A matching generator compatible with MCreator 2026.2 must be installed separately. The extensions do not make generators written for older MCreator versions compatible with 2026.2, and their presence does not guarantee full support for every loader/version combination. Fabric templates expect official Mojang mappings.

## Validation and upgrade

The V2 plugin passed **19 automated regression programs**, including actual Swing text documents and title handlers, formatting, drag-and-drop, saving/reloading, media validation and starting-book behavior. Template/extracted runtime checks cover all 21 profiles. Generated 1.20.x screens compiled against real Minecraft APIs for all seven versions. These checks are not complete in-game, dedicated-server or full-mod validation for every loader/version pair.

Before upgrading, back up the workspace and close MCreator. Move old Custom Book Creator copies out of **both** active plugin directories: the installation's `plugins` folder and the user plugin folder. An older installation-folder copy can mask a newer version. Keep only one installed copy.

Install **CustomBookCreator-MCreator2026.2-v2.0.0.zip**, restart MCreator, regenerate and build the workspace, then check your books in game. **Do not install a sources archive or the MCreator upload kit as a plugin.** Existing valid content is retained, but text already lost or truncated by an older version cannot be reconstructed automatically.

Source code, downloads and issue reports: [Custom Book Creator on GitHub](https://github.com/superbroche-s-organisation/Custom-Book-Creator).
