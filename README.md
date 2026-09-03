# Custom Book Creator 1.1.2-hotfix

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

## Test

```powershell
.\test.ps1 -MCreatorRoot "D:\MCreator" -GeneratedSourcesDirectory "build\generated-validation"
```

This rebuilds the plugin and runs every Java regression test in `tests`. The optional output directory retains representative generated sources for compilation against Minecraft/NeoForge. See the [validation report](docs/1.1.2-hotfix/VALIDATION.md) for the tested scope and remaining manual checks, and the [hotfix changelog](docs/1.1.2-hotfix/CHANGELOG.md) for the fixes.

## Install this hotfix

Back up the workspace and close MCreator before replacing the old plugin ZIP. Keep only one Custom Book Creator plugin installed, including any copy in the MCreator installation's `plugins` directory as well as the user plugin directory. An old installation-directory copy can mask a newer user-directory copy.

Install only `CustomBookCreator-MCreator2026.2-v1.1.2-hotfix.zip`. The separate **sources ZIP is not a plugin** and must never be placed in a `plugins` directory. MCreator attempts to load every ZIP there and can report `plugin is null` for a source archive.

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
- Books with more than eleven categories now have sidebar pagination, so every category remains reachable in game.

## Compatibility fixes

The GUI provides the JVM bridge required by MCreator 2026.2:

```text
getElementFromGUI()Lnet/mcreator/element/GeneratableElement;
```

The plugin also ensures that a valid Mixin configuration is generated when GeckoLib Reborn declares one. Nerdy's Player Animator keeps its higher-priority Mixin template when installed.
