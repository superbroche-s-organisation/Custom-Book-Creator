# Changelog — 2.1.0

Changes since 2.0.0. Author: **Superbroche**.

## Added

- INPUT writing-zone elements: FREE, PRESET and BOTH modes, label, stable field key, initial value, maximum length and predefined choices.
- Writing/signing settings with native procedure selectors, a fixed boolean permission fallback and a post-signature procedure hook.
- Per-ItemStack persisted state, individual field values and signature metadata.
- Native multiline game editor with selection, clipboard, choices, unsaved-change confirmation and permanent-signature confirmation.
- Server-issued editing sessions, replay prevention, expiry, request throttling, bounds checks and per-copy authorization.
- NeoForge 1.21.8, NeoForge 26.2 and Fabric 26.2 extensions.
- Dedicated MCreator 2024.4 / Forge 1.20.1 and MCreator 2022.2 / Forge 1.17.1 packages.
- Legacy editor-component bridges, native 2022.2 serialization checks and Forge 1.17.1 rendering/input adapters in the plugin's own namespace.
- Writing protocol/client tests, native MCreator procedure-resolution tests and modern Fabric starting-book ledger tests.

## Changed

- Writable books force a maximum stack size of one.
- Item use and client callbacks preserve the actual interaction hand.
- Minecraft 26.2 screen calls use Gui ownership; Fabric payload registration uses the current API names.
- NeoForge 1.21.8 and newer send client packets through ClientPacketDistributor.
- Release packaging includes only compiled profiles and a catalog matching the actual ZIP contents. Unvalidated adapters require an explicitly experimental development build.
- New editor writing labels are supplied in every locale catalog with English fallback, plus French translations.

## Fixed during validation

- Escaped Unicode causing otherwise valid 4,096-character books to exceed the wire limit.
- Persistent field names colliding with signature/state metadata.
- Permission procedures swapping the held book or overwriting unrelated custom data during authorization.
- Client send failures leaving a pending request or appearing to save optimistically.
- Duplicate signature effects on replay; failures in the post-signature hook no longer unlock a committed signature.
- Fabric 26.2 starting-book grants being marked delivered despite a failed inventory insertion and failed drop.
- French-locale number formatting producing invalid generated Java limits.
- Missing or incompatible procedure dependencies yielding unclear generated compilation errors.

## Scope

See [VALIDATION.md](VALIDATION.md) for exact profiles and test limits. The complete 1.17.1–26.2 all-loader range is deferred, not advertised as supported.
