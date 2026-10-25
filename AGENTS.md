# Release rules

For every future Custom Book Creator GitHub release, upload exactly these four files, substituting the release version:

- `CustomBookCreator-MCreator2022.2-v<VERSION>.zip`
- `CustomBookCreator-MCreator2024.4-v<VERSION>.zip`
- `CustomBookCreator-MCreator2026.2-v<VERSION>.zip`
- `CustomBookCreator-v<VERSION>-sources.zip`

Do not attach changelogs, patch notes, guides, validation reports, website descriptions, upload instructions, checksum files, icons or MCreator upload kits as separate release assets. Keep documentation and website resources in the repository; put release notes in the release body. Supply extra local kits only when requested, outside the release upload directory.

Use `package-release.ps1` to prepare the upload directory. It must contain only the four ZIPs. Before publishing, check the GitHub API asset inventory against that exact list, even if another upload tool is used. GitHub's automatically generated Source code (zip/tar.gz) links are separate from uploaded assets.

When cleaning assets on an existing release, preserve the four published ZIPs, their contents, the release tag and its commit. Remove only the unwanted attachments. Do not replace published source archives merely to include later packaging-policy changes.

Keep the release credit as **Superbroche**. Publish a release only when the user requests publication; a request to push source changes alone does not authorize a new release. Later explicit user instructions take precedence over these defaults.
