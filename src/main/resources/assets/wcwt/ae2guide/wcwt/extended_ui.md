---
navigation:
  parent: wcwt/index.md
  title: Extension Panel Overview
  icon: wcwt:advanced_coding_card
  position: 50
item_ids:
  - wcwt:advanced_coding_card
---
# Extension Panel Overview

Extension panels are enabled by terminal extension cards. A card that depends on another mod appears in item lists and recipe viewers only while that mod is loaded. The matching panel button appears after the card is installed in the terminal.

## Panels and Cards

- **Advanced Coding Card**: advanced-pattern input directions, pattern copying, batch replacement, and the storage-cell workbench.
- **Resonating Overload Encoder Card**: displays available conversion areas from AE2 Lightning Tech or AE2 Crystal Science.
- **Curios Card**: requires Curios API.
- **Cosmetic Armor Card**: requires Cosmetic Armor Reworked.
- **Card Box Card**: stores supported upgrade cards together.
- **Toolkit Card**: stores tools and provides remembered slots.

Open extension UI from the right toolbar or a configured shortcut. Leaving a panel does not discard items in real slots. Ghost configuration slots store filters only and do not contain real items.

## Refresh and Performance

WCWT requests integration data only while the relevant panel or option is active, or when the player performs an operation. Alternative recipe candidates, cell configuration, and provider lists are not repeatedly scanned in unrelated screens. On a high-latency dedicated server, real slots still wait for server authority.

The following chapters describe advanced coding, special patterns, equipment, and the toolkit in detail.
