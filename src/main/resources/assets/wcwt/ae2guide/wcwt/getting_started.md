---
navigation:
  parent: wcwt/index.md
  title: Getting Started and Layout
  icon: wcwt:wireless_comprehensive_work_terminal
  position: 10
---
# Getting Started and Layout

## Main Screen Areas

- The left side is ME storage: search, sorting, insertion, extraction, favorites, and crafting requests.
- The center-bottom area is the player inventory and hotbar.
- The main work area on the right switches between manual crafting, pattern encoding, the pattern cache, and pattern management.
- Edge toolbars open settings, view cells, extension panels, and other shortcuts.

A button can depend on the current upgrade card, installed mods, and active work mode. If a panel is missing, first verify that its addon is loaded, then verify that the matching card is installed in the terminal extension slots.

## First Use

1. Open the terminal and verify that the left side shows the connected ME network contents.
2. Search by item name, mod name, or another expression supported by AE2 terminal search.
3. Left- or right-click network entries for normal insertion and extraction. Shift-click moves compatible stacks between the player inventory and the active area.
4. If an item has no stock but is craftable, select it to open AE2's crafting-amount flow when a CPU and pattern are available.
5. Use the mode buttons on the right to open a work area. A panel's return button returns to the main terminal without closing the complete screen.

## Wireless Connection Problems

If storage is empty or disconnected, check terminal power, wireless access-point range, both ends of a quantum bridge, security-terminal permissions, dimension restrictions, and AE2 wireless settings. WCWT supports networks connected only through a quantum bridge, but the bridge itself must be online.

## Saved Screen State

The client saves most display preferences, panel toggles, and manual-crafting substitution modes. Server options are controlled by the server; on a dedicated server, only operators can change the supported options with `/wcwt config`.
