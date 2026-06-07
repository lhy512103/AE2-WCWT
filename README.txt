AE2 WCWT
========

English
-------

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

Acknowledgements
----------------

- UI design, JSON layouts, and textures: xiaoleng5261
- Applied Energistics 2 and AE2WTLib provide the foundation this terminal builds
  on.

Repository
----------

GitHub: https://github.com/lhy512103/AE2-WCWT


中文
----

AE2 WCWT 为 Minecraft Forge 1.20.1 的 Applied Energistics 2 添加了一个无线综合工作终端。
它基于 AE2 和 AE2WTLib，将网络仓库访问、样板编码、手动合成、样板供应器管理、
工具槽位以及多个 AE2 附属模组相关流程整合到同一个无线终端界面中。

当前目标版本
------------

- Minecraft: 1.20.1
- Forge: 47.4.10 或 Forge 47 范围内的更新版本
- Java: 17
- 模组 ID: wcwt
- 主物品 ID: wcwt:wireless_comprehensive_work_terminal
- 许可证: MIT

必需依赖
--------

- Applied Energistics 2 15.4.10 或更新版本
- AE2WTLib 15.2.2-forge 或更新版本

可选联动
--------

- JEI: 配方转移、样板编码转移、预览高亮，以及多候选材料时可选的 JEI 书签优先。
- EMI: 配方转移与样板编码支持。
- Curios: 在终端中显示饰品栏面板。
- ExtendedAE Plus: 谐振/过载样板工具与供应器相关流程。
- Advanced AE、Extended Pattern Provider、AE2 Import Export Card、Mekanism 以及相关工具/卡片物品会在存在时进行兼容处理。
- Cloth Config 和 Architectury 用于运行时 Forge 配置界面。

主要功能
--------

- 无线访问 AE2 网络仓库，支持 AE2 终端排序、元件面板、置顶行、正在合成指示和类型显示过滤。
- 顶部提供物品、流体、其他 AE Key 类型显示过滤。物品/流体过滤调用 AE2 原版类型过滤设置；其他类型过滤在客户端记忆，并在本地排除物品和流体条目。
- 网络物品收藏。收藏物品可以优先显示，也可以作为样板编码多候选材料时的可选优先来源。
- 合成网格锁定。锁定时，JEI/EMI 合成配方会转移到手动 3x3 合成区，而不是样板编码网格。
- JEI/EMI 配方拉取处理。终端可以拉取已有材料，为缺失但可合成的输入发起自动合成，并在插入新材料前把不匹配的手动合成区物品放回 ME 网络。
- 支持合成、处理、锻造、切石等样板编码模式。
- 批量样板工具，包括样板倍增、物品/流体替换、处理输入合并和输出轮换。
- 样板供应器管理面板，支持供应器搜索、映射、上传、打开机器 UI、供应器高亮、显示模式和可选主动刷新。
- 扩展终端面板，包括高级编码、元件编辑、Curios、卡槽/工具箱、工具包、装饰盔甲、谐振/过载样板编码等。
- 无线终端设置、磁铁/垃圾桶操作、补货辅助以及常用终端动作热键。
- 当对应配置文件已加载时，可通过 Forge 模组配置界面查看和修改客户端/服务端配置项。

配置
----

客户端配置:

    config/wcwt-client.toml

常用客户端选项包括:

- 启用或禁用 WCWT 的 JEI/EMI 配方拉取和样板编码转移处理。
- 配方转移时自动切换手动工作区。
- 样板编码遇到多候选物品时优先使用 JEI 书签。
- 样板编码遇到多候选物品时优先使用 WCWT 收藏物品，默认关闭。
- 让样板倍增器同时作用于当前处理样板编辑区。
- 将工具包展开到样板管理区。
- 记忆元件面板可见状态、其他类型过滤等 UI 状态。

服务端/世界配置:

    saves/<world>/serverconfig/wcwt-server.toml

常用服务端选项包括:

- toolkitSlotCount: 工具包库存大小，最小 11，默认 64。
- patternProviderActiveRefresh: 开启后，当样板管理区可见时持续刷新供应器列表；关闭时使用单次/手动刷新，性能更稳。

从源码构建
----------

使用 Java 17。

Windows:

    .\gradlew.bat build

快速编译检查:

    .\gradlew.bat compileJava

生成的 jar 位于:

    build/libs/

开发说明
--------

- 项目使用 ForgeGradle、Mojang 官方映射和 Mixin。
- ExtendedAE Plus 开发 API jar 预期位于:

      libs/extendedae_plus-1.20.1-1.5.4-dev.jar

- 当前开发运行时会加载 JEI，方便测试配方转移。
- 性能调试日志由 wcwt.debug.perf 等系统属性控制，默认关闭。

致谢
----

- 界面 UI、JSON 布局与材质提供：xiaoleng5261
- 感谢 Applied Energistics 2 与 AE2WTLib 提供本终端构建所依赖的基础能力。

仓库
----

GitHub: https://github.com/lhy512103/AE2-WCWT
