AE2 WCWT
========

AE2 WCWT adds a Wireless Comprehensive Work Terminal for Applied Energistics 2 on
Minecraft Forge 1.20.1. It is built on AE2 and AE2WTLib, and focuses on putting
storage access, pattern encoding, manual crafting, pattern provider management,
tool slots, and several AE2 add-on workflows into one wireless terminal screen.

Current target
--------------

- Minecraft: 1.20.1
- Forge: 47.4.10 or newer in the Forge 47 range
- Java: 17
- Mod id: wcwt
- Main item id: wcwt:wireless_comprehensive_work_terminal
- License: MIT

Required dependencies
---------------------

- Applied Energistics 2 15.4.10 or newer
- AE2WTLib 15.2.2-forge or newer

Optional integrations
---------------------

- JEI: recipe transfer, pattern encoding transfer, preview highlights, and
  optional JEI bookmark priority for ambiguous ingredients.
- EMI: recipe transfer and pattern encoding support.
- Curios: Curios inventory panel in the terminal.
- ExtendedAE Plus: resonating/overload pattern tools and provider workflows.
- Advanced AE, Extended Pattern Provider, AE2 Import Export Card, Mekanism, and
  related tool/card items are handled where present.
- Cloth Config and Architectury are used at runtime for the Forge config screen.

Main features
-------------

- Wireless AE2 storage access with AE2 terminal sorting, view cells, pinned rows,
  active crafting job indicators, and type display filters.
- Top display filters for items, fluids, and other AE key types. The item/fluid
  filters use AE2's original type filter setting; the other-types filter is
  remembered client-side and excludes item/fluid entries locally.
- Favorite network items. Favorites can be shown first and can be used as an
  optional priority source when encoding patterns with multiple valid item
  candidates.
- Locked manual crafting grid. When locked, JEI/EMI crafting transfers target the
  manual 3x3 grid instead of the pattern encoding grid.
- Recipe pull handling for JEI/EMI. The terminal can pull available ingredients,
  request autocrafting for missing craftable inputs, and clear incompatible
  manual-grid contents back into the ME network before inserting new ingredients.
- Pattern encoding modes for crafting, processing, smithing, and stonecutting.
- Batch pattern tools, including pattern multipliers, item/fluid substitution,
  processing input merging, and output cycling.
- Pattern provider management panel with provider search, mappings, upload,
  machine UI opening, provider highlighting, display modes, and optional active
  refresh.
- Extra terminal panels for advanced coding, cell editing, Curios, card/tool
  boxes, toolkit slots, cosmetic armor, and resonating/overload pattern coding.
- Wireless terminal settings, magnet/trash actions, restock helpers, and hotkeys
  for common terminal actions.
- Client and server config entries exposed through the Forge mod config screen
  when the matching config file is loaded.

Configuration
-------------

Client config:

    config/wcwt-client.toml

Notable client options include:

- Enable or disable WCWT JEI/EMI recipe pull and encoding transfer handling.
- Auto-switch the manual workspace when transferring recipes.
- Prefer JEI bookmarks when pattern encoding has multiple item candidates.
- Prefer WCWT favorites when pattern encoding has multiple item candidates
  (disabled by default).
- Apply pattern multipliers to the current processing pattern editor.
- Expand the toolkit in the pattern management area.
- Remember UI state such as view-cell visibility and the other-types filter.

Server/world config:

    saves/<world>/serverconfig/wcwt-server.toml

Notable server options include:

- toolkitSlotCount: toolkit inventory size, minimum 11 and default 64.
- patternProviderActiveRefresh: when enabled, refresh the provider list while the
  pattern management area is visible. Leave disabled for one-shot/manual refresh
  behavior and steadier performance.

Building from source
--------------------

Use Java 17.

On Windows:

    .\gradlew.bat build

For a quick compile check:

    .\gradlew.bat compileJava

Generated jars are written under:

    build/libs/

Development notes
-----------------

- The project uses ForgeGradle, official Mojang mappings, and Mixin.
- The ExtendedAE Plus development API jar is expected at:

      libs/extendedae_plus-1.20.1-1.5.4-dev.jar

- JEI is currently present on the development runtime classpath to make recipe
  transfer testing easier.
- Performance debug logs are guarded behind system properties such as
  wcwt.debug.perf and are off by default.

Repository
----------

GitHub: https://github.com/lhy512103/AE2-WCWT
