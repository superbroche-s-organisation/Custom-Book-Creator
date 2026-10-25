# Persistent field key

This is the permanent identifier of this writing zone, for example `field_12ab34`. The plugin creates it automatically and displays it as read-only.

Player input is stored on each individual book item using this key. Renaming the zone label, moving the zone or reordering pages keeps that association intact. Two copies of a book can hold different text.

Keep the key stable after distributing the book. Changing it in source data, or deleting a zone and creating a replacement, creates a different field: text already saved under the old key will not appear in the new zone. It does not rename a Minecraft translation key or a procedure variable. Each zone in a book must have its own key; the plugin resolves duplicates when saving.
