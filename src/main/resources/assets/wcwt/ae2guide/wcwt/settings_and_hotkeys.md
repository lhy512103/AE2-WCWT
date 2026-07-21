---
navigation:
  parent: wcwt/index.md
  title: Settings, Hotkeys, and Troubleshooting
  icon: wcwt:wireless_comprehensive_work_terminal
  position: 100
---
# Settings, Hotkeys, and Troubleshooting

## Client Settings

Client options control display and local-player behavior such as pick block, crafting when stock is missing, restocking, magnetic pickup, pickup to ME or player, favorite candidate preference, manual substitution modes, and automatic provider-search population.

When **populate provider search after encoding** is disabled, ordinary encoding does not rewrite Provider Search. If pattern upload itself is enabled, the upload workflow can still populate the field to resolve its destination.

## Hotkeys

Search for WCWT in Minecraft Controls to bind the main terminal and extension-panel shortcuts. While the anvil rename field, search field, or mapping field has focus, typed text takes priority and does not trigger terminal action hotkeys.

If a shortcut conflicts, remove duplicate bindings in Controls. Do not rely on another AE2 wireless terminal's open key as a substitute for WCWT's dedicated key.

## Server Operator Commands

Only server operators can read or change these WCWT server options:

- `/wcwt config toolkitSlotCount <value>` sets the allowed toolkit general/memory slot count.
- `/wcwt config patternProviderActiveRefresh <true|false>` controls active pattern-provider refresh behavior.

An invalid value or insufficient permission does not modify server configuration. Dedicated-server options and client provider-mapping files are separate.

## Troubleshooting Order

1. **Button missing**: verify the addon is loaded and the required extension card is installed in the terminal.
2. **Pattern will not convert**: verify that it is a supported encoded processing pattern, not blank, crafting, smithing, or stonecutting.
3. **Special material cannot be dragged**: verify JEI/EMI, the source mod, and the matching AE2 storage type are loaded.
4. **Provider UI will not open**: check chunk loading, dimension, block facing, target container, and player permission.
5. **Fluid substitution will not refill**: check network fluid amount, whether the empty container returned to its original slot, and whether it supports standard fluid handling.
6. **A slot icon briefly rolls back**: the server rejected the slot action; check permission, reachability, network rebuilding, or target contents.
7. **A modpack differs from the test world**: inspect `latest.log` for a missing optional mod, version mismatch, resource-pack override, or another compatibility mod intercepting the behavior.

For dedicated-server problems, a correct-looking client screen does not prove the server accepted an operation. Check both client and server logs and treat server slot state as authoritative.
