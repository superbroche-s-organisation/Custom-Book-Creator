# V2.1 compatibility and validation

## Release matrix

| MCreator package | Generator | Actual generated-code check |
| --- | --- | --- |
| 2026.2 | Forge 1.20.1 | Forge 47.3.0, full Gradle compileJava |
| 2026.2 | NeoForge 1.21.1 | NeoForge 21.1.232, javac against patched game and loader libraries |
| 2026.2 | NeoForge 1.21.8 | NeoForge 21.8.53, full Gradle compileJava |
| 2026.2 | NeoForge 26.1.2 | NeoForge 26.1.2.95, javac against patched game and loader libraries |
| 2026.2 | NeoForge 26.2 | NeoForge 26.2.0.88, full Gradle compileJava |
| 2026.2 | Fabric 26.2 | Loader 0.19.3 / API 0.155.2+26.2, full Gradle compileJava |
| 2024.4 | Forge 1.20.1 | Native 2024.4 plugin build and Forge 47.3.0 compileJava |
| 2022.2 | Forge 1.17.1 | Native 2022.2 plugin build and Forge 37.1.1 compileJava |

A matching workspace generator must be installed and itself support the selected MCreator version. The legacy 1.17.1 test used [Spectrall368's generator, branch 2022.2](https://github.com/Spectrall368/Generator-1.17.1/tree/2022.2). The NeoForge 1.21.8 API selection follows [the 2026.2 generator](https://github.com/Spectrall368/Generator-1.21.8/tree/2026.2); Fabric 26.2 follows [CamCam5701's 2026.2 generator](https://github.com/CamCam5701/Fabric-26.2-Generator/tree/2026.2). These third-party generators are not bundled.

The 2022.2 plugin uses Java 17 bytecode; its Forge 1.17.1 output targets Java 16. The other plugin variants use Java 21 bytecode. Game code targets Java 17 for 1.20.1, Java 21 for 1.21.x and Java 25 for 26.x.

## Automated checks

- **21 test programs** passed against the packaged 2026.2 build.
- **20 applicable programs** passed against the 2024.4 package (no Fabric profile in that ZIP).
- **73 generated writing protocol/client checks** cover save/reopen, per-copy persistence, main/offhand separation, signed-copy transfer, identity, replay rejection, stale sessions, swapped items, changed permissions, rate limits, invalid input, escaped Unicode, failed sends, timeouts and world switches.
- Native MCreator procedure references resolve their actual dependency lists; missing procedures and unsupported dependencies produce explicit template errors.
- Native 2022.2 GUI/save bridge, mod-element registration, serialization roundtrip including image and procedure references, and 12 Java template renders passed.
- Existing rich-text, title design, media safety, drag-and-drop, workspace-reload and starting-book regressions remain in the suite.
- Fabric 26.2's real generated ledger template is exercised for initial join, reconnect, respawn, persistence across a simulated restart, full inventory, failed drop, retry and separate worlds.
- Representative generated Java includes optional item features, large Unicode text, title styling, media, writing, networking and starting-book code. Read-only template renders are checked not to reference optional writing classes.

## What these checks do not prove

**No interactive in-game, real multiplayer, or dedicated-server launch smoke test was performed.** Headless editor tests and deterministic game stand-ins are not screenshots or gameplay tests. Real API compilation checks signatures and type compatibility, not every runtime interaction. Imported resource/model behavior and every user procedure are not exhaustively covered.

There is no claim of complete 1.17.1–26.2 coverage or support for every loader at every version. NeoForge did not exist for the oldest Minecraft versions. Unlisted ports and the other experimental V2 1.20.x adapters are deferred and excluded from normal release ZIPs. Existing workspaces using those experimental profiles should not switch to the narrower V2.1 package yet.

## Reproduce plugin checks

Run from the repository root with the appropriate MCreator installations:

```powershell
./test.ps1 -MCreatorRoot <MCreator20262> -GeneratedSourcesDirectory build/v21-validation/generated
./test.ps1 -MCreatorRoot <MCreator20244> -TargetMCreator 2024.4 -GeneratedSourcesDirectory build/v21-validation/generated-2024
./test-mcreator2022.ps1 -MCreatorRoot <MCreator20222>
```

The scripts compile against each installation's actual libraries and test newly packaged bytecode. They do not modify the installed application or a user's workspace. Legacy UI icon fixtures are test-only and are not shipped inside the plugin. Source ZIPs do not contain Minecraft binaries, local Gradle caches or third-party generators.

For generated-game builds, use the corresponding generator/dependency versions in the matrix. Successful audit tool versions were Gradle 7.6.4 / ForgeGradle 5.1.77 (1.17.1), Gradle 8.8 / ForgeGradle 6.0.54 (1.20.1), ModDevGradle 2.0.141 (1.21.8), ModDevGradle 2.0.148 (26.2) and Fabric Loom 1.17.21 (26.2), with Gradle 9.6.1 for modern targets.

The test machine required a process-local Java Unix-domain socket fallback for Gradle. This was a local environment workaround, not a plugin requirement or a change to Windows security settings. Generated mods do not contain it.
