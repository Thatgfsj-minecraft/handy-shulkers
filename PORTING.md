# PORTING — 多版本分支与移植状态

> 仓库结构：`main` 只保留 4 个核心 CI 子项目（`1.21.1/fabric`、`1.21.1/neoforge`、`1.21.11/fabric`、`1.21.11/neoforge`）；其余版本各自以 `versions/*` 分支维护（自 main 快照）。
> 状态含义：**snapshot** = 快照已建、未动工；**进行中** = 移植/构建修复中；**可构建** = `./gradlew build` 通过并已推送；**BLOCKED** = 受阻（附原因）。
> 规则：只在 `./gradlew build` 通过后才提交推送；构建不了标 BLOCKED 并写明原因，禁止提交半成品。

| 分支 | 版本 / 加载器 | 状态 | 说明 |
|---|---|---|---|
| versions/1.21.4 | 1.21.4 × Fabric + NeoForge | snapshot | 自 main 快照（f860522），构建未验证 |
| versions/1.21.5 | 1.21.5 × Fabric + NeoForge | snapshot | 自 main 快照（f860522），构建未验证 |
| versions/1.21.8 | 1.21.8 × Fabric + NeoForge | snapshot | 自 main 快照（f860522），构建未验证 |
| versions/1.21.9 | 1.21.9 × Fabric + NeoForge | snapshot | 自 main 快照（f860522）；1.21.9 NeoForge 官方仅 beta（21.9.16-beta） |
| versions/1.21.10 | 1.21.10 × Fabric + NeoForge | snapshot | 自 main 快照（f860522），构建未验证 |
| versions/26.1 | 26.1 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL；commit c7f1262（移植 1.4.1 缺失语义：ScrollingMenu/Screen、256 钳制、largeBoxes、SelfTest 扩展；Screen 按 26.1 提取式渲染管线重写，GuiGraphics 已删除）。26.2/26.3 分支若用 GuiGraphics 写法需参考本分支适配 |
| versions/26.2 | 26.2 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL @ f860522，分支代码零改动即通过（fabric/neoforge jar 各 1 枚）；注意：Loom 1.18.2 需 Gradle daemon JVM ≥25 |
| versions/26.3 | 26.3 × Fabric + NeoForge | 可构建 | 2026-10-05 fabric 零改动即过、neoforge 首次 NeoForm 反编译后通过 @ f860522；26.3.0.37-beta 实测可解析。注：C 盘近满，隔离缓存已迁 `O:\clawwork\.gradle-handyshulkers`，后续构建一律用它 |
| versions/1.16.5 | 1.16.5 × Forge | 可构建 | 2026-10-05 BUILD SUCCESSFUL；commit 69cb258（FG4 + forge 1.16.5-36.2.39 + official mappings + JDK 8）。语义差异：无滚动菜单→>54 格拒绝打开（actionbar 提示）；存储走 BlockEntityTag.Items；自定义 item tag `handyshulkers:shulker_boxes` 数据包可扩展。运行时未实测（构建环境无 MC 客户端） |
| versions/1.12.2 | 1.12.2 × Forge | 可构建 | 2026-10-05 BUILD SUCCESSFUL；commit 6502f42（FG2.3 + Gradle 4.9 + JDK 8 + stable_39；Forge 用 14.23.5.2847——2848+ 的 FG2.3 所需 userdev.jar 已 404）。语义差异：无 tags→白名单 extraShulkerBoxes；无滚动界面→>54 格拒绝打开；织布机/切石机/砂轮/制图台/锻造台（1.14+）不存在不做 |
| versions/1.7.10 | 1.7.10 × Forge | 进行中 | 分支已建（自 main 8b5dc01），`1.7.10/forge` 子项目移植中；注意 1.7.10 无原版潜影盒，移植以"手持右键开功能方块 + 白名单模组盒"为语义基线 |
