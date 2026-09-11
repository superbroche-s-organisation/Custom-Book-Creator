# Custom Book Creator V2.0.0

Author: **Superbroche** | MCreator **2026.2** | September 9, 2026

V2 brings visual text editing and much more control over page titles. It includes the work from the local 1.2.0 test builds and succeeds the public 1.1.2-hotfix release.

## Highlights

- **Visual rich-text editor:** see formatting directly instead of editing `[size]`, `[color]`, `[b]` and other internal tags. Format selected characters without changing the rest of the text. Use typing styles, undo/redo and formatted copy/paste.
- **Independent paragraph alignment:** center a heading and keep the following paragraphs left aligned within the same text zone.
- **Fully customizable page titles:** use the text toolbar for size, color, styles and alignment. Place a title at the top, bottom, left or right of its page; side titles rotate along the edge.
- **Title layout controls:** adjust the title area, its distance from the edge, and the separator bar's visibility, color, length and thickness. Move the circular handle with optional grid snapping.
- **Named Book generation choices:** Original, Copy of original, Copy of copy and Tattered replace the numeric spinner. Existing saved values are preserved; the help indicator has been removed.
- **Experimental Minecraft 1.20.x extensions:** 19 additional Forge, NeoForge and Fabric profiles, alongside the existing NeoForge 1.21.1 and 26.1.2 profiles. See the compatibility limits below.

## Fixes

- Repeated font-size changes no longer accumulate nested size tags or damage closing tags through selection-offset errors.
- Formatting controls stay synchronized with the selected text and the actual spinner value, including when using its arrows.
- Selecting another text position no longer unexpectedly revives a previous typing style.
- Formatting that would exceed the saved-text limit is rejected without cutting off content.
- Page-title formatting and layout survive copying, saving and reloading. Tree labels remain plain text, and renaming a page keeps its title styling.

Existing workspace-loading safeguards, drag-and-drop organization, once-only starting books and save-method compatibility fixes remain included.


## Compatibility and validation

The plugin requires **MCreator 2026.2**. The additional 1.20.x profiles are **experimental generator extensions, not complete workspace generators**. They require a matching generator compatible with MCreator 2026.2. Their inclusion does not guarantee a working workspace for every loader/version pair. Fabric extensions expect official Mojang mappings; Fabric OBJ selections fall back to the normal item texture.

**19/19 automated regression programs passed** against the V2 plugin ZIP. Template and extracted runtime checks cover all 21 profiles. Generated 1.20.x screens also compiled against real Minecraft APIs for all seven versions. Complete mod builds and in-game/multiplayer validation were not performed for every loader/version pair.

## Upgrade safely

1. Back up your workspace and close MCreator.
2. Move previous Custom Book Creator ZIPs out of both active plugin locations: the MCreator installation's `plugins` folder and the user plugin folder. **Keep only one installed copy.** An old installation-folder copy can hide a newer version.
3. Install **`CustomBookCreator-MCreator2026.2-v2.0.0.zip`**.
4. Restart MCreator, regenerate the workspace code, build and check your books.

**Do not install the sources ZIP or the MCreator upload kit as a plugin.** The upload kit contains the plugin ZIP, website icon and English publication text for the MCreator website.

Already missing text or fragments truncated by an older version cannot be reconstructed automatically. Keep the backup if you need to return to an older plugin version.

Full documentation: [changelog](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.0.0/docs/2.0.0/CHANGELOG.md), [user guide](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.0.0/docs/2.0.0/GUIDE.md), [validation report](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.0.0/docs/2.0.0/VALIDATION.md).
