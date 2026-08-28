# Custom Book Creator 1.1

Source code for **Custom Book Creator**, built for **MCreator 2026.2**.

## Compatibility

- MCreator 2026.2
- NeoForge 1.21.1
- NeoForge 26.1.2
- Java bytecode 21

## Build

Run from PowerShell:

```powershell
.\build.ps1 -MCreatorRoot "D:\MCreator"
```

The script compiles against the selected MCreator installation and creates the installable ZIP in `dist`.

## Localization

English is the source language. The plugin includes every locale available in MCreator 2026.2, covering editor labels, dialogs, messages, tooltips, and help pages.

## Starting books

Each custom book can optionally be granted once to every player on their first world join. A persistent per-player marker prevents the book from being granted again after death or reconnection.

Version 1.1 makes the grant server-only, uses the correct persistent-data API for each supported NeoForge version, and records the marker only after the book has been handed to the player.

## Book organization and navigation

- Drag categories to reorder them.
- Drag pages to reorder them or move them to another category.
- Page and category identifiers stay stable, so existing internal page buttons continue to target the same page after a move.
- Enable **Stop on the last page of each category** in Properties to hide and disable the right arrow on a category's final page. Leave it disabled to continue directly to the next category.

## Compatibility fixes

The GUI provides the JVM bridge required by MCreator 2026.2:

```text
getElementFromGUI()Lnet/mcreator/element/GeneratableElement;
```

The plugin also ensures that a valid Mixin configuration is generated when GeckoLib Reborn declares one. Nerdy's Player Animator keeps its higher-priority Mixin template when installed.
