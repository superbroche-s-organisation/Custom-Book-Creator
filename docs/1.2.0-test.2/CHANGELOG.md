# 1.2.0-test.2 — local testing build

Author: **Superbroche**. No GitHub release or push.

## Changed

- Replaced numeric book generation with named Minecraft generation options; removed its help indicator.
- Replaced the raw-markup text area with a visual rich-text editor. Formatting is displayed directly while the existing internal storage format is retained.
- Added selection-based style toggles, typing styles, independent paragraph alignment, undo/redo and formatted clipboard support.
- Added full visual formatting for page titles, four-edge placement, rotated side titles, configurable title area, customizable separator bar, and a grid-aware movement handle.
- Preserved page-title appearance in copies and saves, keeping tree labels free of formatting markup.
- Removed authored-book localization and translation-key controls. Existing key tokens are retained literally; plugin-interface translations remain.

## Corrected during integration

- Style controls read the actual character next to the caret rather than relying on delayed Swing typing attributes.
- Formatting actions that exceed the text limit are rejected without cutting off closing tags or content.
- Renaming a page updates its visible title while retaining formatting.

The experimental 1.20.x compatibility limitations from test.1 remain. See [TESTING.md](TESTING.md) for installation, behavior and manual checks.
