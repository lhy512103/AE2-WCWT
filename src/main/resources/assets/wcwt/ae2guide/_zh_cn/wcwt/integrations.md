---
navigation:
  parent: wcwt/index.md
  title: 附属模组兼容
  icon: ae2:advanced_card
  position: 90
---
# 附属模组兼容

## 配方查看器

安装 JEI 或 EMI 后，可向手动工作台、样板编码区和高级编码幽灵槽传输配方或材料。

- 多候选 Ingredient 用于手动物品替换。
- NBT 和数据组件精确保留，避免同 ID 特殊物品混用。
- 特殊 AEKey 类型会通过 AE2 通用转换路径进入处理样板和高级编辑区。
- 配方页中的数量很大时，WCWT 批量检查可用量，不会逐个物品反复扫描网络。

## Polymorph

安装 Polymorph 后，手动工作区和样板编码区可选择冲突配方的实际输出，不要求额外安装 Polymorphic Energistics。选择结果后再编码，避免相同输入被编码为另一配方。

## Advanced AE

提供高级处理样板格式与按输入设置方向的编码能力。WCWT 的输入方向编辑会调用其编码器完成转换，不自行仿造格式。

## MEGA Cells 与 AllTheCompressed

MEGA Cells 提供兼容大宗元件、压缩卡和压缩截断接口；AllTheCompressed 等内容模组可提供多重压缩链。只有链条能被 MEGA Cells 识别时，WCWT 才显示可选截断级别。

## AE2 Lightning Tech

提供闪电 AEKey 与过载样板。安装后谐振过载编码卡显示过载编辑区，可为各输入输出设置严格或仅 ID 匹配。

## AE2 Crystal Science

提供谐振样板及其转换能力。安装后显示 21 格谐振存储和批量转换按钮。

## Mekanism

Mekanism 化学品可在处理样板和高级替换区作为特殊材料使用，但需要整合环境中存在把该化学品暴露为 AE2 可识别存储键的支持。仅有 Mekanism 本体而没有相应 AE2 化学品存储接入时，终端无法从网络提取它。

## Curios 与 Cosmetic Armor Reworked

分别提供真实饰品槽和装饰盔甲槽。缺少模组时，对应升级卡和按钮隐藏，不影响基础终端。

## ExtendedAE

ExtendedAE 不只是运行时测试模组，WCWT 的样板管理区会实际接入它：ExtendedAE 提供的、通过 AE2 样板容器接口暴露的扩展供应器会进入供应器列表；世界位置高亮按钮调用 ExtendedAE 的客户端高亮渲染器，相关按钮图标也复用其资源。因此未安装 ExtendedAE 时，AE2 基础供应器仍可管理，但 ExtendedAE 专属供应器和世界高亮视觉不可用。

## ExtendedAE Plus

WCWT 会兼容 ExtendedAE Plus 的 JEI 搜索信息、配方类型搜索键、快捷键和映射配置接口。安装后优先调用它的实现；未安装时，WCWT 仍可自行读写相同的客户端映射路径。样板管理区的供应器列表、样板移动、自动上传目标选择，以及打开供应器输出侧目标容器均由 WCWT 自己实现，不以 ExtendedAE Plus 为必需依赖。
