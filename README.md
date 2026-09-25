# AE2 WCWT

AE2 WCWT (Wireless Comprehensive Work Terminal) is an integrated wireless terminal for **Applied Energistics 2 / NeoForge 1.21.1**.
It is not just a wireless crafting terminal: it folds commonly used AE2 addon workflows into a single terminal item and a single UI.

Current version: `v1.3.10`

## Automated Publishing

The repository contains a GitHub Actions workflow at `.github/workflows/publish.yml`. Publishing
a GitHub Release builds the NeoForge jar from that release tag and publishes it to both CurseForge
and Modrinth. The release tag must match `mod_version` in `gradle.properties`; for example,
release tag `v1.3.11` requires `mod_version=1.3.11`.

Configure these repository settings before using the workflow:

- Repository variable `CURSEFORGE_PROJECT_ID`: the CurseForge project ID.
- Repository variable `MODRINTH_PROJECT_ID`: the Modrinth project ID or slug.
- Repository secret `CURSEFORGE_TOKEN`: a CurseForge API token with permission to publish files.
- Repository secret `MODRINTH_TOKEN`: a Modrinth personal access token with the `CREATE_VERSION` scope.

The workflow publishes the jar from `build/libs`, labels it as a NeoForge release for Minecraft
`1.21.1`, and uses the GitHub Release body as the release description on both platforms.

## Overview

- 18-column ME storage view
- Built-in pattern cache for temporary storage and batch operations
- Crafting, processing, smithing, and stonecutting pattern encoding
- Manual workspace with crafting-table, smithing-table, and anvil modes
- Pattern management (provider search, upload, highlight, open target UI)
- Extension UI system gated by upgrade cards
- Independent WCWT hotkey, plus per-panel hotkeys
- Optional-mod integrations that appear only when the related mod is loaded
- Built-in optional dark AE-UI resource pack
- Bilingual GuideME documentation

## Requirements

### Required

- NeoForge `1.21.1`
- Applied Energistics 2 `19.2.17+`
- AE2 Wireless Terminal Library `19.4.1+` (currently built against `19.5.1`)

AE2WTLib is a **hard dependency**. WCWT registers as a WTLib terminal and reuses its wireless settings, magnet card, quantum-bridge card, and Universal Terminal integration.

### Optional integrations

These mods are compile-time `compileOnly` / runtime optional. Missing them does not block launching the game; the related button, panel, or transfer path is hidden or skipped.

- AdvancedAE
- ExtendedAE
- ExtendedAE Plus (1.6.0+)
- Curios
- Cosmetic Armor Reworked
- EMI
- JEI
- AE2 JEI Integration
- MEGA Cells
- Polymorph
- AE2 Crystal Science
- AE2 Lightning Tech
- Extreme Sound Muffler
- Inventory Profiles Next / Inventory Tweaks ReFoxed
- AE2 Import/Export Card
- Applied Mekanistics
- Just Enough Characters
- NeoECOAEExtension

Notes:

- Extension UI buttons only appear when the matching upgrade card is installed **and** the supporting mod is present (where applicable).
- Optional integrations that use third-party internals go through `WcwtReflect`. If an upstream class/method disappears, WCWT logs a one-time warning and continues. Enable `-Dwcwt.debug.reflect=true` to print full stacks.

## Items

- **ME Comprehensive Work Terminal** — the main wireless terminal.
- **Six extension UI cards**, inserted into the terminal upgrade slots:
  - Advanced Coding Card
  - Cosmetic Armor Card (only visible with Cosmetic Armor Reworked)
  - Curios Card (only visible with Curios)
  - Network Tool Slot Pack Card
  - Toolkit Card
  - Resonating Overload Encoder Card (only visible with AE2 Crystal Science and/or AE2 Lightning Tech)
- Compatible AE / WTLib upgrade cards: Energy Card (×10), Quantum Bridge Card, Magnet Card, Import/Export Cards when that mod is loaded.

Crafting:

- The terminal recipe inherits data from all four input wireless terminals.
- Incompatible or excess upgrade cards are returned to the player inventory (or dropped if full).
- WCWT is also registered with AE2WTLib's Wireless Universal Terminal. Use the official WUT selector / hotkey to switch into WCWT.

## Main Terminal UI

### 1. Main repository view

- Displays items, fluids, and other ME key types
- Top-row filters: items / fluids / other types
- Mute button when Extreme Sound Muffler is installed
- Favorite-items toggle: press the bound key (default `A`) on a storage entry to pin it; optional “favorited items first” display
- View-cell panel can be shown or hidden

### 2. Top-right actions

- Wireless terminal settings: Pick Block, Craft If Missing, Restock, Magnet, Pickup To Me
- Magnet card menu (AE2WTLib)
- Trash menu
- Mute (Extreme Sound Muffler only)

### 3. Manual crafting area

Modes:

- Crafting table (3×3)
- Smithing table
- Anvil (with name field and XP cost)

JEI/EMI:

- `+` encodes the recipe into the pattern encoding area
- A separate hammer button pulls items into the manual 3×3 and then returns to the terminal

Crafting-table extras:

- Item substitution and fluid substitution (independent of pattern encoding mode)
- Polymorph compatibility for conflicting recipes

### 4. Pattern encoding area

Modes: crafting / processing / smithing / stonecutting.

Controls: encode, clear, merge identical processing inputs, substitution / fluid substitution, processing-output cycle, pattern multiplier (`x2` `x3` `x5` `=1` `/2` `/3` `/5`).

### 5. Pattern cache

Temporary storage used as the input source for advanced coding, batch upload, copy/replace, and resonating/overload conversion.

## Pattern Management

Aligned with ExtendedAE / ExtendedAE Plus:

- Provider search (pinyin-capable via Just Enough Characters)
- Search-mapping add / reload / delete
- Display modes: All / Visible / Not Full
- Search modes: input / output / input+output
- Show or compact empty provider slots
- Automatic upload toggle (stored on the terminal item)
- Upload cached patterns, open the provider's target UI, highlight the provider in-world
- Optional ExtendedAE Plus provider-select screen when several providers share a name
- Lightning Tech Tianshu Supercomputing Array crafting-pattern auto-upload
- Assembler-matrix upload uses ExtendedAE Plus public APIs (no reflection)

Shift quick extract/insert is a client option.

Server config `maxSyncedSlotsPerProvider` (default 1024) caps non-empty pattern slots synced per provider so large networks cannot disconnect the client.

## Extension UI

Right-side buttons. Each panel has its own hotkey.

| Panel | Requires | Purpose |
|---|---|---|
| Advanced Coding | Advanced Coding Card | Direction editor, copy, batch replace, cell workbench (partition / clear / copy mode / upgrades). MEGA Cells bulk-compression cutoff appears when applicable. |
| Cosmetic Armor | Cosmetic Armor Card + Cosmetic Armor Reworked | Cosmetic armor slots |
| Curios | Curios Card + Curios | Real Curios slots, scroll, render toggle. The currently open WCWT cannot be unequipped from Curios. |
| Card Box | Network Tool Slot Pack Card | AE / addon upgrade cards; persistent 3×3 upgrade inventory without a physical Network Tool |
| Toolkit | Toolkit Card | Tools, not upgrade cards. The toolkit has 18 general slots at minimum, configurable up to 640. When enabled separately, slots 0-8 and 9-17 are shown as left and right HUD hotbars, and the selected toolkit item behaves as the main-hand item. Toolkit hotkey can open the panel even when the main screen is closed. |
| Resonating Overload Encoder | Resonating Overload Encoder Card | Overload conversion (AE2 Lightning Tech) and resonating conversion (AE2 Crystal Science) |

## Hotkeys

Configurable in Controls:

- Open ME Comprehensive Work Terminal (independent of the generic AE wireless-terminal hotkey)
- Open Advanced Coding / Cosmetic Armor / Curios / Card Box / Toolkit / Resonating Overload Encoder
- Toggle crafting-grid lock (also works from JEI/EMI opened off the terminal)
- Favorite hovered storage item

The Toolkit hotkey works while the main terminal is closed. The other extension-UI hotkeys are intended for use while the terminal is open.

## Commands

OP-only (`permission level 2`):

```
/wcwt config toolkitSlotCount [18-640]
/wcwt config patternProviderActiveRefresh [true|false]
```

## Configuration

Client (`config/wcwt-client.toml`) covers recipe-transfer behaviour, pattern-upload fallback, JEI/EMI bookmark/favorite priority, toolkit embedding, empty-slot compacting, and similar UI options. Most of these also appear in the in-game Wireless Terminal Settings screen.

Server (`config/wcwt-server.toml`):

- `toolkitSlotCount` — 18–640, default 64
- `patternProviderActiveRefresh` — refresh provider lists while the area is open
- `maxSyncedSlotsPerProvider` — 64–8192, default 1024

## Localization and docs

- Languages: `en_us`, `zh_cn`, `zh_tw`, `ja_jp`, `ru_ru`
- GuideME pages under `assets/wcwt/ae2guide/` (English + Simplified Chinese)
- Built-in optional resource pack “WCWT Dark AE-UI Textures” (by fish_dan_); enable it from Resource Packs. GUI text colors are driven by `wcwt_palette.json`, so packs can restyle them.

## Debug JVM flags

All default to off:

| Flag | Effect |
|---|---|
| `-Dwcwt.debug.reflect=true` | Full stack traces for optional-integration reflection failures |
| `-Dwcwt.debug.patternUpload=true` | Pattern-upload path tracing |
| `-Dwcwt.debug.encode=true` | Pattern encoding tracing |
| `-Dwcwt.debug.advanced=true` | Advanced-coding tracing |
| `-Dwcwt.debug.magnet=true` | Magnet/restock tracing |
| `-Dwcwt.debug.toolkit=true` | Toolkit hotkey tracing |
| `-Dwcwt.debug.perf=true` | Slow menu/screen tick warnings |
| `-Dwcwt.debug.frameSync=true` | Per-frame sync counters |
| `-Dwcwt.debug.slotHit=true` | Pattern-management hitboxes |

## Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Optional mods are resolved from Maven / CurseMaven / Modrinth. There are no local `compileOnly files(...)` paths in the published build scripts. `localRuntime` entries in `dependencies.gradle` are only for the author's client/server run configurations.

## License

MIT License.

## Credits

- UI design, JSON layout, and texture assets: `xiaoleng5261`
- Dark AE-UI resource pack: `fish_dan_`

---

# AE2 WCWT 中文说明

AE2 WCWT（Wireless Comprehensive Work Terminal）是面向 **Applied Energistics 2 / NeoForge 1.21.1** 的综合型无线终端。
目标不是只做“无线合成终端”，而是把 AE2 生态里常用、常切换、常要开很多界面的功能，集中到一把终端里。

当前版本：`v1.3.10`

## 特性概览

- 18 列主仓库显示区
- 内置样板缓存区，便于批量编码、复制、替换、上传
- 同时支持合成 / 处理 / 锻造台 / 切石机样板
- 手动工作区：工作台、锻造台、铁砧三种模式
- 样板管理：供应器搜索、映射、上传、高亮、打开目标 UI
- 扩展 UI 由升级卡控制是否显示
- 独立的终端开启快捷键，以及各扩展面板快捷键
- 未安装的可选模组不会阻止进游戏，对应按钮/面板自动隐藏
- 内置可选暗色 AE-UI 资源包
- 中英文 GuideME 指南

## 依赖

### 必需

- NeoForge `1.21.1`
- Applied Energistics 2 `19.2.17+`
- AE2 Wireless Terminal Library `19.4.1+`（当前构建对照 `19.5.1`）

AE2WTLib **是硬依赖**，不是可选模组。WCWT 作为 WTLib 终端注册，复用其无线设置、磁力卡、量子桥卡和无线通用终端切换。

### 可选联动

编译期全部 `compileOnly`，运行时按 `ModList` 判断。缺模组不会阻断启动；对应功能隐藏或跳过。

AdvancedAE、ExtendedAE、ExtendedAE Plus、Curios、Cosmetic Armor Reworked、EMI、JEI、AE2 JEI Integration、MEGA Cells、Polymorph、AE2 Crystal Science、AE2 Lightning Tech、Extreme Sound Muffler、Inventory Profiles Next / Inventory Tweaks ReFoxed、AE2 Import/Export Card、Applied Mekanistics、Just Enough Characters、NeoECOAEExtension。

说明：

- 扩展 UI 按钮必须在升级槽装了对应卡、且相关模组已加载时才会显示。
- 访问第三方内部实现的代码统一走 `WcwtReflect`。上游改了类名/方法时只打一次 warn，界面和进游戏不受影响。排查时加 `-Dwcwt.debug.reflect=true`。

## 物品

- **ME 综合工作终端**
- **六种扩展 UI 卡**：高级编码卡、装饰盔甲卡、饰品栏卡、网络工具卡槽包卡、工具包卡、谐振过载编码器卡
- 兼容能源卡 ×10、量子桥卡、磁力卡；安装 ae2importexportcard 后还可装输入/输出卡

合成：

- 终端配方会继承四个输入无线终端中的数据
- 不兼容或超出容量的升级卡退回玩家物品栏，满则掉落
- 已接入 AE2WTLib 无线通用终端，使用其官方选择器与快捷键切换到 WCWT

## 主界面

### 1. 主仓库

物品 / 流体 / 其它类型过滤；安装 Extreme Sound Muffler 时显示静音按钮。对着仓库物品按收藏键（默认 `A`）可置顶。显示元件面板可显示或隐藏。

### 2. 右上角操作

无线终端设置（Pick Block / 缺少时合成 / Restock / Magnet / Pickup To Me）、磁卡菜单、垃圾桶、静音。

### 3. 手动合成区

工作台 / 锻造台 / 铁砧。JEI/EMI 的 `+` 编码到样板编码区；锤子按钮把物品拉入手动 3×3 后返回终端。工作台模式有独立的物品替换与流体替换（例如用 ME 网络里的水把空桶接回水桶）。兼容 Polymorph。

### 4. 样板编码区

合成 / 处理 / 锻造 / 切石。编码、清空、合并相同输入、替代/流体替代、处理输出切换、样板倍增。

### 5. 样板缓存区

高级编码、批量上传、复制/替换、谐振/过载转换都以这里为输入。

## 样板管理

对齐 ExtendedAE / ExtendedAE Plus：供应器搜索（可拼音）、映射增删重载、显示模式（全部 / 可见 / 未满）、搜索模式（输入 / 输出 / 输入+输出）、空槽压缩、自动上传开关（存在终端物品上）、上传、打开目标 UI、世界高亮。同名供应器可选打开 ExtendedAE Plus 选择界面。闪电科技天枢超算阵列的合成样板自动上传已接入。装配矩阵上传改走 ExtendedAE Plus 公开 API，不再反射。

Shift 快取/快放是客户端选项。服务端 `maxSyncedSlotsPerProvider`（默认 1024）限制单个供应器同步的非空槽位数，避免大网络把客户端打断。

## 扩展 UI

| 面板 | 条件 | 作用 |
|---|---|---|
| 高级编码 | 高级编码卡 | 输入方向、复制、批量替换、元件工作台；满足条件时显示 MEGA Cells 大宗压缩截断 |
| 装饰盔甲 | 装饰盔甲卡 + Cosmetic Armor Reworked | 装饰盔甲槽 |
| 饰品栏 | 饰品栏卡 + Curios | 真实 Curios 槽、滚动、渲染开关。当前打开的 WCWT 不能从饰品槽取下 |
| 卡槽箱 | 网络工具卡槽包卡 | 升级卡集中存放；无需实体网络工具即可提供可持久化的 3×3 升级槽 |
| 工具包 | 工具包卡 | 放工具不是放卡。工具包至少 18 个通用槽位，服务端可配 `18 ~ 640`，默认 64。独立开关开启后，第 0-8 格和第 9-17 格会作为左右扩展 HUD 快捷栏显示；快捷键可在未打开终端时唤起 |
| 谐振过载编码器 | 谐振过载编码器卡 | 过载转换（闪电科技）与谐振转换（晶体科学） |

## 快捷键

控制设置中可改：独立开启终端、各扩展 UI、收藏当前悬停物品。工具包快捷键在终端关闭时也可使用。

## 指令

仅 OP（权限等级 2）：

```
/wcwt config toolkitSlotCount [18-640]
/wcwt config patternProviderActiveRefresh [true|false]
```

## 配置

客户端 `config/wcwt-client.toml`：配方拉取、上传失败回退、书签/收藏优先、工具包嵌入样板管理区、空槽压缩等。大部分也可在游戏内“无线终端设置”里改。

服务端 `config/wcwt-server.toml`：

- `toolkitSlotCount`：18–640，默认 64
- `patternProviderActiveRefresh`：打开样板管理时是否主动刷新供应器列表
- `maxSyncedSlotsPerProvider`：64–8192，默认 1024

## 本地化与文档

- 语言：`en_us` / `zh_cn` / `zh_tw` / `ja_jp` / `ru_ru`
- GuideME：`assets/wcwt/ae2guide/`（中英）
- 内置可选资源包「WCWT 暗色 AE-UI 扩展材质」（作者 fish_dan_），在资源包菜单中启用。文字颜色由 `wcwt_palette.json` 定义，资源包可覆盖。

## 调试开关

默认全部关闭。反射排查用 `-Dwcwt.debug.reflect=true`；其它还有 `patternUpload` / `encode` / `advanced` / `magnet` / `toolkit` / `perf` / `frameSync` / `slotHit`。

## 构建

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

可选模组从 Maven / CurseMaven / Modrinth 解析。公开构建脚本里**没有**本地 `compileOnly files(...)` 路径。`dependencies.gradle` 的 `localRuntime` 仅用于作者本机的 client/server run。

## 许可证

MIT License。

## 致谢

- 界面 UI、JSON 布局与材质：`xiaoleng5261`
- 暗色 AE-UI 资源包：`fish_dan_`
