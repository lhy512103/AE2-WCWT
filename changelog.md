# 1.20.1.6

## English

1. Optimized recipe transfer preview checks to reduce frame drops on JEI recipe pages with many animated renders.
2. Fixed a dedicated-server crash caused by restock amount syncing encoding item holders through a client-unsafe registry codec.
3. Fixed the terminal storage area appearing empty on the first reopen after blank patterns are consumed by pattern encoding.
4. Ported the 1.21.1 Wireless Comprehensive Non-Universal Terminal feature: WCWT can now embed compatible wireless terminals, switch between them from the toolbar, and split embedded terminals with Ctrl+Shift+right-click air.
5. Fixed the universal terminal switch button disappearing immediately after opening the base WCWT screen.

## 中文

1. 优化配方转移预览检测，降低打开包含大量动画渲染的 JEI 配方页时的帧率下降。
2. 修复补货数量同步通过客户端不安全的注册表编码传输物品 Holder，导致专用服务器崩溃的问题。
3. 修复样板编码消耗空白样板后，再次打开终端时主库存区首次显示为空、需要重开一次才恢复的问题。
4. 迁移 1.21.1 的无线综合非通用终端功能：WCWT 现在可以合入兼容无线终端，在工具栏中切换，并可通过 Ctrl+Shift+右键空气拆出已合入终端。
5. 修复打开 WCWT 主终端界面后，通用终端切换按钮会立刻消失的问题。

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
