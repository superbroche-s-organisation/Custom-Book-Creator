# Changelog

## 1.1

- Added drag-and-drop reordering for categories and pages in the book tree.
- Added drag-and-drop page moves between categories while preserving page identifiers, content, and internal button targets.
- Added **Stop on the last page of each category** to Properties. When enabled, the right navigation arrow is hidden and cannot advance beyond the current category; when disabled, navigation continues to the next category as before.
- Fixed starting books for MCreator 2026.2 and NeoForge 26.1.2 by using the current persistent-data boolean API.
- Made starting-book grants server-only and store the once-per-player marker only after the book is handed to the player.
- Added the new editor labels and drag-and-drop guidance to every locale supported by MCreator 2026.2.
- Added regression coverage for starting-book generation, category-boundary navigation, drag-and-drop wiring, and localization.

## 1.0.1

- Fixed generated book screens failing to compile with MCreator 2026.2 and NeoForge 26.1.2.
- Updated the Minecraft `Util` import used by the NeoForge 26.1.2 screen template.
- Preserved the NeoForge 1.21.1 import so both supported generators use their correct version-specific API.
- Added a regression test covering external-link generation for both NeoForge versions.

## 1.0.0

- First stable release.
- Made English the source language for the complete plugin, including source messages and documentation.
- Externalized all editor labels, dialogs, errors, tooltips, preview text, and default UI names through MCreator's native localization system.
- Added every locale shipped with MCreator 2026.2.
- Added localized help pages for all supported locales.
- Added the optional starting-book behavior, granted once per player on their first world join.
- Kept the corrected `getElementFromGUI(): GeneratableElement` bridge.
- Kept GeckoLib Reborn and Nerdy's Player Animator Mixin compatibility.
- Included NeoForge 1.21.1 and NeoForge 26.1.2 generators.

## 2.8.1-development

- Fixed the missing Mixin configuration crash caused by an interaction between GeckoLib Reborn and Player Animator.

## 2.8.0-development

- Fixed the MCreator 2026.2 save bridge.
- Refined the Visuals and Properties pages and button styles.
