# AE2 WCWT

AE2 WCWT adds a **Wireless Comprehensive Work Terminal** for **Applied Energistics 2** on **Minecraft Forge 1.20.1**.

It is not just another wireless crafting terminal. It gathers storage access, pattern encoding, manual crafting, pattern provider management, toolkit slots, and several AE2 add-on workflows into one wireless terminal screen.

Current version: `20.0.0.2`

## Feature Overview

- 18-column ME storage view for a larger wireless item browsing area.
- AE2 terminal sorting, view cells, pinned rows, active crafting indicators, and display filters.
- Top display filters for items, fluids, and other AE key types.
- Favorite network items with optional priority for ambiguous pattern ingredients.
- Locked manual crafting grid for JEI/EMI recipe transfer into the 3x3 manual crafting area.
- Recipe pull handling that can clear incompatible manual-grid contents back into the ME network before inserting new ingredients.
- Pattern encoding for crafting, processing, smithing, and stonecutting patterns.
- Batch pattern tools, including multipliers, item/fluid substitution, processing input merging, and output cycling.
- Pattern provider management with search, mappings, upload, machine UI opening, provider highlighting, display modes, and optional active refresh.
- Extra panels for advanced coding, cell editing, Curios, card/tool boxes, toolkit slots, cosmetic armor, and resonating/overload pattern coding.
- Wireless settings, magnet/trash actions, restock helpers, and hotkeys for common terminal actions.
- Optional built-in 1.21-style resource pack with matching UI textures and animated terminal item texture.

## Dependencies And Compatibility

### Required

- Minecraft `1.20.1`
- Forge `47.4.10` or newer in the Forge 47 range
- Java `17`
- Applied Energistics 2 `15.4.10` or newer
- AE2WTLib `15.2.2-forge` or newer

### Optional Integrations

- JEI: recipe transfer, pattern encoding transfer, preview highlights, and optional JEI bookmark priority.
- EMI: recipe transfer and pattern encoding support.
- Curios: Curios inventory panel in the terminal.
- ExtendedAE Plus: resonating/overload pattern tools and provider workflows.
- Advanced AE, Extended Pattern Provider, AE2 Import Export Card, Mekanism, and related tool/card items are handled when present.
- Cloth Config and Architectury are used at runtime for the Forge config screen.

## Built-In Resource Pack

AE2 WCWT includes an optional built-in resource pack:

- `AE2 WCWT 1.21 Style`
- Replaces WCWT GUI textures with a 1.21-style UI.
- Adds matching AE2 1.21-style GUI resources used by this terminal.
- Overrides the wireless comprehensive work terminal item texture in resource-pack mode.
- Includes the animated terminal item texture from the 1.21.1 NeoForge reference version.
- Requires the `AE2_bright_assets_and_redraws` resource pack to be loaded together for the intended appearance.

This pack is optional. When it is not loaded, the mod keeps the default 1.20.1-style resources.

## Configuration

### Client Config

Path:

```text
config/wcwt-client.toml
```

Notable options:

- Enable or disable WCWT JEI/EMI recipe pull and encoding transfer handling.
- Auto-switch the manual workspace when transferring recipes.
- Prefer JEI bookmarks when pattern encoding has multiple item candidates.
- Prefer WCWT favorites when pattern encoding has multiple item candidates. Disabled by default.
- Apply pattern multipliers to the current processing pattern editor.
- Expand the toolkit in the pattern management area.
- Remember UI state such as view-cell visibility and the other-types filter.

### Server/World Config

Path:

```text
saves/<world>/serverconfig/wcwt-server.toml
```

Notable options:

- `toolkitSlotCount`: toolkit inventory size. Minimum `11`, default `64`.
- `patternProviderActiveRefresh`: refresh the provider list while the pattern management area is visible. Disable it for one-shot/manual refresh behavior and steadier performance.

## Build From Source

Use Java 17.

Windows:

```powershell
.\gradlew.bat build
```

Quick compile check:

```powershell
.\gradlew.bat compileJava
```

Generated jars are written under:

```text
build/libs/
```

## Development Notes

- The project uses ForgeGradle, official Mojang mappings, and Mixin.
- The ExtendedAE Plus development API jar is expected at:

```text
libs/extendedae_plus-1.20.1-1.5.4-dev.jar
```

- JEI is present on the development runtime classpath to make recipe transfer testing easier.
- Performance debug logs are guarded behind system properties such as `wcwt.debug.perf` and are off by default.

## Acknowledgements

- UI design, JSON layouts, and textures: **xiaoleng5261**
- Applied Energistics 2 and AE2WTLib provide the foundation this terminal builds on.

## Repository

GitHub: <https://github.com/lhy512103/AE2-WCWT>

---

# AE2 WCWT 中文说明

AE2 WCWT 为 **Minecraft Forge 1.20.1** 的 **Applied Energistics 2** 添加了一个 **无线综合工作终端**。

它的目标不只是做“无线合成终端”，而是把多个 AE2 生态附属模组中常用、常切换、需要开很多界面的功能，集中整合到一个无线终端里。

当前版本：`20.0.0.2`

## 特性概览

- 18 列主仓库显示区，提供更大的无线物品查看面积。
- 支持 AE2 终端排序、元件面板、置顶行、正在合成指示和类型显示过滤。
- 顶部提供物品、流体、其他 AE Key 类型显示过滤。
- 网络物品收藏，可优先显示，也可作为样板编码多候选材料时的可选优先来源。
- 支持锁定手动合成区，锁定时 JEI/EMI 合成配方会转移到手动 3x3 合成区。
- 支持 JEI/EMI 配方拉取，可在插入新材料前把不匹配的手动合成区物品放回 ME 网络。
- 支持合成样板、处理样板、锻造台样板、切石机样板编码。
- 支持批量样板工具，包括样板倍增、物品/流体替换、处理输入合并和输出轮换。
- 集成样板供应器管理，支持供应器搜索、映射、上传、打开机器 UI、供应器高亮、显示模式和可选主动刷新。
- 集成高级编码、元件编辑、Curios、卡槽/工具箱、工具包、装饰盔甲、谐振/过载样板编码等扩展 UI。
- 支持无线终端设置、磁铁/垃圾桶操作、补货辅助以及常用终端动作热键。
- 内置可选 1.21 风格资源包，包含匹配的 UI 贴图和带动画效果的终端物品贴图。

## 依赖与兼容

### 必需依赖

- Minecraft `1.20.1`
- Forge `47.4.10` 或 Forge 47 范围内的更新版本
- Java `17`
- Applied Energistics 2 `15.4.10` 或更新版本
- AE2WTLib `15.2.2-forge` 或更新版本

### 已集成/兼容的可选模组

- JEI：配方转移、样板编码转移、预览高亮，以及多候选材料时可选的 JEI 书签优先。
- EMI：配方转移与样板编码支持。
- Curios：在终端中显示饰品栏面板。
- ExtendedAE Plus：谐振/过载样板工具与供应器相关流程。
- Advanced AE、Extended Pattern Provider、AE2 Import Export Card、Mekanism 以及相关工具/卡片物品会在存在时进行兼容处理。
- Cloth Config 和 Architectury 用于运行时 Forge 配置界面。

## 内置资源包

AE2 WCWT 内置一个可选资源包：

- `AE2 WCWT 1.21 Style`
- 将 WCWT 界面替换为 1.21 风格 UI。
- 提供本终端使用到的 AE2 1.21 风格 GUI 资源。
- 在资源包模式下替换无线综合工作终端的物品贴图。
- 包含来自 1.21.1 NeoForge 参考版本的终端物品动画贴图。
- 需要搭配 `AE2_bright_assets_and_redraws` 资源包一起加载，才能获得预期显示效果。

该资源包是可选的。不启用时，模组保持默认 1.20.1 风格资源。

## 配置

### 客户端配置

路径：

```text
config/wcwt-client.toml
```

常用选项：

- 启用或禁用 WCWT 的 JEI/EMI 配方拉取和样板编码转移处理。
- 配方转移时自动切换手动工作区。
- 样板编码遇到多候选物品时优先使用 JEI 书签。
- 样板编码遇到多候选物品时优先使用 WCWT 收藏物品，默认关闭。
- 让样板倍增器同时作用于当前处理样板编辑区。
- 将工具包展开到样板管理区。
- 记忆元件面板可见状态、其他类型过滤等 UI 状态。

### 服务端/世界配置

路径：

```text
saves/<world>/serverconfig/wcwt-server.toml
```

常用选项：

- `toolkitSlotCount`：工具包库存大小，最小 `11`，默认 `64`。
- `patternProviderActiveRefresh`：开启后，当样板管理区可见时持续刷新供应器列表；关闭时使用单次/手动刷新，性能更稳。

## 从源码构建

使用 Java 17。

Windows：

```powershell
.\gradlew.bat build
```

快速编译检查：

```powershell
.\gradlew.bat compileJava
```

生成的 jar 位于：

```text
build/libs/
```

## 开发说明

- 项目使用 ForgeGradle、Mojang 官方映射和 Mixin。
- ExtendedAE Plus 开发 API jar 预期位于：

```text
libs/extendedae_plus-1.20.1-1.5.4-dev.jar
```

- 当前开发运行时会加载 JEI，方便测试配方转移。
- 性能调试日志由 `wcwt.debug.perf` 等系统属性控制，默认关闭。

## 致谢

- 界面 UI、JSON 布局与材质提供：**xiaoleng5261**
- 感谢 Applied Energistics 2 与 AE2WTLib 提供本终端构建所依赖的基础能力。

## 仓库

GitHub: <https://github.com/lhy512103/AE2-WCWT>
