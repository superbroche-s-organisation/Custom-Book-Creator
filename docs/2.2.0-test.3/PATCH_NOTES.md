# Custom Book Creator 2.2.0-test.3

Author: **Superbroche** | October 25, 2026

This snapshot removes the extra selected-category header crossed out in the interface feedback. The category area now displays **one directly visible scrollable list**, without a duplicate selected value, chevron or open/close step. The selected category is highlighted once in the list. Eleven entries fit above the centered signing action; mouse-wheel scrolling and the scrollbar keep longer lists accessible.

Page/category links still reveal the selected category automatically. Selecting an entry keeps the list available. Escape immediately follows normal book/editing behavior; there is no extra panel to dismiss. The sound elements, translation editor, help, signing placement, writing footer and border fixes from test.2 remain available.

## Checks

- Targeted `ScreenRuntimeLogicTest`, `PageInteractionTest` and `TemplateRenderTest` runs cover the packaged 2026.2 profiles and both 2024.4 profiles.
- The actual renderer is checked for a single occurrence of the selected label, direct entry hit regions, absence of the old selector/pager hit regions, eleven-row bounds, wheel scrolling and access to the last of 25 categories.
- Existing writing protection, category changes with dirty/pending drafts, navigation and read-only inventory-key template checks remain in the focused run.
- The native 2022.2 ABI/registration/serialization and twelve Java-template checks are repeated for the legacy ZIP.
- This change adds no Minecraft API calls. The broader tests and real API compilation from the [test.2 validation record](../2.2.0-test.2/VALIDATION.md) remain the baseline. No new interactive gameplay validation is claimed.

## Installation

Close MCreator, replace the previous plugin ZIP with the matching **2.2.0-test.3** package and keep only one installed copy. Restart MCreator, use **Regenerate code**, then rebuild the mod so existing books receive the updated interface. Sources and test-kit archives must not be installed as plugins.

The compatibility matrix is unchanged: MCreator 2026.2 (Forge 1.20.1, NeoForge 1.21.1/1.21.8/26.1.2/26.2, Fabric 26.2), 2024.4 (Forge 1.20.1, NeoForge 1.21.1) and 2022.2 (Forge 1.17.1). Matching generators are separate requirements. This update is pushed to GitHub sources without creating a tag or release.
