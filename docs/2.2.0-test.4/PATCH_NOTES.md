# Custom Book Creator 2.2.0-test.4

Author: **Superbroche** | October 25, 2026

All book sound/music elements now use Minecraft's **Jukebox/Note Blocks** sound category (`SoundSource.RECORDS`). This applies to automatic page-entry playback and Play/Stop buttons. Their volume follows that slider instead of the Music slider, while Minecraft's normal master volume still applies.

Audio selection, page transitions and independent per-element playback are unchanged. Tooltips in all 30 editor catalogs and the [current guide](GUIDE.md) name the new volume setting.

## Targeted checks

- `PageSoundTest` executes the actual packaged playback methods and asserts that every automatic/button sound instance is created in the `RECORDS` channel. Existing independent playback, stop, replay, page change and unrelated-audio checks remain.
- `LocalizationIntegrityTest` checks the 30 catalogs and help paths; `TemplateRenderTest` renders each packaged profile.
- These focused checks are run against the MCreator 2026.2 and 2024.4 ZIPs. The legacy 2022.2 package repeats ABI/registration/serialization and twelve Java-template checks.
- `RECORDS` is verified in the installed NeoForge 1.21.1 and 26.1.2 APIs. Broader API/gameplay limitations remain as described in the [test.2 validation record](../2.2.0-test.2/VALIDATION.md). No interactive slider test is claimed for this snapshot.

## Update

Close MCreator and replace the previous plugin ZIP with the package for your MCreator version. Keep only one installed copy. Restart MCreator, use **Regenerate code**, then rebuild the mod. This regeneration moves previously created books to the new sound category too.

Compatibility is unchanged: MCreator 2026.2 (Forge 1.20.1, NeoForge 1.21.1/1.21.8/26.1.2/26.2 and Fabric 26.2), 2024.4 (Forge 1.20.1 and NeoForge 1.21.1), and 2022.2 (Forge 1.17.1). A matching workspace generator remains required. Sources are pushed to GitHub without creating a tag or release.
