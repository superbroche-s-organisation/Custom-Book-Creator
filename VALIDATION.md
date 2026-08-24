# Validation 1.0.0

The release is validated against the local MCreator 2026.2 installation:

- Java sources compile to Java 21 bytecode;
- the synthetic `getElementFromGUI(): GeneratableElement` bridge is present;
- `plugin.json` reports version `1.0.0`;
- English is the source language throughout the plugin;
- every MCreator 2026.2 locale has a complete property catalog and localized help tree;
- translation keys and MessageFormat placeholders match the English catalog;
- the starting-book option persists through save/load and generates login/clone event handlers;
- both NeoForge generator resource sets are present;
- both generators always produce a valid `@modid.mixins.json` resource;
- both release archives pass integrity checks.

The binary contract test is available in `tests/BinaryContractTest.java`.
