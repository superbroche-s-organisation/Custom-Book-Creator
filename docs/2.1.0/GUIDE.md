# V2.1 writing and signing guide

The [V2 visual-editor guide](../2.0.0/GUIDE.md) still covers rich text, title design, images, buttons, navigation and drag-and-drop organization.

## Add an editable area

1. Open the book editor and add a **Writing zone** to a page.
2. Position and resize it like another page element.
3. Choose **FREE** for typed answers, **PRESET** for predefined choices, or **BOTH** to let the player switch.
4. Enter a label, an initial value and, when needed, one preset choice per line.
5. Set the maximum length. Field keys are generated automatically and remain stable across reordering.

A writing area stores plain text; its content is player input, not executable code or rich-text markup. Static rich text remains editable separately in the existing visual editor. Writable books have a maximum stack size of one so different answers cannot merge into the same stack.

## Decide who can write

In **Writing and signing**, select a boolean permission procedure or a constant permission value. Available dependencies are **entity, itemstack, world, x, y and z**. The server checks permission when opening a session and again before saving or signing. Do not replace the held item or modify its writing state in a permission procedure; such edits invalidate the session.

For a first-use choice between free writing and fixed terms, a procedure can store your chosen mode in custom data and permit editing accordingly. The built-in BOTH mode provides the simple free-text/preset switch; arbitrary contract outcomes remain your procedure's responsibility.

## Sign a copy

Enable signing if players should be able to finalize the book. After saving the desired answers, the player chooses **Sign**, enters a title and confirms. Signing locks every writing zone in that copy permanently. The recorded author and UUID come from the server, not from client-supplied identity.

An optional signature procedure runs once after committing the signed data. A failed procedure leaves the copy signed and is reported in the server log; it does not automatically retry or roll back other procedure effects. Administrative commands, creative-mode copying and procedures can still alter or duplicate items, so this is gameplay persistence, not cryptographic proof.

## Read answers from a procedure

For a book registry name `my_contract` and field key `terms`, use the held itemstack's custom data/NBT fields:

- `cbc_my_contract_field_terms`: the answer string.
- `cbc_my_contract_signature_signed`: boolean signature flag.
- `cbc_my_contract_signature_author`: signer name.
- `cbc_my_contract_signature_author_uuid`: signer UUID string.
- `cbc_my_contract_signature_title`: signature title.

The full internal JSON lives in `cbc_my_contract_state`. Do not edit it directly. Field keys are different from signature metadata. Copying a zone creates a new key; moving it keeps its key.

## Limits and persistence

Maximum 64 zones per book, 1,024 UTF-16 code units per zone, 4,096 in total, 16 choices per zone and 32 characters for the signature title. Supplementary Unicode symbols consume two UTF-16 units. Formatting controls, malformed Unicode, unknown fields, invalid choices and oversized messages are rejected.

Saved answers travel with that item copy through inventory movement, transfer and world saves. An unsaved draft exists only in the editor. Sessions expire after five minutes and invalidate on held-item replacement or saved-state changes. A timeout is not proof that a save failed: reopen the book to read the server's saved state.

Changing field keys, limits or preset choices after distributing a mod can make existing answers incompatible with the new definition. Back up worlds before such schema changes; no automatic migration of existing player-written data is provided in V2.1.
