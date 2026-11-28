# Changelog

## v26.1.2-0.0.1

### English

1. Changed: Migrated the mod to Minecraft 26.1.2 and NeoForge 26.1.2.129 on Java 25 and Gradle 9.2.1, updating GUI rendering, mixins, recipes, item model definitions, tests, and 26.1.2 dependencies (AE2, AE2WTLib, JEI, Curios, and optional integrations), while removing EMI support and safely stubbing optional mods without 26.1.2 releases.
2. Fixed: Clientbound pattern provider packets hand off to a client-only handler class instead of using `@OnlyIn`, so NeoForge 26.1.2 no longer reports a loading issue for the mod.
3. Changed: README, the publish workflow and AGENTS.md now describe Minecraft 26.1.2, NeoForge 26.1.2 and version 26.1.2-0.0.1.
4. Changed: Added .gitattributes so text files use LF line endings and Windows batch scripts use CRLF.
5. Fixed: GUI texture rendering now uses the correct blit overload with RenderPipelines.GUI_TEXTURED, restoring pattern encoding area backgrounds, manual workspace backgrounds, pattern management backgrounds, and panel textures that were missing due to the Minecraft 26.1.2 GuiGraphicsExtractor API change.
6. Fixed: WTLib magnet and trash button icons now load from the correct ae2wtlib_api namespace path (ae2wtlib_api:textures/guis/icons.png), fixing missing icons on the top bar.
7. Fixed: Pattern management search-mode and highlight buttons no longer depend on ExtendedAE's texture; replaced with our own wcwt_pattern_icons.png. Added missing lang keys (search_mode.output/input/input_output, highlight.tooltip, show_slots, hide_slots, curios.toggle) to all five lang files so tooltips now show translated text instead of raw keys.
8. Fixed: Advanced coding card recipes load only when their optional ingredients are registered.
9. Fixed: JEI waits for the terminal screen to initialize before it reads the screen dimensions.
10. Added: Brought in IUCE's development dependencies with updated coordinates and separate profiles for optional integrations.
11. Fixed: The key bindings screen shows the mod name for the terminal shortcuts.
12. Fixed: Installed expansion panels can be opened from their buttons.
13. Fixed: Pattern management slots show pattern icons and hidden-slot counts again and use the standard container slot highlight.

### 中文

1. 新增：将模组迁移至 Minecraft 26.1.2 与 NeoForge 26.1.2.129（Java 25 与 Gradle 9.2.1），完成界面渲染、混入、配方、物品模型定义、单元测试及核心依赖升级，移除暂未支持该版本的 EMI 集成并对尚未发布的其他可选模组做了安全兼容回退。
2. 修复：移除 `@OnlyIn`，样板供应器客户端包改由客户端处理类处理。
3. 优化：README、发布工作流和开发约定改为 26.1.2 版本描述。
4. 新增：添加 .gitattributes，文本文件统一为 LF 换行，.bat 脚本保持 CRLF。
5. 修复：界面贴图改用带渲染管线的 blit 重载，样板编码区、工作台区、样板管理区和面板背景不再缺失。
6. 修复：无线终端库磁铁和清空按钮图标改用正确的 ae2wtlib_api 命名空间路径，顶部栏图标不再缺失。
7. 修复：样板管理区搜索模式和高亮按钮贴图改用自有图集 wcwt_pattern_icons.png，不再依赖 ExtendedAE；为五个语言文件补充缺失的译名键（搜索模式、高亮、显示/隐藏槽位、饰品槽切换）。
8. 修复：高级编码卡配方仅在可选材料已注册时加载。
9. 修复：终端界面初始化前跳过配方查看器的尺寸查询。
10. 新增：同步工业升级社区版的开发运行依赖，并更新兼容坐标。
11. 修复：按键绑定分类显示模组名称。
12. 修复：已安装卡片的扩展界面可以点击打开。
13. 修复：恢复样板槽图标和隐藏槽位角标，并对齐原版悬停高亮。
