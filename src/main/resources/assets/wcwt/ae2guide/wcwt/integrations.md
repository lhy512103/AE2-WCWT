---
navigation:
  parent: wcwt/index.md
  title: Addon Integrations
  icon: ae2:advanced_card
  position: 90
---
# Addon Integrations

## Recipe Viewers

With JEI or EMI installed, recipes and ingredients can be transferred to the manual crafting table, pattern encoder, and advanced-coding ghost slots.

- Multi-candidate Ingredients drive manual item substitution.
- NBT and data components remain exact so special variants sharing an ID are not mixed.
- Special AEKey types use AE2's generic conversion path for processing patterns and advanced editing.
- Very large displayed recipe amounts use batched availability checks instead of repeatedly scanning the network one item at a time.

## Polymorph

With Polymorph installed, the manual workspace and pattern encoder can choose the intended output for conflicting recipes without requiring Polymorphic Energistics. Select the result before encoding so identical inputs do not encode the wrong recipe.

## Advanced AE

Advanced AE supplies the advanced processing-pattern format and per-input direction encoding. WCWT calls its encoder for conversion instead of creating a lookalike private format.

## MEGA Cells and AllTheCompressed

MEGA Cells supplies compatible bulk cells, compression upgrades, and the compression-cutoff integration. Content mods such as AllTheCompressed can supply multilevel compression chains. WCWT offers a cutoff only when MEGA Cells recognizes that chain.

## AE2 Lightning Tech

Supplies lightning AEKeys and overload patterns. Once loaded, the special encoder shows overload editing and allows strict or ID-only matching per input and output.

## AE2 Crystal Science

Supplies resonating patterns and their conversion. Once loaded, the panel shows 21 resonating storage slots and its batch conversion action.

## Mekanism

Mekanism chemicals can be used as special processing-pattern and replacement materials when the pack also exposes them as AE2-recognized storage keys. Mekanism by itself is not enough for WCWT to extract a chemical from ME storage without the corresponding AE2 chemical-storage bridge.

## Curios and Cosmetic Armor Reworked

These provide real accessory and cosmetic-armor slots respectively. Their cards and buttons stay hidden when the required mod is absent, without affecting the base terminal.

## ExtendedAE

ExtendedAE is an active integration, not only a runtime test mod. Its extended providers participate in WCWT's provider list when they expose AE2's pattern-container interface. The world Highlight action calls ExtendedAE's client highlight renderer, and related button artwork reuses its resources. Without ExtendedAE, base AE2 providers can still be managed, but ExtendedAE-specific providers and the world-highlight visual are unavailable.

## ExtendedAE Plus

WCWT integrates with ExtendedAE Plus recipe-viewer search data, recipe-type search keys, its fill-search hotkey, and mapping configuration APIs. When installed, WCWT prefers those APIs; otherwise WCWT can still read and write the same client mapping path. Provider listing, slot movement, automatic-upload target selection, and opening the provider's output-side target container are implemented by WCWT and do not require ExtendedAE Plus.
