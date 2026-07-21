---
navigation:
  parent: wcwt/index.md
  title: Equipment, Card Box, and Toolkit
  icon: wcwt:toolkit_card
  position: 80
item_ids:
  - wcwt:toolkit_card
---
# Equipment, Card Box, and Toolkit

## Player Equipment

Armor, offhand, Curios, and cosmetic-armor panels display real player equipment slots rather than copies.

- Armor and offhand require no addon.
- Curios slots require Curios API and the Curios Card.
- Cosmetic armor requires Cosmetic Armor Reworked and the Cosmetic Armor Card.

These slots reject Shift quick-move and inventory-sorting automation. Insert or remove their contents manually so sorting mods cannot unload equipped items.

When the terminal itself is in a Curios slot and is hosting the currently open menu, that **exact terminal instance** cannot be removed from the accessory slot. Close the menu to remove it normally. Other identical terminals and unrelated accessories remain unlocked.

## Card Box

The card box stores upgrade cards supported by WCWT. Insert cards manually. A stored card remains usable only when its required addon is loaded. Removing the card that provides the current panel closes or hides that panel, while real slot contents remain synchronized by the server.

## Toolkit

The toolkit stores non-stackable tool items. Its first 11 slots are dedicated tool slots; later general slots can use slot memory.

1. Open the toolkit panel and manually place a tool in a general slot.
2. Click the memory button to enable memory editing, then click a general slot to remember it.
3. A remembered empty slot uses the active/hover style and records the matching tool type.
4. Later, Shift-moving a matching tool prefers that remembered empty slot.
5. Turning memory editing off returns the button to its disabled style without deleting saved slot memories.

The memory control has disabled, hovered, and enabled states; hovered and enabled share the highlighted appearance. The server option `toolkitSlotCount` controls the available memory/general slot count, and the client cannot exceed the server limit.

The toolkit does not duplicate items. If a target is occupied, an item is stackable, or it does not qualify as a tool, normal movement rules apply or insertion is rejected.
