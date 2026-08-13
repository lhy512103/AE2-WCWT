# 1.20.1.9

## English

1. Integrated WCWT with AE2WTLib's Wireless Universal Terminal, using its official terminal hotkeys and cycle button.
2. Removed the Wireless Comprehensive Non-Universal Terminal. Legacy terminals embedded in WCWT are returned to the player's inventory; overflow is safely dropped.
3. Fixed WCWT clearing the terminal search field after reopening even when AE2's "Remember Last Search" option was enabled.


## 中文

1. 将 WCWT 接入 AE2WTLib 无线通用终端，并使用其官方终端快捷键和循环按钮。
2. 移除无线综合非通用终端，旧版合入的终端会返还至玩家背包；背包已满时会安全掉落。
3. 修复开启 AE2“记住上次进行的搜索”后，重新打开 WCWT 仍会清空终端搜索框的问题。


# 1.20.1.8

## English

1. Fixed Curios terminal pick-block synchronization and empty hotbar slot selection.
2. Fixed GTCEu programmed circuit priority and magnet filter slot matching.
3. Fixed duplicate provider titles not being grouped when multiple identical providers appear in the same pattern-management provider group.
4. Added configurable empty-slot compaction grouped by provider.
5. Added a configurable EAEP provider-selection screen when uploading patterns to multiple providers with the same name.
6. Optimized tool ingredient matching for recipe transfer.
7. Added the `maxSyncedSlotsPerProvider` server setting (default 1024) to cap synchronized non-empty pattern slots per provider and prevent oversized provider-list packets from disconnecting clients.
8. Fixed JEI/EMI transfers of recipes with extremely large quantities freezing the client due to per-item preview loops.
9. Capped each pulled ingredient at its runtime maximum stack size, including stack-size changes from other mods, and removed the fixed 64-item transfer limit.
10. Added generic JEI multiblock-structure pattern encoding for structure pages without real outputs, while retaining GTL hatch filtering.
11. Fixed the 3×3 upgrade toolbox still requiring a carried Network Tool after installing the Network Tool Slot Pack Card.
12. Added the `fillProviderSearchFromJeiBookmark` client option to control whether pressing F over a JEI ingredient or bookmark also fills the pattern-management provider search field.
13. Fixed pressing F over a bookmark automatically filling the JEI search field.


## 中文

1. 修复饰品栏终端中键取物设置同步和手上有物品时切换空快捷栏槽位的问题。
2. 修复GTCEu 编程电路优先级和磁力过滤槽位匹配问题。
3. 修复样板管理区供应器列表组内多个相同供应器标题不分组显示标题的问题。
4. 新增按供应器合并的空槽压缩显示，可在配置文件中开启。
5. 新增多个同名供应器上传样板时打开 EAEP 供应器选择界面，可在配置文件中开启。
6. 优化配方转移时的工具类材料匹配。
7. 新增服务端配置 `maxSyncedSlotsPerProvider`（默认 1024），限制每个供应器同步的非空样板槽位数量，避免过大的供应器列表数据包导致客户端断连。
8. 修复 JEI/EMI 转移超大合成量配方时按单个物品循环预览导致客户端卡死的问题。
9. 单项配方材料拉取数量改为不超过物品运行时最大堆叠数量，自动兼容其他模组对堆叠上限的修改，并移除固定 64 个的限制。
10. 新增通用 JEI 多方块结构样板编码：识别没有真实产物的多方块结构信息页，保留 GTL 仓室过滤。
11. 修复安装网络工具卡槽包卡后，仍需携带网络工具才显示 3×3 升级卡槽的问题。
12. 新增客户端配置 `fillProviderSearchFromJeiBookmark` ，用于控制对着 JEI 配料或书签按 F 键时是否同时填充样板管理区的供应器搜索框。
13. 修复对着书签 F 键时会自动填充到 JEI 搜索框的问题。


# 1.20.1.7-hotfix

1. Fixed recipe pull to the manual crafting area and pattern encoding area ignoring NBT variant bugs.


## 中文

1. 修复配方拉取至手动合成区和样板编码区忽略NBT变体的bug。



# 1.20.1.7

## English

1. Added a client option to keep GTCEu Programmed Circuits when filtering non-consumable inputs from transferred processing patterns.
2. Skipped WCWT JEI transfer analysis for GTCEu multiblock info pages to reduce extra stalls when opening their structure previews from the terminal.
3. Added toolkit memory slots.
4. Added dedicated upgrade cards for extended UI panels; each panel button now appears only when its matching card is installed in the terminal upgrade slots, and mod-dependent cards only appear when their required integrations are loaded.
5. Moved WCWT server-side options from per-world serverconfig storage to global config/wcwt-server.toml and added scrolling to the WCWT config screen.
6. Added OP-only chat commands for reading and changing WCWT server-side options: `/wcwt config toolkitSlotCount <value>` and `/wcwt config patternProviderActiveRefresh <true|false>`.
7. Fixed recipe pull and preview highlights overmatching NBT-specific item inputs, and limited Shift one-batch pulling to non-crafting recipes.
8. Added Polymorph conflict-recipe compatibility for the manual crafting area, manual smithing area, and crafting/smithing pattern encoding previews.
9. Fixed terminal text-field typing also triggering shortcuts.
10. Prevented quick-move fallback from inserting items into manual workspace smithing/anvil slots when ME storage is full.
11. Added item and fluid substitution buttons to the manual crafting area, plus post-crafting restocking with substitute materials.
12. Fixed the Advanced Coding screen not showing cell upgrade slots when a storage cell is inserted.
13. Adjusted several texture positions and sizes.


## 中文

1. 新增客户端选项：剔除 GTCEu 不消耗输入时可保留处理样板中的编程电路。
2. 跳过 GTCEu 多方块信息页的 WCWT JEI 转移分析，降低从终端打开结构预览时的额外卡顿。
3. 新增工具包记忆槽位功能。
4. 新增扩展 UI 专用升级卡；终端升级槽插入对应升级卡后，才会显示对应扩展 UI 按钮，需要额外模组支持的升级卡会在对应模组加载后才显示。
5. 将 WCWT 服务端选项从按世界保存的 serverconfig 改为全局 config/wcwt-server.toml，并为 WCWT 配置界面加入滚动条。
6. 新增仅 OP 可用的聊天框指令，用于读取和修改 WCWT 服务端选项：`/wcwt config toolkitSlotCount <数值>` 与 `/wcwt config patternProviderActiveRefresh <true|false>`。
7. 修复配方拉取与预览高亮对带 NBT 物品输入匹配过宽的问题，并将 Shift 拉取一组限制为仅对非合成配方生效。
8. 兼容 Polymorph 多态合成：手动合成区、手动锻造区，以及合成/锻造样板编码预览均可选择冲突配方。
9. 修复终端输入框输入时会触发快捷键的bug。
10. 修复 ME 存储已满时，快速移动回退逻辑会把物品塞进手动合成区锻造台/铁砧槽位的问题。
11. 手动合成区新增物品替换与流体替换按钮，以及合成后按替代材料自动补货逻辑。
12. 修复高级编码界面放入存储元件时，不显示元件升级卡槽的bug。
13. 修正部分贴图位置与大小。


# 1.20.1.6

## English

1. Optimized recipe transfer preview checks to reduce frame drops on JEI recipe pages with many animated renders.
2. Fixed a dedicated-server crash caused by restock amount syncing encoding item holders through a client-unsafe registry codec.
3. Fixed the terminal storage area appearing empty on the first reopen after blank patterns are consumed by pattern encoding.
4. Added the Wireless Comprehensive Non-Universal Terminal: WCWT can now embed compatible wireless terminals, switch between them from the toolbar, and split embedded terminals with Ctrl+Shift+right-click air.
5. Reduced JEI recipe page stalls in unlocked encoding mode by avoiding full terminal inventory scans during encoding preview highlights.
6. Merged WCWT's two advancements under the "Thunder Terminal" tab and reused AE2's original advancement background.

## 中文

1. 优化配方转移预览检测，降低打开包含大量动画渲染的 JEI 配方页时的帧率下降。
2. 修复补货数量同步通过客户端不安全的注册表编码传输物品 Holder，导致专用服务器崩溃的问题。
3. 修复样板编码消耗空白样板后，再次打开终端时主库存区首次显示为空、需要重开一次才恢复的问题。
4. 新增无线综合非通用终端：WCWT 现在可以合入兼容无线终端，在工具栏中切换，并可通过 Ctrl+Shift+右键空气拆出已合入终端。
5. 优化未锁定编码模式下的 JEI 配方预览高亮，避免每次切换配方页都全量扫描终端库存导致卡顿。
6. 将 WCWT 的两个成就合并到“雷霆大终端”标签下，并复用 AE2 原版成就背景图。


# 1.20.1.4

## English

1. Fixed a bug that prevented the mod from working on servers.
2. Fixed GTL pattern upload machine-name parsing so the correct machine name is resolved.
3. Added pattern highlighting in the pattern management provider list search.
4. Fixed middle-click pick block when the terminal is placed in a Curios slot. Thanks to Maiqilin for the idea.
5. Fixed network tool detection when the terminal is in a Curios slot and the network tool is stored in the toolkit.
6. Fixed pattern item count mismatches when recipe transfers pull more than two items.
7. Added automatic removal of GTCEu non-consumable inputs, with a client config toggle.
8. Added a dedicated WCWT terminal open hotkey. The vanilla AE wireless terminal hotkey no longer opens WCWT.
9. Added a fallback flow for pattern upload: when upload is enabled and Shift is held while clicking Encode Pattern, WCWT opens EAEP's original ProviderSelectScreen mapping/selection UI and uses EAEP's original upload logic. This is intended as a fallback; WCWT's upload logic is still recommended.

## 中文

1. 修复服务器无法使用的 bug。
2. 修复 GTL 上传样板时无法解析正确机器名称的问题。
3. 新增样板管理区供应器列表搜索样板高亮。
4. 修复当终端放入饰品栏时鼠标中键失效的 bug，感谢麦淇淋大佬提供的灵感。
5. 修复当终端放入饰品栏且网络工具放入工具包时网络工具无法被识别的 bug。
6. 修复当拉取物品数量大于 2 时的样板物品数量不匹配的 bug。
7. 新增自动剔除 GTCEu 不消耗输入，可在客户端配置里开关。
8. 新增 WCWT 终端开启快捷键，原版 AE 的无线终端快捷键不会再打开 WCWT。
9. 新增在“上传功能开启”并且按住 Shift 点击编写样板时，打开 EAEP 原版 ProviderSelectScreen 映射/选择界面，走原版 EAEP 上传逻辑。此功能作为兜底功能，仍推荐使用 WCWT 上传逻辑。
