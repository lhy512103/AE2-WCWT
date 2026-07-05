# Changelog

## v1.3.2

### English
1. Aligned the toolkit memory-slot button with the 1.20.1 layout and state styling, including separate off, hover, and on states where hover and on share the same style.
2. Ignored local scratch assets under `临时文件/`.
3. Fixed JEI/EMI recipe pull and preview matching so item data components/NBT are respected, preventing same-item-ID variants such as Productive Bees bee spawn eggs from being treated as interchangeable.
4. Limited Shift one-batch recipe pull to non-crafting recipes and updated the tooltip.
5. Added a fallback Curios-card recipe input so the card recipe appears in JEI/EMI when Curios is loaded.
6. Hid optional integration upgrade cards until their supporting mods are loaded, including Curios, Cosmetic Armor Reworked, AE2 Crystal Science, and AE2 Lightning Tech integrations.
7. Added AE2 Lightning Tech to the local runtime dependency set.
8. Switched the local runtime Curios dependency to CurseMaven file `curse.maven:curios-309927:6529130`.
9. Kept pattern-provider mapping edits client-local like ExtendedAE Plus, so dedicated servers no longer write `config/extendedae_plus/recipe_type_names.json` for those UI buttons and upload/search paths use the client-resolved mapping text.
10. Fixed a dedicated-server disconnect when putting an encoded pattern back into the pattern encoding output slot by updating derived pattern UI slots before broadcasting container contents.
11. Added OP-only `/wcwt config` commands for reading and changing the WCWT server options `toolkitSlotCount` and `patternProviderActiveRefresh`.
12. Added mirrored item and fluid substitution toggles under the manual crafting area's Store Items and Take Items buttons while using crafting pattern mode.
13. Implemented the manual crafting substitution toggles: repeated crafts can switch to another valid JEI/EMI item candidate when the current ingredient runs out, and fluid substitution can refill returned containers from ME fluids, such as refilling empty buckets from stored water.
14. Fixed manual crafting fluid substitution not refilling vanilla empty buckets after bucket recipes, so repeated crafts can consume stored ME fluids instead of leaving the crafting grid with an empty bucket.
15. Improved manual crafting item substitution with a server-side current-recipe ingredient fallback, allowing tag-based candidates such as alternate glass panes to be used when JEI/EMI did not send every candidate.
16. Protected AE2 view cell slots from shift/quick-move extraction so sorting mods no longer unload view cells from the view cell panel.
17. Added AllTheCompressed to the local runtime dependency set.
18. Added MEGA Cells bulk compression cutoff support to the advanced coding cell editor, including a cutoff button beside the cell upgrade slot and server-side cycling of the bulk cell cutoff.
19. Moved the MEGA Cells bulk compression cutoff button next to the cell editor copy-mode button, removed the separate upgrade-slot background, and scaled the cutoff item icon to the same 12x12 button size.
20. Restored the AE2 toolbar-style background for the MEGA Cells bulk compression cutoff button and shifted the four cell editor utility buttons left by 2 px.
21. Made the manual crafting area item and fluid substitution toggles independent from the pattern encoding area's substitution toggles.
22. Persisted the manual crafting area item and fluid substitution toggle states on the terminal item so they survive closing and reopening the terminal.

### 中文
1. 将工具包记忆槽位按钮的位置与状态样式对齐到 1.20.1 版本，支持关闭、悬停、开启三种状态，其中悬停与开启使用相同样式。
2. 忽略本地临时素材目录 `临时文件/`。
3. 修复 JEI/EMI 配方拉取与预览匹配未正确区分物品数据组件/NBT 的问题，避免 Productive Bees 蜜蜂刷怪蛋等同 ID 不同数据的物品变体被当成同一种物品。
4. 将按住 Shift 拉取一组配方用量限制为仅非合成配方生效，并同步更新 tooltip。
5. 为饰品栏卡补充稳定的配方输入，使 Curios 加载时该卡配方可在 JEI/EMI 中显示。
6. 需要额外模组支持的扩展升级卡现在只会在对应模组加载后显示，包括 Curios、Cosmetic Armor Reworked、AE2 Crystal Science 与 AE2 Lightning Tech 相关卡。
7. 将 AE2 Lightning Tech 加入本地运行时依赖。
8. 将本地运行时 Curios 依赖切换为 CurseMaven 文件 `curse.maven:curios-309927:6529130`。
9. 将样板供应器映射编辑对齐 ExtendedAE Plus 的客户端本地行为，专用服务器不再因这些 UI 按钮写入 `config/extendedae_plus/recipe_type_names.json`，上传与搜索路径改用客户端解析后的映射文本。
10. 修复专用服务器环境下，将已编码样板放回样板编码区默认输出槽时，容器内容包编码失败并断开连接的问题；现在会先更新样板相关派生 UI 槽，再广播容器内容。
11. 新增仅 OP 可用的 `/wcwt config` 指令，用于读取和修改 WCWT 服务端选项 `toolkitSlotCount` 与 `patternProviderActiveRefresh`。
12. 在手动合成区工作台模式下，将物品替换与流体替换按钮镜像到放入物品、取走物品按钮下方。
13. 实现手动合成区替换按钮的实际功能：连续合成时当前材料用完后可切换到 JEI/EMI 配方中的其它有效物品候选；流体替换可用 ME 网络流体重新填充返还容器，例如用网络里的水把空桶接回水桶。
14. 修复手动合成区流体替换在原版空桶上不生效的问题；水桶类配方合成后现在会用 ME 网络流体重新填充空桶，避免连续合成停在空桶状态。
15. 改进手动合成区物品替换：当 JEI/EMI 没有传完整候选时，服务端会在替换开启且同款材料抽取失败后，从当前配方 ingredient 兜底展开候选，使玻璃板等 tag 材料可继续替换。
16. 保护 AE2 显示元件槽位，禁止 Shift/整理模组快速移动从显示元件面板卸下显示元件。
17. 将 AllTheCompressed 加入本地运行时依赖。
18. 在高级编码扩展 UI 的元件编辑区加入 MEGA Cells 大宗压缩截断支持：满足条件时会在元件升级槽右侧显示截断按钮，并可在服务端循环切换大宗元件的截断物品。
19. 将 MEGA Cells 大宗压缩截断按钮移动到元件编辑区复制模式按钮右侧 14px 处，去掉独立升级槽背景，并把截断物品图标缩放到同样的 12x12 按钮尺寸。
20. 为 MEGA Cells 大宗压缩截断按钮恢复与复制模式按钮一致的 AE2 toolbar 小按钮底图，并将元件编辑区这一排四个功能按钮整体左移 2px。
21. 将手动合成区的物品替换与流体替换开关改为独立状态，不再与样板编码区的替换开关联动。
22. 将手动合成区物品替换与流体替换开关状态持久化到终端物品上，关闭并重新打开终端后仍会保留。

## v1.3.1

### English

1. Fixed JEI recipe transfer into the pattern encoding area replacing item variants with the same item ID but different data components/NBT, such as Productive Bees bee spawn eggs.
2. Fixed the comprehensive terminal offhand slot only accepting one item when placing a stackable item stack.
3. Restored the shapeless recipe for crafting the ME Comprehensive Work Terminal and moved the non-universal terminal upgrade recipe to a separate recipe ID.
4. Added toolkit memory slots for general toolkit slots: remembered empty toolkit slots are preferred when shift-moving matching tools into the toolkit, while the first 11 dedicated toolkit slots stay out of memory-slot mode.
5. Merged the two WCWT advancements into the same advancement tab and reused AE2's vanilla advancement background.

### 中文

1. 修复从JEI将配方拉取到样板编码区时，带数据组件/NBT的同ID物品变体可能被替换成其它变体的问题，例如Productive Bees不同蜂种刷怪蛋；
2. 修复在综合终端页面将可堆叠的一组物品放入副手栏时只会放入1个的问题；
3. 恢复ME综合工作终端的无序合成配方，并将无线综合非通用终端升级配方拆分为独立配方ID；
4. 新增工具包通用槽位记忆功能：Shift 左键将工具移入工具包时会优先进入已记忆且匹配的空槽位，并保持前11个工具包专用槽不参与记忆槽位模式；
5. 将 WCWT 的两个成就合并到同一标签下，并复用 AE2 原版成就背景图。

## v1.3.0

### English

#### New Features

1. Added an independent hotkey for opening the ME Comprehensive Work Terminal. The original AE wireless terminal open hotkey is no longer used for this terminal.
2. Added pattern search highlighting in the pattern management area.
3. Added Polymorph compatibility for the manual crafting area and pattern encoding area. Conflicting recipes can now be selected and encoded without installing Polymorphic Energistics.
4. Restored visibility of vanilla AE view cell slots and added a button to show or hide the view cell panel.
5. Added a default Curios slot for the ME Comprehensive Work Terminal. Other Curios slots can no longer accept this terminal.
6. Added an optional client config feature to prefer items favorited in the main storage area when encoding or pulling recipes with multiple candidate ingredients. Disabled by default.
7. Added the advancement "我嘞个雷霆大终端！" for obtaining the terminal for the first time.
8. Added six extension UI cards: Advanced Coding Card, Cosmetic Armor Card, Curios Card, Network Tool Slot Pack Card, Toolkit Card, and Resonating Overload Encoder Card. Extension UI buttons now only appear when the corresponding card is installed in the upgrade slots.
9. Added the Wireless Comprehensive Non-Universal Terminal:
   - Crafted by combining the ME Comprehensive Work Terminal with other wireless terminals.
   - Similar to the wireless universal terminal, but each terminal remains independent and only shares part of the switching logic.
   - Left-click the bottom-left terminal switch button to switch to the next terminal, right-click to switch to the previous terminal, and middle-click to return to the comprehensive work terminal.
   - Unlocks the advancement "什么？雷霆大终端已经满足不了你了嘛！" the first time it is crafted.
   - Hold the Wireless Comprehensive Non-Universal Terminal and Ctrl+Shift+right-click air to split out the merged terminals.

#### Fixes

1. Fixed server-side pattern provider mapping name changes not taking effect.
2. Fixed processing recipe encoding with multiple candidates not using existing network patterns.
3. Fixed EMI-only pattern encoding rendering errors caused by missing JEI classes.
4. Fixed identical pattern providers in the same pattern management provider list group not being grouped under a shared title.
5. Fixed space/shift item extraction from the network filling the offhand slot.
6. Fixed the advanced coding extension UI cell workbench upgrade scrollbar sliding outside the scroll track after inserting a storage cell.
7. Fixed the visible type configuration screen not being centered the first time it opened.
8. Fixed tool durability bars or energy bars rendering incompletely for the first 11 fixed toolkit slots when the toolkit was embedded in the pattern management area.
9. Fixed EMI pattern encoding outputs for fluids or chemicals always being encoded as 0.001B.
10. Fixed shift-left-clicking items into the terminal while out of power, unlinked, or disconnected inserting items into smithing/anvil workspace empty slots.
11. Fixed locked crafting grid processing recipe pulls not merging identical ingredients and leaving extra gaps.
12. Fixed encoded patterns not showing the encoding player information when ExtendedAE-Plus is installed.
13. Fixed shift-taking results from manual smithing mode not consuming ingredients.
14. Fixed shift-removing armor from armor slots sending it directly to the ME network, and fixed shift-inserting armor preferring armor slots incorrectly.
15. Fixed the bottom border of highlighted adjacent pattern provider list slots rendering incompletely.
16. Fixed the favorited item icon being offset by 1px.
17. Fixed inaccurate focusing after uploading patterns.
18. Fixed the quantum bridge card tooltip not showing compatibility with the comprehensive work terminal.


#### Improvements

1. Improved click interaction feel for slots in the pattern management provider list.
2. Added local runtime dependencies for Draconic Evolution and Productive Bees compatibility testing.

### 中文

#### 新增

1. 新增独立的开启ME综合工作终端快捷键，原打开无线终端快捷键不可用；
2. 新增样板管理区样板搜索高亮；
3. 手动合成区和样板编码区兼容“多态合成(Polymorph)”模组，无需安装Polymorphic Energistics模组也可编码冲突配方样板；
4. 修复原版AE显示元件槽位不可见的问题，新增显示/隐藏显示元件面板按钮；
5. 新增ME综合工作终端饰品栏默认槽位，其它饰品栏不可再放入本终端；
6. 新增多候选配方编码/拉取物品时优先使用主仓库区收藏物品功能，可在客户端配置中开关，默认关；
7. 新增首次获得终端时解锁成就“我嘞个雷霆大终端！”；
8. 新增6种扩展UI卡：高级编码卡、装饰盔甲卡、饰品栏卡、网络工具包卡槽包卡、工具包卡、谐振过载编码器卡，扩展UI按钮必须在升级槽内安装有对应卡时才会显示；
9. 新增无线综合非通用终端：
   - 使用ME综合工作终端与其它无线终端可合成；
   - 与无线通用终端功能类似，但不同终端互相独立，仅供用部分逻辑；
   - 在左下角切换终端按钮左键可切换下一终端，右键切换上一终端、中键跳转到综合工作终端；
   - 首次合成时可解锁成就“什么？雷霆大终端已经满足不了你了嘛！”；
   - 手持无线综合非通用终端ctrl+shift+右键空气可将已合并终端拆分；

#### 修复

1. 修复服务器修改样板映射名称后不生效的bug；
2. 修复处理配方多候选配方编码时不使用网络里已有样板的bug；
3. 修复EMI-only 环境下，由于 JEI 类缺失编码样板时提示渲染错误的bug；
4. 修复样板管理区供应器列表组内多个相同供应器标题不分组显示标题；
5. 修复空格/shift从网络取物品把副手填满的问题；
6. 修复高级编码扩展UI界面元件工作台放入存储元件后的升级槽滑块滑动时超出滑动条的bug；
7. 修复首次打开配置可见类型界面时界面位置未处于正中心的bug；
8. 修复工具包嵌入样板管理区时，放入前11个固定槽位的工具耐久条或者电量条显示不完整的bug；
9. 修复使用EMI编码样板且配方产物为流体或者化学品时，编码样板时数量恒为0.001B；
10. 修复终端在电源不足/未链接/断开链接时按shift+左键物品会进入锻造台区和铁砧区的物品空格栏的bug；
11. 修复合成网格锁定时，拉取处理配方物品的相同材料不自动合并到一起且存在多余空格的bug；
12. 修复在安装有ExtendedAE-Plus 模组时编码样板不显示样板编码玩家信息的bug；
13. 修复手动合成区锻造台模式按住shift取出物品时不消耗原材料的bug；
14. 修复按住shift从盔甲槽取下盔甲直接放入ME网络、shift放入盔甲优先放入盔甲槽的bug；
15. 修复样板管理区供应器列表上下相邻槽位间高亮框底部边框显示不完整的问题；
16. 修复收藏物品图标偏移1px的问题；
17. 修复上传样板时无法精准聚焦到上传样板位置的问题；
18. 修复量子桥卡tooltip不显示可用于综合工作终端的问题；


#### 优化

1. 优化样板管理区供应器列表槽位点击交互体验。
2. 新增用于Draconic Evolution和Productive Bees兼容性测试的本地运行时依赖。

## v1.2.1

### English

#### New Features

1. Added a hotkey to quickly toggle crafting grid lock while the terminal is open or when JEI/EMI is opened from the terminal.
2. Added an option to embed the toolkit into the pattern management area, configurable in the Wireless Terminal Settings screen.
3. Added a favorite items priority display feature to the left terminal button area. When enabled, favorited items are pinned to the top. Press `A` on an item in the terminal storage area to favorite it, and the key can be changed in key bindings.

#### Fixes

1. Fixed a bug where pulling processing recipe ingredients did not automatically return existing crafting grid items into the ME network first.
2. Fixed a bug where consuming a blank pattern could lead to a gray-screen ME view after reopening the terminal.
3. Fixed unreasonable priority behavior during recipe pulling.
4. Adjusted several texture layering issues.
5. Fixed the crafting-pin background not rendering completely.
6. Fixed terminal open/close hotkeys still triggering while typing in the two pattern management input fields.
7. Fixed a bug where some recipes could not be encoded into patterns.
8. Fixed a middle-click quantity adjustment conflict caused by sorting mod button injection in the pattern encoding area.
9. Fixed incorrect slot order when EMI pulled crafting recipe ingredients.
10. Fixed Ctrl + left click not auto-crafting missing ingredients under EMI.
11. Fixed the "store main hand item into the ME network" feature not working.
12. Fixed missing auto-restock behavior in manual anvil mode within the manual crafting area.
13. Fixed abnormal experience cost behavior, missing text prompt, and mismatched text color in manual anvil mode.
14. Fixed a bug where, with certain sorting mods installed, shift-moving items from the ME network could remove the upgrade card from the first upgrade slot.

#### Improvements

1. Improved auto-restock behavior in smithing and anvil modes to better align with vanilla AE restocking logic.

### 中文

#### 新增

1. 新增快捷键快速切换合成网格锁定状态功能，可在打开终端时或从终端打开 JEI/EMI 时使用；
2. 新增工具包可嵌入到样板管理区功能，可在“无线终端设置”里进行开关；
3. 在终端左侧按钮区新增“收藏物品优先显示功能”，开启后可置顶显示已收藏物品，对着仓库区物品按 `A` 键可收藏，可在按键绑定中更改；

#### 修复

1. 修复拉取处理配方物品时不自动将合成网格已有物品放入 ME 网络的 bug；
2. 修复消耗空白样板再次打开终端后，ME 网络出现灰屏的 bug；
3. 修复拉取配方时优先级不合理的问题；
4. 调整部分贴图显示层级不合理问题；
5. 修复合成置顶底图显示不完全的问题；
6. 修复样板管理区两个输入框输入时终端开启/关闭快捷键仍生效的 bug；
7. 修复部分配方无法编码样板的 bug；
8. 修复样板编码区鼠标中键调整数量时整理模组按钮注入的 bug；
9. 修复 EMI 拉取合成配方时顺序错乱的 bug；
10. 修复 EMI 环境下 `Ctrl + 左键` 无法自动合成缺失物品的 bug；
11. 修复“把主手物品放入 ME 网络”功能不生效的 bug；
12. 修复手动合成区铁砧模式下无法自动补料的 bug；
13. 修复手动合成区铁砧模式下经验消耗不正常、文字提示缺失、文本颜色不匹配的 bug；
14. 修复在安装某类整理模组时，`Shift` 快速移动网络里的物品会将升级槽中第一个槽位的升级卡卸下的 bug；

#### 优化

1. 优化锻造台模式和铁砧模式下的自动补料体验，与原版 AE 补料逻辑对齐。

## v1.2.0

### English

#### New Features

1. Added smithing table and anvil mode switching to the manual crafting workspace.
2. Added a pattern multiplier for processing pattern encoding, with a client config option to disable it.
3. Added a wireless terminal settings screen for toggling selected client-side features directly in-game.
4. Added an option to prioritize bookmarked ingredients during recipe pulling, configurable via client config.

#### Improvements

1. Improved the English localization to better fit the UI layout.
2. Added Traditional Chinese, Japanese, and Russian localization files (machine translated).
3. Added pinyin search compatibility for the pattern management provider search box.

#### Fixes

1. Fixed latency when placing a network tool into the toolkit.
2. Fixed latency when opening JEI multi-candidate and tag views from the terminal with this mod installed.
3. Fixed the inability to use middle-click quantity setting directly in the processing pattern encoding area.
4. Fixed shift-quick-move inserting items into toolkit and cosmetic armor slots even when those UIs were not open.
5. Fixed missing low-power warning popups when the terminal ran out of charge.
6. Fixed terminal hotkeys still triggering while the two pattern management text fields were focused.
7. Fixed clearing the pattern encoding area not restoring the blank pattern item.
8. Fixed JEI/EMI recipe pull previews incorrectly treating already stored network items as blue highlighted existing pattern items.
9. Fixed EMI crafting recipe pulls not following the actual visible input slot order of the current UI.
10. Fixed JEI and EMI shift-pull of one full set closing the terminal immediately and failing the transfer.

### 中文

#### 新功能

1. “手动合成区”新增切换锻造台和铁砧模式；
2. 新增样板倍增器，可对样板编码区处理样板进行倍增，可在客户端配置文件中关闭；
3. 新增“无线终端设置”界面，可直接开关部分客户端设置功能；
4. 增加拉取配方时优先使用书签栏物品的功能，可在配置文件中开关；

#### 优化

1. 优化英语语言文件，使其更贴合界面布局；
2. 新增繁体中文、日语和俄语文件（均为机翻）；
3. 样板管理区供应器搜索框兼容拼音搜索；

#### 修复

1. 修复将网络工具放入工具包时延迟卡顿问题；
2. 修复安装该模组时从终端打开 JEI 多候选/标签界面时延迟卡顿问题；
3. 修复样板编码区处理样板无法直接使用鼠标中键设置物品数量的 bug；
4. 修复 `Shift` 快速移动时，即使未打开对应界面，终端仍把物品塞进工具包槽和装饰盔甲槽的 bug；
5. 修复终端在电量不足时不自动弹出电源不足提示的 bug；
6. 修复样板管理两个输入框聚焦时终端快捷键仍生效的 bug；
7. 修复样板编码区点击清除后未还原成空白样板的 bug；
8. 修复 JEI/EMI 拉取配方样板时将网络已存在物品作为已有样板蓝色高亮的问题；
9. 修复 EMI 拉取合成配方物品时未按照当前界面里实际显示出的输入槽顺序来取的 bug；
10. 修复 JEI 和 EMI 按住 `Shift` 拉取一组物品时直接关闭终端界面且拉取失败的 bug；

## v1.1.1

### English

1. Fixed a bug where the resonating pattern cache still showed the sort button after closing the UI when `Inventory Tweaks - ReFoxed` was installed.
2. Fixed a bug where the resonating/overload encoder button was not hidden and textures were missing when `AE2 Lightning Tech` and `AE2 Crystal Science` were not installed.
3. Improved some stutter and latency issues a bit (hopefully helpful).
4. Toolkit now supports cross-terminal usage and data persistence for the same player.
5. Fixed a bug where shift-clicking curios would still send them directly into Curios even when the terminal Curios UI was not open. When the terminal Curios UI is open, shift-clicking a curio now prioritizes the player inventory first, and falls back to the terminal if full.
6. Fixed a bug where the toolkit hotkey could not open the toolkit when the terminal was equipped in Curios.
7. Fixed a bug where the pattern management provider display mode, show/hide slots toggle, and search mode toggle were reset after reopening the terminal.
8. Fixed a bug where the pattern upload enable button style did not match its text/state.
9. Added a client config option to choose whether failed pattern uploads return to the pattern editor slot or the pattern cache slot. Default is the pattern cache slot.
10. Moved the pattern management shift quick move option to client config.

### 中文

1. 修复安装 `Inventory Tweaks - ReFoxed` 模组时谐振样板缓存区在关闭界面后仍出现整理图标的 bug；
2. 修复未安装 `AE2 Lightning Tech`、`AE2 Crystal Science` 模组时过载谐振编码器按钮不隐藏，材质贴图缺失的 bug；
3. 优化了些许卡顿与延迟（可能有用？）；
4. 工具包支持同玩家跨终端使用与数据保存；
5. 修复未打开终端饰品栏时 `shift` 点击仍会直接放入饰品栏中的 bug，打开终端饰品栏界面 `shift` 点击饰品时优先放入玩家物品栏，已满则放入终端；
6. 修复当终端放入饰品栏时无法使用快捷键打开工具包的 bug；
7. 修复样板管理区的切换供应器显示模式、显示/隐藏槽位、切换搜索模式按钮重新打开终端后重置状态的 bug；
8. 修复启用上传样板功能按钮样式与文本不匹配的 bug；
9. 增加客户端可配置上传样板失败后回退到样板编辑槽或样板缓存区，默认回退到样板缓存区；
10. 将样板管理 `shift` 快速移动配置更改为客户端配置。
