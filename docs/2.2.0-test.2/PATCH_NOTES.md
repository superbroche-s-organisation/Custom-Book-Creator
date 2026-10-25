# Custom Book Creator 2.2.0-test.2

Author: **Superbroche** | October 25, 2026

This development snapshot updates test.1 following interface feedback. Sources are pushed to the repository; no GitHub release is published.

- **Readable translations:** both the source and translation editor display white text on a dark background. Explicit dark colors, read-only states and formatting changes cannot make the text dark. Original color tags remain stored for in-game rendering; white is a preview setting.
- **Sound/music elements:** click **+ Sound/music** to add a normal page element. Select its workspace sound and choose **Page sound (on open)** or **Play / stop button**. Button mode has its own label, page position and size. Sound elements support duplicate, Delete and general Undo/Redo.
- **Migration:** test.1 page-level sound settings become a single corresponding sound element, preserving the resource and trigger. Reopening/normalizing the book does not duplicate the migrated element. Multiple automatic/button sounds remain independent.
- **Working help:** the four writing help controls now use the complete resource paths. Persistent key, input mode, default text and preset choices display their detailed help instead of a missing-entry message.
- **Category dropdown:** the sidebar uses a dropdown with wheel scrolling and a scrollbar for long category lists. The old `1 / 6` counter and pager arrows are removed. Selecting a category or pressing Escape closes the list.
- **Signing and controls:** **Sign book...** is centered at the bottom of the category sidebar. Writing-mode, next-choice, Save and Cancel buttons appear below the right-hand page, leaving the category pane available. Switching categories while editing confirms discarding a dirty draft and blocks navigation during a pending write.
- **Closed text-zone borders:** all four border edges keep a visible GUI pixel at fractional scaling. The frame is redrawn after the active input widget as well.
- **Old books:** after replacing the plugin ZIP, run **Regenerate code** and rebuild the mod. Existing books then receive the new interface and inventory-key closing. The user confirmed that regeneration resolves the old-book E-key case.

The compatibility matrix is unchanged: six profiles in the MCreator 2026.2 ZIP, Forge 1.20.1 plus NeoForge 1.21.1 in the 2024.4 ZIP, and Forge 1.17.1 in the 2022.2 ZIP. Matching workspace generators remain separate requirements. See the [guide](GUIDE.md) and [validation record](VALIDATION.md).
