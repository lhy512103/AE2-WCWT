---
navigation:
  parent: wcwt/index.md
  title: Pattern Management
  icon: ae2:pattern_provider
  position: 40
---
# Pattern Management

Pattern management lists pattern providers accessible on the current ME network. It can locate providers, edit their slots, save cached patterns in order, and automatically upload a newly encoded pattern to a chosen destination.

## Prepare Provider Names

Mappings and automatic upload target the **provider group name** shown in the list. Give different jobs clear, distinct names such as “Enriching”, “Compressing”, and “Smelting”.

- Physical providers with exactly the same display name are treated as one group. WCWT tries each member until it finds a writable empty slot.
- Providers for different jobs should not share a name. Rename them, or explicitly select the intended provider before encoding.
- A provider must be online, loaded, and have a writable empty pattern slot.

## Find and Operate a Provider

1. Enter a provider name, mapping alias, or text accepted by the current search mode in **Provider Search**.
2. Choose the name/content search mode as needed, and switch display mode among all, visible, and non-full providers.
3. Select a provider row to inspect its slots. A valid selected provider also becomes the preferred target for the next automatic upload.
4. Use Highlight to mark its world position. This calls ExtendedAE's client highlight renderer and therefore requires ExtendedAE.
5. Use Open UI to open the machine or container on the provider's output side. WCWT implements this remote-opening path itself; ExtendedAE Plus is not required.

Highlighting can fail when ExtendedAE is absent or location data is unavailable. Opening can fail when the chunk is unloaded, facing changed, the output side has no menu, the dimension is unavailable, the network has rebuilt, or permission is denied.

## Save Cached Patterns

1. Put encoded patterns into the cache in the desired order.
2. Find and select the destination provider.
3. Confirm that its group has enough empty slots.
4. Press **Save cached patterns to this provider in order**.
5. WCWT inserts patterns in cache-slot order into available slots in that provider group.

That target belongs only to this action. Later Shift-moving from player inventory uses the provider currently shown and selected, not the previous destination. Provider slots use immediate client prediction followed by server confirmation without an extra full-provider scan for adjacent moves.

## What a Search Mapping Does

A mapping converts a recipe-type key recorded by JEI or EMI into the provider group name displayed by WCWT. Its direction is fixed:

- Upper **Provider Search**: source recipe-type key or alias.
- Lower **Mapping Edit**: exact destination provider group name, such as “Enriching”.

Mappings are stored in the current client's **config/extendedae_plus/recipe_type_names.json**. A dedicated server does not store this file for clients. Copy it when moving to another computer, game directory, or client instance. With ExtendedAE Plus installed, WCWT prefers its mapping API; without it, WCWT still reads and writes the same path through its fallback.

## Create a Mapping for the First Time

Disable automatic upload while discovering a key so a test pattern cannot reach the wrong provider.

1. Read the exact destination group name from the provider list, including spaces and symbols.
2. Open a processing recipe for the target machine in JEI or EMI and transfer it into WCWT's pattern encoder. WCWT records that recipe type's search key.
3. With the default “populate Provider Search while encoding” client option enabled, encode one test pattern and note the key placed in **Provider Search**. Upload is off, so the pattern remains local.
4. Re-enter that source key in **Provider Search**.
5. Enter the exact destination group name in **Mapping Edit**.
6. Press **Add Mapping**. On success, Provider Search changes to the resolved provider name and the list should narrow to the intended group.
7. Remove or recycle the test pattern, then enable automatic upload.

When the recipe-type key is already known, start at step 4. Crafting patterns use the built-in **crafting** key; processing patterns normally use the machine recipe-type key supplied by JEI or EMI.

## Edit, Delete, and Reload Mappings

### Change a destination

1. Re-enter the original source key in **Provider Search**. After adding, the field displays the resolved target, so the source key must be entered again.
2. Enter the new provider group name in **Mapping Edit**.
3. Press **Add Mapping** again. The same source key is updated to the new target.
4. Confirm that only the new destination group remains.

### Delete mappings

1. Enter the **destination provider display name** in Mapping Edit, not the source key.
2. Press **Delete Mapping**.
3. The result count is the number removed. Every source key pointing to that destination is deleted together.

### Reload an external edit

1. Save changes to **config/extendedae_plus/recipe_type_names.json**.
2. Return to the terminal and press **Reload Mapping**.
3. Re-enter a source key and verify the new destination. Keep the JSON valid.

**Cancel** only clears the two fields; it does not delete saved mappings.

## Automatically Upload a Specific Pattern

1. Confirm that the destination is online, correctly named, and has an empty slot. Select it when it should be the explicit preference.
2. Put a blank pattern in the encoder and choose the correct crafting or processing mode.
3. Enable **Automatic Upload**. This state is stored on the current terminal item.
4. For processing, transfer the **exact recipe about to be encoded** from JEI or EMI again. This refreshes the pending recipe-type key; do not rely on a different recipe transferred much earlier.
5. Verify inputs, outputs, substitutions, and quantities. To preflight, enter the source key in Provider Search and confirm that its mapping narrows the list correctly.
6. Press Encode. WCWT first uses a currently valid selected provider. Without one, it resolves the recipe key or Provider Search text through the mapping.
7. Upload proceeds only when one distinct provider group is identified. Physical providers sharing that name are tried in order for an empty slot.
8. Success displays a message, refreshes the slots, and focuses the inserted slot. The blank pattern is consumed and another can be refilled from ME storage.

Without a JEI or EMI transfer, WCWT uses Provider Search. A crafting pattern with empty search uses **crafting**. A freshly recorded recipe-viewer key takes priority over the text field, so transfer each new machine recipe again before encoding it.

The client option controlling whether encoding populates Provider Search is enabled by default. When disabled, ordinary encoding while upload is off does not rewrite the field. With upload enabled, WCWT still fills and resolves the data required by the upload workflow.

## If Upload Fails

Failure does not silently delete the pattern. It falls back to the editor or local cache according to the client option. Check:

1. The terminal is connected to the correct powered ME network.
2. The provider is online, loaded, and has an empty slot.
3. Mapping Edit used the exact group name shown in the list.
4. The source key belongs to the recipe just transferred from JEI or EMI.
5. Search does not match several differently named provider groups; rename, narrow the mapping, or explicitly select one.
6. The selected provider has not gone offline or disappeared from the list.
7. **Reload Mapping** was pressed after editing the JSON externally.
