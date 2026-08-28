# Custom Book Creator 1.1

Custom Book Creator 1.1 adds full drag-and-drop organization and safer category navigation, and fixes the reported starting-book crash on MCreator 2026.2.

## New features

- Drag categories to change their display order.
- Drag pages to reorder them inside a category.
- Drop a page into another category to move it while keeping its content, identifier, and internal links intact.
- A source category is automatically kept valid with a blank page if its only page is moved away.
- New Properties option: **Stop on the last page of each category**.
  - Enabled: the right arrow is hidden on the category's last page and cannot switch categories.
  - Disabled: the last page continues directly to the first page of the next category.

## Fixes

- Fixed the starting-book event for NeoForge 26.1.2 by using its current persistent-data boolean API.
- Starting books are now granted only to server players.
- The once-per-player marker is recorded after the book is handed to the player, preventing an incomplete grant from being marked as successful.
- Kept the version-specific implementation required by NeoForge 1.21.1.

## Compatibility

- MCreator 2026.2
- NeoForge 1.21.1
- NeoForge 26.1.2
- All locales available in MCreator 2026.2
