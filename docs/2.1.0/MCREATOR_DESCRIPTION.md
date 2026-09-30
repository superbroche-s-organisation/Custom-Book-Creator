# Custom Book Creator

**Custom Book Creator by Superbroche** adds a visual book mod element to MCreator. Build illustrated guidebooks, tutorials, interactive manuals, roleplay forms and contracts without designing an entire screen in Java.

## Visual book design

Organize a book into categories and pages. Reorder categories by drag and drop, move pages within a category or between categories, and keep internal page links intact. Choose whether the next-page arrow crosses category boundaries or stops at the last page. Sidebar pagination keeps large category lists accessible.

Write body text and page titles in the visual rich-text editor: formatting is displayed directly instead of exposing markup. Apply bold, italic, underline, strikethrough, obfuscation, color and font size to a selection, and use different paragraph alignments within one text area. The editor includes undo/redo, clipboard support and internal-page or web links.

Customize each page title independently: typography, alignment, top/bottom/left/right placement, inward offset and title-area height. Configure the separator's visibility, color, length and thickness, and move the title with a grid-aware handle.

Add text zones, imported images, animated GIFs and navigation buttons. Adjust placement, size, appearance and image fitting, with the page canvas and grid helping to arrange the layout. Customize page arrows independently.

## Player writing and contracts — new in V2.1

Add editable writing zones alongside your static text, illustrations and buttons. A zone can accept free text, offer predefined choices, or allow both. Each physical book copy stores its own answers and writable copies cannot stack.

Choose a server-side boolean procedure that controls who may edit or sign. Optional permanent signing records the actual player's name and UUID and locks the copy's writing zones. An optional post-signature procedure lets your mod apply its own contract rules. Individual answer and signature fields are exposed through the itemstack's custom data/NBT for procedures.

The in-game editor provides multiline input, text selection, clipboard support, choice cycling, save confirmation and unsaved-change prompts. Server-side validation checks permissions, held item identity, session tokens, text limits and preset values. Saves are not reported as successful until the server confirms them.

## Item settings

Configure the displayed item name, rarity, creative tab, stack size, enchantability, fire immunity, glow and other supported item properties. Choose a texture and a resource model. JSON model support follows the generator; Fabric does not include an OBJ loader and uses the item texture as a fallback for that option.

Enable **Starting book** to grant a copy once per player per world. Existing V2 features, including the named book-generation choices, remain available.

## Supported packages

- **MCreator 2026.2:** Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2 and 26.2; Fabric 26.2.
- **MCreator 2024.4:** Forge 1.20.1.
- **MCreator 2022.2:** Forge 1.17.1 through the separate legacy plugin ZIP.

Install the appropriate compatible workspace generator as well: these ZIPs extend generators, they do not supply complete replacements. Install one Custom Book Creator variant only. Newer workspaces cannot be downgraded by changing the plugin ZIP.

The editor retains its MCreator language catalogs; new writing labels fall back to English where needed, and the in-game writing interface is English. Writing limits are 64 zones, 1,024 UTF-16 units per zone, 4,096 total and 16 preset choices per zone.

## Installation and testing notes

Back up your workspace, close MCreator, remove older copies, install the matching plugin ZIP, restart, regenerate code and build. Do not install source ZIPs or the upload kit in the plugins directory. Server and clients need the same regenerated mod.

This release is checked with automated regression tests and actual generated-code compilation. Interactive in-game and dedicated-server launch tests were not performed. The exact validation record and deferred compatibility targets are included in the release. No blanket support for every loader/version between 1.17.1 and 26.2 is claimed.
