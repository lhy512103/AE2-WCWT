# Changelog

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
