# Changelog

## v26.1.2-0.0.1

### English

1. Changed: Migrated the mod to Minecraft 26.1.2 and NeoForge 26.1.2.129 on Java 25 and Gradle 9.2.1, updating GUI rendering, mixins, recipes, item model definitions, tests, and 26.1.2 dependencies (AE2, AE2WTLib, JEI, Curios, and optional integrations), while removing EMI support and safely stubbing optional mods without 26.1.2 releases.
2. Fixed: Clientbound pattern provider packets hand off to a client-only handler class instead of using `@OnlyIn`, so NeoForge 26.1.2 no longer reports a loading issue for the mod.
3. Changed: README, the publish workflow and AGENTS.md now describe Minecraft 26.1.2, NeoForge 26.1.2 and version 26.1.2-0.0.1.

### 中文

1. 新增：将模组迁移至 Minecraft 26.1.2 与 NeoForge 26.1.2.129（Java 25 与 Gradle 9.2.1），完成界面渲染、混入、配方、物品模型定义、单元测试及核心依赖升级，移除暂未支持该版本的 EMI 集成并对尚未发布的其他可选模组做了安全兼容回退。
2. 修复：移除 `@OnlyIn`，样板供应器客户端包改由客户端处理类处理。
3. 优化：README、发布工作流和开发约定改为 26.1.2 版本描述。
