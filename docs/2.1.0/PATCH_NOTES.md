# Custom Book Creator V2.1.0

By **Superbroche** — September 30, 2026.

## Write inside your books

Add **Writing zones** to a page and let players enter their own text, select predefined choices, or switch between both. Each book copy stores its own answers. Static instructions, images and buttons can be mixed with editable areas on the same page.

Use **Writing and signing** to select a boolean permission procedure, enable permanent signing and optionally run a procedure after the signature has been stored. This makes contracts, application forms, roleplay documents and interactive manuals possible. Signing records the server-side player identity and locks that copy's writing zones.

## Compatibility

Choose exactly one plugin ZIP for your MCreator installation:

- **2026.2:** Forge 1.20.1; NeoForge 1.21.1, **1.21.8**, 26.1.2, **26.2**; **Fabric 26.2**.
- **2024.4:** Forge 1.20.1.
- **2022.2:** Forge 1.17.1, using a dedicated legacy port.

An installed, compatible workspace generator is required. These are plugin extensions, not full generators. Not every Minecraft/loader combination between 1.17.1 and 26.2 is supported. Other experimental 1.20.x profiles from V2 are not included in the standard V2.1 ZIPs; keep V2 for those workspaces until their port is validated.

## Reliability improvements

- Server-side checks prevent unauthorized edits, replayed signatures, stale sessions and editing the wrong held copy.
- Offhand editing targets the offhand book correctly.
- Writable books cannot stack; field IDs survive reordering and are unique after copying.
- Unsaved drafts survive resizing; save status waits for server confirmation.
- Missing permission/signature procedures produce a clear generation error.
- Valid long Unicode text now fits the network limit after JSON escaping.
- Fabric 26.2 starting books remain retryable when neither inventory insertion nor dropping succeeds.
- Separate package manifests and native save-method checks for older MCreator versions.

## Before upgrading

Back up the workspace. Close MCreator and remove previous Custom Book Creator plugin copies, including installation-directory copies. Install only the matching **plugin ZIP**, restart MCreator, regenerate code and build.

Do not install the source archive or upload kit as a plugin. Do not install multiple variants together or open a newer workspace in an older MCreator version. Multiplayer requires the same regenerated mod on server and clients.

Validation includes automated behavior tests and real generated-code compilation. Interactive in-game, multiplayer and dedicated-server launch smoke tests were **not** performed for this release; this is not a claim of exhaustive bug-free coverage. See [VALIDATION.md](VALIDATION.md).
