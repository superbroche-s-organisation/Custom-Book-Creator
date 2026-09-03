# Custom Book Creator 1.1.2-hotfix

This is a maintenance build for MCreator 2026.2. Its sources are published on GitHub; no GitHub release has been created and existing tags remain unchanged.

## Main fixes

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

Run `test.ps1` to rebuild the plugin and execute all regression programs. The generated item, screen, client events, and starting-book handlers are also compiled against the real APIs for both supported NeoForge versions. See `VALIDATION.md` for exact coverage and limitations.

## Installation

1. Back up your workspace and close MCreator.
2. Replace the old Custom Book Creator plugin ZIP, checking both the MCreator installation and user plugin directories. Keep only one copy installed.
3. Install `CustomBookCreator-MCreator2026.2-v1.1.2-hotfix.zip`.
4. Restart MCreator and regenerate the workspace code.

Do not install the sources ZIP. It is for development only and can cause a plugin-loading error if placed in a `plugins` directory.
