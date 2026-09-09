# Custom Book Creator 1.2.0-test.1

Author: **Superbroche**. Local testing build for MCreator 2026.2.

## Changes to test

- Repeated font-size changes replace the size of the selected characters, without nesting duplicate size tags. Other styles and text outside the selection retain their formatting.
- Text editing uses the Swing document's offsets, including multiline content imported with Windows CRLF line endings.
- The size and alignment controls follow the first character of the current selection.
- Alignment affects the paragraphs touched by a selection (or the paragraph containing the caret). Centering a heading leaves the other paragraphs independent.
- Translation keys are resolved on the client in the current Minecraft language. The **Translation key** button inserts a key into a text zone.
- Nineteen experimental generator extensions cover the 1.20.x versions available for Forge, NeoForge and Fabric. The existing NeoForge 1.21.1 and 26.1.2 generators remain included.

## Minecraft translations

Use this syntax in text zones, book titles, category names, page titles, or button labels:

    [tr=book.example.introduction]

Define the same key in the mod's language files, through MCreator's localization editor or resource files.

For assets/example/lang/en_us.json:

    {
      "book.example.introduction": "[align=CENTER][size=18][b]Welcome[/b][/size][/align]\nThis paragraph is left aligned."
    }

For assets/example/lang/fr_fr.json:

    {
      "book.example.introduction": "[align=CENTER][size=18][b]Bienvenue[/b][/size][/align]\nCe paragraphe est aligné à gauche."
    }

Replace example with your mod ID. A translation value may contain the book's rich-text tags and line breaks. Changing Minecraft's language changes the displayed book text. Missing translations follow Minecraft's normal language fallback and ultimately display the key. Translation keys are expanded once, so recursive translation references are not supported.

This is localization support: it does not generate translations in languages you have not provided. The MCreator preview displays keys because its language is separate from the game's active resource packs.

## Mixed paragraph alignment

Select the heading and choose **CENTER**. Select the body and choose **LEFT**. The corresponding markup is:

    [align=CENTER][size=18][b]Heading[/b][/size][/align]
    [align=LEFT]First paragraph.
    Second paragraph.[/align]

Line wrapping preserves each paragraph's alignment. Existing text zones keep their previous block alignment as the default for paragraphs without an explicit alignment tag.

## 1.20.x compatibility

These are **generator extensions**, not complete Minecraft generators. Install a generator with the matching identifier that is compatible with your MCreator version before creating that workspace. This plugin does not make a generator written for an older MCreator release compatible with MCreator 2026.2.

| Loader | Included experimental profiles |
| --- | --- |
| Forge | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.6 |
| NeoForge | 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |
| Fabric | 1.20, 1.20.1, 1.20.2, 1.20.3, 1.20.4, 1.20.5, 1.20.6 |

Forge does not distribute a 1.20.5 version. NeoForge starts at 1.20.1; that initial version uses the Forge-compatible API. Fabric adapters expect **official Mojang mappings**, as used by the referenced MCreator Fabric generator.

Generator availability is a separate constraint: Forge 1.20.1 has a maintained MCreator 2026.2 generator, whereas several other 1.20.x generators target older MCreator versions. Consequently, this test build does **not** establish working MCreator 2026.2 workspaces for every entry in the table.

Fabric supports standard and custom JSON item models. Since vanilla Fabric has no Forge/NeoForge OBJ model loader, an OBJ selection falls back to the normal item texture. Piglin currency uses the generated vanilla piglin_loved item tag.

Fabric starting books use a per-world, per-player saved ledger. Receiving the book once remains recorded after respawn, reconnect and server restart. Inventory overflow drops the book; a failed handoff is not marked as received.

Sources for the compatibility work:

- [Maintained Forge 1.20.1 generator](https://github.com/Spectrall368/Generator-Forge-1.20.1/tree/2026.2)
- [NeoForge 1.20.4 generator](https://github.com/Spectrall368/Generator-1.20.4)
- [NeoForge 1.20.6 generator](https://github.com/Spectrall368/Generator-1.20.6)
- [Fabric 1.20.1 generator and Mojang-mapped templates](https://github.com/Goldorion/Fabric-Generator-MCreator/tree/1.20.1)
- [Official Minecraft version metadata and mappings](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)

## Installation and manual checks

1. Back up the workspace and close MCreator.
2. Replace the installed Custom Book Creator ZIP with CustomBookCreator-MCreator2026.2-v1.2.0-test.1.zip. Keep only one installed copy across the MCreator installation and user plugin directories.
3. Open a copy of a workspace and regenerate its code.
4. Select multiline formatted text and click the size arrows repeatedly. Verify that the text and other styles remain intact.
5. Center only the heading, leave the body left aligned, save, reopen and check the in-game result.
6. Add two language entries, insert their translation key, and switch Minecraft between the two languages.
7. For each available generator you use, test opening the book, images/GIFs, navigation, links and starting-book behavior in a fresh world, then after reconnect and respawn.

Already truncated text or fragments of tags saved by an older build cannot be reconstructed automatically. Valid repeated formatting tags are normalized when applying a style.

The tests are headless. A real multiplayer server, mouse-driven editor session and full in-game visual review are still part of this test build's manual checks.
