---
navigation:
  parent: wcwt/index.md
  title: Pattern Encoding
  icon: ae2:blank_pattern
  position: 30
---
# Pattern Encoding

## Encoding Crafting Patterns

1. Open pattern encoding and choose crafting mode.
2. Insert a blank pattern, or insert an encoded pattern to edit it.
3. Arrange the recipe manually or transfer it from JEI/EMI.
4. With Polymorph installed, select the intended output when the inputs have conflicting recipes.
5. Verify the output preview and press Encode.

The result normally returns to the encoding slot. Depending on the active workflow, it can then enter the pattern cache or an enabled provider-upload flow. Recipe transfer preserves NBT and data components instead of loosely swapping special variants with the same item ID.

## Encoding Processing Patterns

1. Switch to processing mode.
2. Put materials consumed by the machine on the input side and expected products on the output side.
3. Use quantity editing and scaling to define the amount processed per operation.
4. Enable Merge Materials, item substitution, or fluid substitution when required. Merge Materials is stored on the current terminal item and remains set after closing and reopening it.
5. Encode the pattern and install it in the pattern provider for the target machine.

Processing patterns can contain items, fluids, and other AEKey types registered with AE2. Special ingredients dragged from JEI/EMI pass through WCWT's generic AE2 conversion, allowing types such as Mekanism chemicals and Lightning Tech lightning when their mods and storage support are loaded.

## Smithing and Stonecutting Patterns

Select smithing or stonecutting mode, transfer the recipe, and encode it. These remain their own recipe-pattern types and cannot be converted with Advanced AE processing-input direction controls.

## Encoding Substitution Toggles

The pattern encoder's **Item Swap** and **Fluid Swap** are options for its encoding workflow. They are independent from the manual crafting workspace, and both sets save their own state.

## Pattern Cache and SCALE

The pattern cache is the shared workspace for batch upload, advanced editing, copy and replacement, and special-pattern conversion.

- Put encoded patterns in the cache before selecting them in another panel.
- `SCALE` multiplies or divides quantities in cached patterns and writes each result back to the same cache position.
- Scaling cannot make an invalid amount into a valid recipe. Values beyond a pattern or AEKey limit are rejected or bounded.
- The cache is not a provider. Move completed patterns manually or upload them through pattern management.
