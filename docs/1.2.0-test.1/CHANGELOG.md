# Custom Book Creator 1.2.0-test.1 — local preview

Author: **Superbroche**. Not a public release.

## Fixed

- Changing the font size repeatedly now replaces the selected text's size instead of accumulating duplicate size tags.
- Multiline selections use Swing document offsets, preventing Windows line endings from shifting selections into closing tags.
- Existing styles and text outside a selection retain their formatting.
- Size and alignment controls reflect the first character of the current selection without modifying the text when the caret moves.

## Added

- Independent paragraph alignment within one text zone: center a heading while keeping its body left aligned.
- Minecraft language-key support using `[tr=book.example.introduction]`, plus an editor button to insert keys. Rich-text tags can appear in translation values.
- Translation-key resolution in book titles, category names, page titles, text zones and button labels.
- Nineteen experimental 1.20.x generator extensions for Forge, NeoForge and Fabric, with adaptations for older screen APIs, item NBT/data components and platform events.
- A Fabric starting-book ledger saved per world and player, including reconnect/respawn persistence and failed-handoff retry behavior.
- Regression coverage for text editing, translated/mixed-alignment rendering and the seven Fabric starting-book variants.

## Before testing

Back up your workspace and install only one copy of the plugin. Regenerate the workspace code after upgrading. The 1.20.x profiles require compatible installed generators; the ZIP does not supply complete generators. Fabric OBJ selections fall back to the normal texture. Translation values must be provided by the mod author.

See the [testing guide](TESTING.md) for the compatibility matrix and examples, and the [validation report](VALIDATION.md) for exactly what was tested and what remains manual.
