---
navigation:
  parent: wcwt/index.md
  title: Overload and Resonating Patterns
  icon: wcwt:resonating_lightning_pattern_coding_card
  position: 70
item_ids:
  - wcwt:resonating_lightning_pattern_coding_card
---
# Overload and Resonating Patterns

The Resonating Overload Encoder Card displays different areas based on installed addons. Loading either supported addon makes the card available; the panel shows only conversions that are actually usable.

## Overload Pattern Editor

**Requires AE2 Lightning Tech (mod ID `ae2lt`).**

1. Put a decodable processing pattern in the main pattern cache.
2. Open the Resonating Overload Encoder and left-click that pattern in the cache.
3. The upper list displays every nonempty input and output.
4. Click each row's matching switch to choose strict matching or ID-only matching.
5. Every switch immediately submits an update. A normal AE2 processing pattern becomes an overload pattern in the same cache slot; an existing overload pattern is updated in place.

Strict matching compares complete components. ID-only matching ignores component differences. Keep strict matching for bee types, potions, enchantments, durability, or any other NBT/component-sensitive material so the machine does not accept the wrong variant.

Conversion requires no blank pattern and never moves the result to another slot. It does nothing when no pattern is selected, the pattern cannot be decoded, or the pattern is not a supported processing format.

## Resonating Pattern Converter

**Requires AE2 Crystal Science (mod ID `ae2cs`).**

1. Put processing patterns to convert into the pattern cache.
2. Make sure the panel's 21-slot resonating-pattern storage has free space.
3. Press **Convert to Resonating**.
4. The converter processes all supported cache slots in order, not only the currently selected pattern.
5. Every successful result enters the first empty resonating-storage slot and consumes one source pattern from its cache slot.

Unsupported patterns are skipped. Conversion stops with a message when resonating storage is full, and source patterns that were not successfully converted remain in place. Check free storage before using the button; it is intentionally a batch action.

## Difference Between the Conversions

- Overload editing handles only the selected pattern, overwrites it in place, and consumes no blank pattern.
- Resonating conversion processes the cache in batch, outputs into separate 21-slot storage, and consumes one source for every successful result.
- Both use the addon mod's own pattern format and encoder. WCWT provides the combined UI and transfers data safely.
