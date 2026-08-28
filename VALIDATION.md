# Validation 1.1

The release is validated against the local MCreator 2026.2 installation:

- Java sources compile to Java 21 bytecode;
- the synthetic `getElementFromGUI(): GeneratableElement` bridge is present;
- `plugin.json` reports version `1.1`;
- English is the source language throughout the plugin;
- every MCreator 2026.2 locale has a complete property catalog and localized help tree;
- translation keys and MessageFormat placeholders match the English catalog;
- the starting-book option persists through save/load and generates server-only login/clone event handlers;
- NeoForge 26.1.2 starting books use `CompoundTag.getBooleanOr`, while NeoForge 1.21.1 keeps its version-correct `getBoolean` call;
- the starting-book received marker is written only after the item has been created and offered to the player;
- categories and pages can be reordered by drag and drop, and pages can move between categories without receiving new identifiers;
- the category-boundary option persists through save/load and controls right-arrow availability in both generated screen variants;
- both NeoForge generator resource sets are present;
- external-link code uses the version-correct Minecraft `Util` package in each NeoForge generator;
- both generators always produce a valid `@modid.mixins.json` resource;
- both release archives pass integrity checks.

The binary contract test is available in `tests/BinaryContractTest.java`. Template and v1.1 feature regressions are covered by `tests/StartingBookTemplateTest.java`, `tests/UrlOpeningTemplateTest.java`, and `tests/V11FeaturesTest.java`.
