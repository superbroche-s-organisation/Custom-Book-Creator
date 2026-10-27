# Custom Book Creator 2.2.1 — Validation

Author: **Superbroche** | October 27, 2026

## Cause and fix

The public 2.2.0 plugin's 2024.4 adapter included `writingPermission` and `onBookSigned` in `AggregatedValidationResult` even though these optional native procedure controls do not have validators. Their `getValidationStatus()` returns null. MCreator 2024.4's aggregate dereferences that result in `getGroupedValidationResults()`, throwing before either native Save action reaches `getElementFromGUI()`.

The adapter now includes each selector only when it actually has a validator. No procedure or permission becomes mandatory, and a configured validator can still report a blocking error. The fix is confined to `prepare-mcreator2024.ps1`; game templates, book storage format and the other MCreator adapters are unchanged.

## Evidence

- Reproduced the null-pointer exception with the public 2.2.0 archive and real MCreator 2024.4.52410 controls/native `validateAllPages()`, in an isolated installation.
- `SaveValidationTest` fails before the fix and passes afterward. It covers empty optional controls, a valid configured control, explicit errors in either control, and absent controls.
- All **26** applicable automated test programs pass on 2024.4, including template rendering, writing protocol checks, rich text, translations, drag/drop and workspace image reload.
- The native 2024.4 test clicks **Save mod element** and **Save and keep open** using MCreator's own listeners. It verifies GUI-to-model conversion, then generation, storage and reload for read-only and writable books on **Forge 1.20.1** and **NeoForge 1.21.1**.
- The native button fixture creates the real Interaction controls and native page/Save infrastructure, with empty panels for the other editor tabs. It stops after conversion to avoid desktop tabs, analytics and Gradle. The returned GUI model is passed through the real storage/generation/reload path separately.
- The new test sources also compile against 2026.2; the 2024-specific validation test correctly skips there. No 2026.2 or 2022.2 plugin behavior was changed.

Local evidence: `build/bug2024-repro.log`, `build/bug2024-native-baseline.log`, `build/bug2024-fixed-tests.log`, `build/bug2024-native.log` and `build/bug2024-tests2026-compile.log`.

## Installation and scope

Close MCreator, replace the installed **2024.4** Custom Book Creator ZIP with `CustomBookCreator-MCreator2024.4-v2.2.1.zip`, and keep only one installed copy. Restart MCreator, create a Custom Book and try both Save actions. This editor fix requires no game-code regeneration to restore saving.

The stable 2.2.1 release uses the same fix as the validated 2.2.1-test.1 snapshots. Existing V2.2.0 release files remain unchanged. No full interactive desktop/gameplay or dedicated-server test is claimed.

## MCreator 2026.2 / NeoForge 1.21.1

The separate `CustomBookCreator-MCreator2026.2-v2.2.1.zip` is now built and checked for this combination. The NeoForge 1.21.1 profile was already present in the 2026.2 source/package configuration; no new game templates were necessary. The 2024.4-specific save adapter is only applied to the 2024.4 ZIP.

- Verified the new archive manifest targets MCreator `2026002`, its catalog identifies NeoForge 1.21.1 / MCreator 2026.2, and its profile contains the item, screen and writing templates.
- Binary contract, validation-adapter selection and template rendering tests pass on MCreator 2026.2. This run contains three selected test programs; the full 2024.4 test run above is separate.
- The native 2026.2 plugin loader exposes Custom Book for the installed NeoForge 1.21.1 generator. Generation, storage and reload pass for both read-only and writable books.
- The newly generated NeoForge 1.21.1 Java sources compile against Minecraft 1.21.1 / NeoForge 21.1.232 libraries with Java 21 compatibility. The compiler reports two upstream event-bus deprecation warnings and no errors.

Evidence: `build/neoforge1211-2026-tests.log`, `build/neoforge1211-2026-native.log`, `build/neoforge1211-2026-api/compile.log` and `build/neoforge1211-2026-package.json`. No interactive gameplay test was performed for this check.

Install the **2026.2** ZIP in MCreator **2026.2**, together with a compatible **NeoForge 1.21.1 generator**. Keep only one Custom Book Creator ZIP installed. Restart MCreator and regenerate existing workspace code before building. Release downloads contain the three version-specific plugin ZIPs and the sources ZIP.

## Stable release checks

The final `2.2.1` ZIPs were rebuilt and checked before publication:

- MCreator 2026.2: all **27** automated programs pass, including all packaged-profile template rendering and 73 writing protocol checks.
- MCreator 2024.4: all **26** applicable automated programs pass, including the Save regression.
- MCreator 2022.2: legacy ABI, registration, JSON reload and **12** generated-template checks pass.
- Native integration with the final 2024.4 ZIP passes both Save actions and read-only/writable book generation/storage/reload for Forge 1.20.1 and NeoForge 1.21.1.
- Native integration with the final 2026.2 ZIP passes read-only/writable book generation/storage/reload for NeoForge 1.21.1 and NeoForge 26.1.2.
- Comparing archive payloads with V2.2.0 confirms that the 2022.2 and 2026.2 packages differ only in `plugin.json`; the 2024.4 package changes its manifest and the adapted `CustomBookGUI` class family. All game templates are byte-for-byte unchanged, so prior generated-code compilation evidence remains applicable.
- Release packaging checks the compatibility catalogs, plugin identifiers/credit, Java bytecode targets, payload contents, source inventory and the exact four ZIP names. Website descriptions, icon and notes are supplied from the repository rather than as extra GitHub assets.

Stable-build evidence: `build/release221-tests2026.log`, `build/release221-tests2024.log`, `build/release221-tests2022.log`, `build/release221-native2024.log`, `build/release221-native2026.log`, `build/release221-payload-comparison.json` and `build/release221-package.log`.
