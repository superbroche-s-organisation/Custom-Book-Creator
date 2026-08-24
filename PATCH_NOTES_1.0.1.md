# Custom Book Creator 1.0.1 — Patch Notes

## Fixed

- Fixed a build failure affecting books generated with MCreator 2026.2 and NeoForge 26.1.2.
- Updated the generated reader screen to import Minecraft's URL utility from its new 26.1.2 package.
- External links in book text once again compile correctly and continue to use Minecraft's standard confirmation warning before opening a browser.

## Compatibility

- NeoForge 1.21.1 keeps its original, version-correct URL utility import.
- NeoForge 26.1.2 now uses the new URL utility package.
- Existing Custom Book elements and saved book data remain compatible; this update does not change the element data format.

## Updating from 1.0.0

1. Replace the Custom Book Creator 1.0.0 plugin archive with version 1.0.1.
2. Restart MCreator.
3. Regenerate the workspace code, or open and save the affected Custom Book element.
4. Build the workspace again.
