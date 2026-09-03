# Custom Book Creator 1.1.2-hotfix

Maintenance update for MCreator 2026.2, supporting the NeoForge 1.21.1 and NeoForge 26.1.2 generators.

Author: **Superbroche**. See [RELEASE_NOTES.md](RELEASE_NOTES.md) for the detailed release overview.

## Main fixes

- Standardized author attribution and internal Java namespace to Superbroche, while retaining the stable plugin and saved-book identifiers.
- Safer drag-and-drop with rejection of invalid destinations and rollback on failure.
- Internal page buttons remain consistent after moving or deleting pages/categories.
- Image selections are no longer overwritten while the inspector refreshes its fields.
- Legacy and partially damaged book data is normalized without aborting workspace loading.
- Invalid item properties, texture references, and missing custom-model names receive safe defaults.
- Rich-text tags and supplementary Unicode characters behave consistently in preview and in game.
- Long non-ASCII text no longer exceeds Java's single-string constant limit.
- All categories remain accessible through sidebar pagination.
- Image and GIF imports validate dimensions and bound decoded resource usage.

## Validation

All **14/14 automated test programs passed** against the freshly packaged plugin. The generated item, screen, client events, and starting-book handlers also compiled against the real APIs for both supported NeoForge versions. Run `test.ps1` to rebuild the plugin and execute the regression programs. See [VALIDATION.md](VALIDATION.md) for exact coverage and limitations; a full `runClient` session was not completed in this validation.

## Installation

1. Back up your workspace and close MCreator.
2. Replace the old Custom Book Creator plugin ZIP, checking both the MCreator installation and user plugin directories. Keep only one copy installed.
3. Install `CustomBookCreator-MCreator2026.2-v1.1.2-hotfix.zip`.
4. Restart MCreator and regenerate the workspace code.

Do not install the sources ZIP. It is for development only and can cause a plugin-loading error if placed in a `plugins` directory.
