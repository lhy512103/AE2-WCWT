---
navigation:
  parent: wcwt/index.md
  title: Storage and Manual Workspaces
  icon: ae2:crafting_terminal
  position: 20
---
# Storage and Manual Workspaces

## ME Storage

Items, fluids, and other AEKey types share the standard AE2 network-access model. Type buttons filter the visible content, while sorting, display mode, and row size follow AE2 terminal behavior.

- Pick block requests the targeted block from ME storage and selects the hotbar slot that actually received it.
- Favorites make entries easier to locate. With favorite preference enabled, WCWT favors a favorite candidate in JEI/EMI recipes with alternatives.
- Items with NBT or data components are distinguished exactly, including different Productive Bees bee spawn eggs sharing one item ID.

## Crafting Table Mode

1. Switch to the 3x3 crafting table.
2. Place ingredients manually, or transfer a recipe from JEI/EMI.
3. While a recipe preview is locked, the terminal checks availability in both the network and player inventory.
4. Take the output. Ingredients counted in the player inventory are physically moved into the crafting grid before use.

Holding Shift to pull a full recipe batch applies only to non-craftable ingredients. Craftable ingredients continue through AE2's crafting flow.

## Item Substitution

Manual-workspace item substitution is independent from the pattern-encoding button with the same name.

1. Enable **Item Swap** in crafting-table mode.
2. Transfer a recipe containing a tag or a JEI/EMI Ingredient with multiple candidates.
3. Craft normally. When the current candidate runs out, WCWT looks up other candidates only while substitution is enabled.
4. If the ME network or player inventory has another recipe-valid material, WCWT continues filling the grid with it.

Durability-bearing and component-bearing items still pass recipe validation; they are not replaced by a loose item-ID comparison. Turning substitution off also disables the extra candidate lookup.

## Fluid Substitution

Fluid substitution supports continuous recipes where a filled container participates and an empty container is returned, such as a water bucket returning an empty bucket.

1. Enable **Fluid Swap** in crafting-table mode.
2. Make sure the ME network stores the required fluid in AE2-accessible fluid storage.
3. Begin crafting with a filled container in the recipe.
4. When crafting returns the empty container, WCWT tries to extract the required fluid, refill it, and return it to the matching crafting slot.

The feature does not create containers or fluids. The network needs enough fluid and the container must expose standard filling behavior. Custom containers that do not expose compatible fluid handling may not be supported.

## Smithing, Anvil, and Stonecutting

After switching modes, wheel transfer and quick movement target the visible workspace rather than the hidden crafting grid.

- Smithing uses its template, base, and addition slots.
- Anvil mode accepts the input and material; type into the rename field before taking the result. While that field is focused, WCWT hotkeys do not consume typed keys.
- Stonecutting accepts an input and lets you select its output recipe.

These modes do not use the crafting table's item- or fluid-substitution logic.
