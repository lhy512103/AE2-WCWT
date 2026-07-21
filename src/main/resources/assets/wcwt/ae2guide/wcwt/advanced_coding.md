---
navigation:
  parent: wcwt/index.md
  title: Advanced Coding and Cell Editing
  icon: wcwt:advanced_coding_card
  position: 60
item_ids:
  - wcwt:advanced_coding_card
---
# Advanced Coding and Cell Editing

The advanced coding panel contains four independent tools: Advanced AE input directions, pattern copy, batch material replacement, and a storage-cell workbench.

## Selecting a Pattern

1. Put encoded patterns in the main screen's pattern cache.
2. Open the advanced coding panel.
3. Left-click a cached pattern. This selects the slot without putting the pattern on the cursor.
4. The panel reads the selected pattern. Selecting another cache slot changes the direction editor and copy source.

Blank, undecodable, or unsupported patterns do not expose an action for the current tool.

## Editing Input and Output Directions

**Requires Advanced AE.** This feature integrates with Advanced AE's advanced processing-pattern format.

1. Select a normal AE2 processing pattern or an existing Advanced AE advanced processing pattern.
2. The panel creates a row for every input AEKey. The control on the right shows the currently allowed input side.
3. Click the direction control to cycle through Any, North, East, South, West, Up, and Down.
4. On the first successful change to a normal processing pattern, WCWT calls the Advanced AE encoder and converts it into an advanced processing pattern.
5. The converted pattern is written directly back to its original cache slot. It consumes no blank pattern and creates no second copy.

Any means that the input is not side-restricted. The direction is interpreted by machines or providers supporting this format; WCWT does not rotate a world block.

Crafting, smithing, and stonecutting patterns are not processing patterns and cannot be converted by this control. Without Advanced AE, the direction conversion action is unavailable.

## Copy Button

1. Select the encoded source pattern in the cache.
2. Supply one blank pattern to the copy input. When network refill is allowed and a blank pattern is available, the panel tries to fill it automatically.
3. Press Copy.
4. The copied pattern enters the real copy-result slot. Completely identical copies can stack there.

Copy reads only the currently selected pattern. It does not modify the source or copy the complete cache. A full output, missing blank pattern, or undecodable source prevents the action.

## Batch Replace Button

The two replacement slots are ghost filters and do not consume the real material on the cursor.

1. Drag the material to find into the left ghost slot.
2. Drag the replacement into the right ghost slot. Leave the right slot empty to remove the material from patterns.
3. Press Replace.
4. WCWT scans every decodable pattern in the pattern cache and writes successfully re-encoded results back to their original slots.

Processing patterns support items, fluids, and other generic AEKeys. Crafting patterns require item candidates and must still validate against their original recipe and output after replacement. Patterns that cannot be re-encoded or would become invalid are skipped instead of being forcibly corrupted.

Special types such as Mekanism chemicals and Lightning Tech lightning can be dragged from JEI/EMI into these ghost slots when their mods register compatible AEKey or GenericStack conversion.

## Storage-Cell Workbench

1. Put a real storage cell in the cell slot.
2. Upgrade slots on the right follow the number supported by that cell; insert compression cards or other supported upgrades there.
3. Click or drag JEI/EMI ingredients into the scrollable 3x6 configuration grid. These are ghost partition filters and do not consume items.
4. **Partition Storage** fills configuration from types currently stored in the cell. **Clear** removes all configuration entries.
5. The copy-mode button controls whether the edited configuration remains when the cell is removed, following AE2 cell-workbench copy-mode behavior.

## MEGA Cells Bulk Compression Cutoff

**Requires MEGA Cells.** The cutoff button appears only when all conditions are met: a compatible bulk cell is inserted, compression is enabled, required compression upgrades are installed, and the selected material has a recognized compression chain.

The button is to the right of the copy-mode control. Left- and right-click cycle available cutoff levels in opposite directions, for example between cobblestone and multiblocked cobblestone supplied by AllTheCompressed. The setting applies to the current cell integration only and does not scan or rewrite the complete ME network.

If the button is absent, verify the cell type, upgrades, recognized compression chain, and that MEGA Cells is loaded. Installing AllTheCompressed alone is not enough.
