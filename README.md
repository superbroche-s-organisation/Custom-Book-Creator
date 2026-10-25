# Custom Book Creator

Source code for **Custom Book Creator**, with separate plugin packages for **MCreator 2026.2, 2024.4 and 2022.2**.

Author: **Superbroche**.

The current development snapshot is **2.2.0-test.4**. Book audio now follows Minecraft's **Jukebox/Note Blocks** volume setting, for both automatic playback and sound buttons. Categories appear directly in one scrollable list, without the duplicate selection header. It retains the sound elements, white translation editor, writing help, centered signing, writing footer, Undo/Redo and Delete features. The MCreator 2024.4 package covers **Forge 1.20.1 and NeoForge 1.21.1**. See the [current patch notes and checks](docs/2.2.0-test.4/PATCH_NOTES.md), the [feature guide](docs/2.2.0-test.4/GUIDE.md) and the [broader validation record](docs/2.2.0-test.2/VALIDATION.md). This snapshot has no GitHub release; the public release below is still V2.1.0.

**V2.1** adds player-writable zones, preset choices, server-side writing permissions and permanent signatures to the visual book editor. V2's rich-text editor, independent paragraph alignment, page-title styling, media, drag-and-drop organization and starting books remain available.

Download the plugin variants, sources and MCreator upload kit from [V2.1.0 on GitHub](https://github.com/superbroche-s-organisation/Custom-Book-Creator/releases/tag/v2.1.0). See the [patch notes](docs/2.1.0/PATCH_NOTES.md), [changelog](CHANGELOG.md), [writing guide](docs/2.1.0/GUIDE.md), [compatibility and validation](docs/2.1.0/VALIDATION.md), and [feature description](docs/2.1.0/MCREATOR_DESCRIPTION.md).

This release succeeds V2.0.0. Historical test documents describe their snapshots, not necessarily current behavior.

## Compatibility

| Plugin ZIP | Generator profiles in the archive |
| --- | --- |
| MCreator 2026.2 | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |
| MCreator 2024.4 | Forge 1.20.1; NeoForge 1.21.1 (added in 2.2.0-test.1) |
| MCreator 2022.2 | Forge 1.17.1 |

A matching **workspace generator compatible with that MCreator version** must also be installed. The plugin ZIP does not supply a complete generator and cannot downgrade a workspace. The 2022.2 ZIP uses Java 17 plugin bytecode; the other two use Java 21. Generated game code follows the selected generator's Java requirements.

V2.1 deliberately ships only profiles with successful generated-code compilation. It does **not** promise every Forge/NeoForge/Fabric combination from 1.17.1 to 26.2. The other experimental 1.20.x adapters remain in the sources but are excluded from standard release ZIPs. If your V2 workspace uses one of those excluded profiles, keep its previous plugin until that port is validated; back up before changing plugins. Build with `-IncludeExperimental` only for development (the output is explicitly named `-experimental`).

## Build

Run from PowerShell:

```powershell
.\build.ps1 -MCreatorRoot "C:\Path\To\MCreator"
```

The script compiles against the selected MCreator installation and creates the installable ZIP in `dist`. For older packages add `-TargetMCreator 2024.4` or `-TargetMCreator 2022.2` and use that version's installation path.

## Test

```powershell
.\test.ps1 -MCreatorRoot "C:\Path\To\MCreator" -GeneratedSourcesDirectory "build\generated-validation"
```

This rebuilds the plugin and runs the applicable Java regression tests. The optional output directory retains representative generated sources for compilation against Minecraft and loader APIs. Use `test-mcreator2022.ps1` for the legacy package. See the [validation report](docs/2.1.0/VALIDATION.md) for exact commands and remaining manual checks.

The development snapshot passes 26 distinct automated programs on 2026.2 and 25 applicable programs on 2024.4, including Swing editor/drag tests, generated interaction and scaled-border tests, and 73 writing protocol/client checks. Real generated-code compilation is recorded separately for each packaged profile. No interactive gameplay or dedicated-server launch test is claimed.

## Installation and upgrade

Back up the workspace and close MCreator before replacing the old plugin ZIP. Keep only one Custom Book Creator plugin installed, including any copy in the MCreator installation's `plugins` directory as well as the user plugin directory. An old installation-directory copy can mask a newer user-directory copy.

Install only the `CustomBookCreator-MCreator<your-version>-v<version>.zip` matching your installation. Restart MCreator, regenerate the workspace code and build it. A **sources ZIP or MCreator upload kit is not a plugin** and must never be placed in a `plugins` directory. MCreator attempts to load every ZIP there and can report `plugin is null` for a source archive.

**After replacing a plugin ZIP, use MCreator's Regenerate code action.** Existing books keep their previously generated Java until regeneration; replacing the ZIP alone cannot update their in-game keyboard handling or interface. Regeneration applies the new behavior to old books as well.

## Localization

English is the source language. The editor keeps its 30 MCreator locale catalogs. New controls/help have English fallbacks, with French translations supplied. In-game writing controls are in English.

Use **Translations** above the book preview to translate authored book titles, categories, page titles, body text, button/sound labels and writing-zone labels. These entries use MCreator's normal Localization resources and follow the player's Minecraft language, with the original text as fallback. The translation editor displays white text while preserving saved color tags. See the [translation guide](docs/2.2.0-test.2/GUIDE.md#book-content-translations) for the scope and stable keys.

## Starting books

Each custom book can optionally be granted once to every player on their first world join. A persistent per-player marker prevents the book from being granted again after death or reconnection.

Version 1.1 makes the grant server-only, uses the correct persistent-data API for each supported NeoForge version, and records the marker only after the book has been handed to the player.

## Book organization and navigation

- Drag categories to reorder them.
- Drag pages to reorder them or move them to another category.
- Page and category identifiers stay stable, so existing internal page buttons continue to target the same page after a move.
- Enable **Stop on the last page of each category** in Properties to hide and disable the right arrow on a category's final page. Leave it disabled to continue directly to the next category.
- Scroll the category list to reach every category. The selection header/chevron and old sidebar page counter/arrows are removed. Signing is centered at the bottom; writing controls appear below the right-hand page.

## Compatibility fixes

The GUI provides the JVM bridge required by MCreator 2026.2:

```text
getElementFromGUI()Lnet/mcreator/element/GeneratableElement;
```

The plugin also ensures that a valid Mixin configuration is generated when GeckoLib Reborn declares one. Nerdy's Player Animator keeps its higher-priority Mixin template when installed.
