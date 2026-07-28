# AE2 WCWT 1.20.1.7-hotfix

## English

This fork release adds a Forge 1.20.1 compatibility hotfix for
TooManyRecipeViewers and ExtendedAE Plus.

- Applies full-JEI compatibility Mixins only when both the JEI mod ID and the
  required JEI internal target class are present.
- Prevents TooManyRecipeViewers from triggering a client class-loading crash
  merely because it exposes the JEI mod ID.
- Separates ExtendedAE Plus mouse input from WCWT's full-JEI internal GUI
  path.
- Converts hovered item, fluid, and Mekanism chemical ingredients through a
  compatibility layer that depends only on JEI's public ingredient API.
- Leaves unsupported versions or missing optional integrations on their
  original input path instead of consuming the click.
- Includes the existing NBT-aware recipe-pull correction from the
  `1.20.1.7-hotfix` source line.

Install the same JAR on the dedicated server and every client.

## 日本語

このforkリリースは、TooManyRecipeViewersとExtendedAE Plusを併用する
Forge 1.20.1環境向けの互換修正です。

- フルJEI互換Mixinを、JEIのmod IDと必要なJEI内部対象クラスの両方が
  存在する場合だけ適用。
- JEIのmod IDを公開するTooManyRecipeViewersによって、JEI内部クラスの
  読み込みクラッシュが誘発される問題を修正。
- ExtendedAE Plusのマウス入力処理をWCWTのフルJEI内部GUI経路から分離。
- マウス下のアイテム、液体、Mekanism Chemicalを、JEI公開材料APIだけに
  依存する互換層で変換。
- 未対応版や任意連携が欠ける場合はクリックを消費せず、本来の入力経路へ
  戻す。
- `1.20.1.7-hotfix`系統に含まれるNBT差分対応レシピ転送修正も収録。

専用サーバーと全クライアントへ同じJARを導入してください。
