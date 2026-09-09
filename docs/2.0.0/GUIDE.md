# V2 user guide

Author: **Superbroche**

## Installation

Back up the workspace and close MCreator. Keep only `CustomBookCreator-MCreator2026.2-v2.0.0.zip` installed across both the MCreator installation and user plugin directories. Move older copies outside those directories, then restart MCreator and regenerate the workspace code. Never install the sources or upload-kit archive.

If the editor still shows numeric generation, raw style tags or no title controls, check the MCreator log for a duplicate `custom_book_creator` plugin and the path of the version actually loaded.

## Generation and text

Book generation offers Original, Copy of original, Copy of copy and Tattered. The underlying values are still 0–3.

Select a text zone to edit it visually. Select characters before applying font size, color or style; without a selection, character styles affect what you type next. Alignment applies to the current paragraph or the paragraphs touched by a selection. To center only a heading, place it on its own paragraph and center that paragraph, leaving the body left aligned.

The toolbar offers style toggles, links, undo and redo. Keyboard undo/redo and formatted copy/paste are supported. Plain clipboard text excludes the internal formatting tags. Existing valid tags are rendered as styling automatically. Obfuscation is represented by a shaded monospaced style so the original text remains readable while editing; its actual animated effect appears in game.

The save format has a 32,767 UTF-16-code-unit limit, including markup. An operation exceeding it is rejected instead of cutting off text. Old data already truncated by previous versions cannot be reconstructed.

## Page titles

Select a page and click **Edit page title**, or click its circular preview handle. The shared visual editor and toolbar now edit the title. Select a body element to switch back to it.

Choose Top, Bottom, Left or Right placement. Side titles rotate along the edge. Adjust the title-area height, distance from the edge, and separator bar visibility, color, length and thickness.

Drag the circular handle toward or away from the selected edge. Snap to grid applies to the handle even if the separator is hidden. Hiding the page title hides its handle as well. Formatting and layout are independent for each page and survive copies, saves and reloads. Body elements keep their coordinates: move them manually if a large title band overlaps them.

## Compatibility

MCreator 2026.2 and Java 21 plugin bytecode are required. Existing profiles: NeoForge 1.21.1 and NeoForge 26.1.2.

| Loader | Experimental 1.20.x profiles |
| --- | --- |
| Forge | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.6 |
| NeoForge | 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |
| Fabric | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |

These are extensions, not complete workspace generators. A matching generator compatible with MCreator 2026.2 must be installed separately. Not every combination has a compatible available generator or has been validated as a complete mod. Fabric adapters expect official Mojang mappings. Fabric OBJ selections use the normal item texture; normal and custom JSON models are supported.

## Language behavior

The plugin keeps its 30 MCreator interface-language catalogs. New title controls use English fallback outside English and French. Book-content translation keys are not supported in V2. Former `[tr=...]` strings remain literal; replace them with the desired text.

## Checks for your workspace

After upgrading, test save/reopen, repeated size changes, mixed paragraph alignment and title movement on all four edges. Regenerate and build the workspace. In game, check title wrapping at your GUI scale, media/GIFs, internal/external links, category navigation, and starting-book behavior on a new world, after reconnect and after respawn. Back up before experimenting with additional generators.
