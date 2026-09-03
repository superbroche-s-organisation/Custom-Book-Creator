# Custom Book Creator 1.1.2-hotfix

Author: **Superbroche**

This maintenance update focuses on protecting saved book data, making page organization more reliable, and preventing invalid content or media from breaking the editor or generated code.

Author attribution and the internal Java namespace now consistently use **Superbroche**. The plugin identifier and saved-book type remain unchanged, so existing books do not need a namespace migration.

## Compatibility

- **MCreator 2026.2**
- **NeoForge 1.21.1** generator
- **NeoForge 26.1.2** generator

The plugin targets Java 21 bytecode. Generated code was also compiled against the corresponding game APIs for both supported generators, including the Java 25 target used by NeoForge 26.1.2.

## Workspace loading and saved data

- Improved recovery of legacy and partially damaged book structures so missing categories, pages, or elements do not abort workspace deserialization.
- Imported image data is covered by repeated save/reload regression tests using MCreator's actual serialization path.
- Missing or duplicate identifiers are normalized, and invalid item properties, texture references, or custom-model names receive safe defaults.
- Fixed an editor refresh issue that could overwrite a button's selected image while its inspector fields were being populated.

These safeguards repair supported data structures; they cannot reconstruct files that are already missing or repair arbitrarily corrupted JSON syntax. Keep a backup before upgrading.

## Drag-and-drop and internal navigation

- Reorder categories, reorder pages within a category, and move pages between categories while preserving their content, media, and stable identifiers.
- Invalid, stale, detached, or foreign-tree drop targets are rejected.
- Failed moves roll back instead of leaving the book in a partially moved state.
- Moving the only page out of a category creates a usable replacement page in that category.
- Internal page buttons are repaired after page/category moves or deletions, with valid fallback destinations when their original target no longer exists.
- Books with more than eleven categories use sidebar pagination, keeping every category reachable in game.

The existing **Stop on the last page of each category** property remains available: enable it to hide and disable the right arrow on that category's final page, or leave it disabled to continue into the next category.

## Rich text and long content

- Supplementary Unicode characters, such as emoji, are kept intact in both the editor preview and generated screen logic.
- Closing formatting tags now follow consistent matching rules: unmatched closing tags are ignored, while a matching closing tag also closes any still-open nested formatting.
- Text truncation no longer splits a Unicode surrogate pair.
- The existing maximum text length of **32,767 UTF-16 code units** remains supported.
- Long non-ASCII content is emitted as safe Java string chunks to avoid the single-string constant-size compilation limit.

## Image and GIF import safety

- PNG files are checked for their actual format and dimensions before pixel decoding or large image allocation.
- Renamed files with an unsupported underlying format are rejected instead of being copied as PNG textures.
- GIF imports check frame dimensions, logical canvas size, frame count, and the total decoded-pixel budget.
- Known GIF canvas dimensions and frame count are checked together before allocating oversized animations.
- Invalid, truncated, or excessive media is rejected through the import error path.

The current guards allow at most **8,192 pixels per dimension**, **500 GIF frames**, and a **64-million decoded-pixel budget**. An image or animation may reach the pixel budget before reaching the individual dimension or frame limits.

## Validation

**14/14 automated test programs passed against the freshly packaged plugin.** The checks include:

- MCreator's required save-method binary contract and actual serialization/reload cycles.
- **251 dynamic drag-and-drop assertions**, including injected failures and rollback.
- **36 dynamic media-import assertions**, also passing separately with a 128 MB Java heap.
- **11 dynamic rich-text preview assertions**.
- Model normalization, template rendering, generated screen navigation, links, Unicode, and access to 25 categories.
- Starting-book login/clone behavior in an isolated API fixture, including server-only handling, once-only grants, reconnects, inventory overflow, retry after failure, and marker preservation.
- Localization structure across 30 catalogs and their help pages.

Representative generated item, screen, client-event, and starting-book code compiled successfully against the real APIs for both **NeoForge 1.21.1** and **NeoForge 26.1.2**.

### Validation limits

The automated tests are headless. A full **`runClient` session was not completed**, and this validation does not replace a visual in-game review, a physical mouse-drag check in MCreator, or a real multiplayer/dedicated-server join and respawn test. Translation completeness was checked programmatically, not reviewed by native speakers for every language.

See [VALIDATION.md](VALIDATION.md) for the full test inventory, compilation details, and remaining manual checks.

## Installation and upgrade

1. **Back up your workspace** and keep a copy of the plugin version you currently use.
2. Close MCreator.
3. Remove the old Custom Book Creator plugin ZIP from the active plugin folders. Check both the MCreator installation's `plugins` folder and the user plugin folder: an old installation-folder copy can mask a newer user-folder copy.
4. Install only **`CustomBookCreator-MCreator2026.2-v1.1.2-hotfix.zip`**. Keep **one installed copy** of Custom Book Creator across those locations.
5. Restart MCreator, open the workspace, and regenerate the workspace code.
6. Check the book editor and build the workspace before continuing normal development.

**Do not install the sources ZIP.** It contains development files, not an installable plugin. Placing it in a `plugins` folder can cause MCreator to display a plugin-loading error such as `plugin is null`.

The source archive and repository files are intended for development, review, and rebuilding. See the [README](../../README.md) for build and test commands.
