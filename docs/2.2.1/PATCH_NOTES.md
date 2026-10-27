# Custom Book Creator V2.2.1

Author: **Superbroche** | October 27, 2026

## MCreator 2024.4 Save hotfix

Fixed **Save mod element** and **Save and keep open** doing nothing in the Custom Book editor on MCreator 2024.4, with both **Forge 1.20.1** and **NeoForge 1.21.1** workspaces.

The plugin tried to validate optional procedure controls that had no validator. MCreator 2024.4 then threw an exception before the book could be saved. The fix only aggregates controls with an actual validator; explicitly configured validation errors still block saving.

Regression coverage now includes the native Save listeners, GUI-to-model conversion, generation, storage and reload of read-only and writable books.

## MCreator 2026.2 / NeoForge 1.21.1

The **MCreator2026.2 ZIP includes NeoForge 1.21.1**. This previously included combination has been checked again for plugin registration, generation, storage/reload and compilation against Minecraft 1.21.1 / NeoForge 21.1.232. Install a matching generator compatible with MCreator 2026.2 separately.

## Downloads

Use the ZIP matching your MCreator version:

| Plugin ZIP | Included workspace profiles |
| --- | --- |
| `CustomBookCreator-MCreator2022.2-v2.2.1.zip` | Forge 1.17.1 |
| `CustomBookCreator-MCreator2024.4-v2.2.1.zip` | Forge 1.20.1; NeoForge 1.21.1 |
| `CustomBookCreator-MCreator2026.2-v2.2.1.zip` | Forge 1.20.1; NeoForge 1.21.1, 1.21.8, 26.1.2, 26.2; Fabric 26.2 |

`CustomBookCreator-v2.2.1-sources.zip` contains the source code and documentation. It is **not an installable plugin**. The release has only these four uploaded ZIPs; guides, validation and website resources remain in the repository.

## Update instructions

Close MCreator, replace the previous plugin ZIP and keep only one installed Custom Book Creator copy. Restart MCreator. The editor Save fix applies immediately; regenerate existing workspace code before rebuilding to ensure generated book code matches the installed plugin.

MCreator 2022.2/2026.2 plugin behavior and game templates are unchanged from V2.2.0. No complete interactive gameplay or dedicated-server launch test is claimed. See [validation](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.2.1/docs/2.2.1/VALIDATION.md) for the exact automated test scope and the [changelog](https://github.com/superbroche-s-organisation/Custom-Book-Creator/blob/v2.2.1/CHANGELOG.md) for release history.
