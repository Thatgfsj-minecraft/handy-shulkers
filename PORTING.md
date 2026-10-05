# PORTING — 多版本分支与移植状态

> 仓库结构：`main` 只保留 4 个核心 CI 子项目（`1.21.1/fabric`、`1.21.1/neoforge`、`1.21.11/fabric`、`1.21.11/neoforge`）；其余版本各自以 `versions/*` 分支维护（自 main 快照）。
> 状态含义：**snapshot** = 快照已建、未动工；**进行中** = 移植/构建修复中；**可构建** = `./gradlew build` 通过并已推送；**BLOCKED** = 受阻（附原因）。
> 规则：只在 `./gradlew build` 通过后才提交推送；构建不了标 BLOCKED 并写明原因，禁止提交半成品。
> ⚠️ 运行时验证进行中：headless `runServer` 冒烟已通过 10/10——1.20.1（fabric+forge）、1.19.2（fabric+forge）、1.18.2（fabric+forge）、1.21.4/fabric、26.2/fabric、main 的 1.21.1/fabric 与 1.21.11/fabric（服务器启动至 Done，日志 `SELF-TEST PASS: container round-trip OK`）；其余分支仍止于编译验证。已发现并修复中：forge 子项目缺 pack.mcmeta 导致 data/ 标签不生效（"Missing data pack"）、FG 系缺 runs{} 配置。

| 分支 | 版本 / 加载器 | 状态 | 说明 |
|---|---|---|---|
| versions/1.21.4 | 1.21.4 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL，零改动 @ f860522 |
| versions/1.21.5 | 1.21.5 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL，零改动 @ f860522 |
| versions/1.21.8 | 1.21.8 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL，零改动 @ f860522 |
| versions/1.21.9 | 1.21.9 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL，零改动 @ f860522；NeoForge 21.9.16-beta 已实测可解析，正式版发布后建议升级 |
| versions/1.21.10 | 1.21.10 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL，零改动 @ f860522 |
| versions/26.1 | 26.1 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL；commit c7f1262（移植 1.4.1 缺失语义：ScrollingMenu/Screen、256 钳制、largeBoxes、SelfTest 扩展；Screen 按 26.1 提取式渲染管线重写，GuiGraphics 已删除）。26.2/26.3 分支若用 GuiGraphics 写法需参考本分支适配 |
| versions/26.2 | 26.2 × Fabric + NeoForge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL @ f860522，分支代码零改动即通过（fabric/neoforge jar 各 1 枚）；注意：Loom 1.18.2 需 Gradle daemon JVM ≥25 |
| versions/26.3 | 26.3 × Fabric + NeoForge | 可构建 | 2026-10-05 fabric 零改动即过、neoforge 首次 NeoForm 反编译后通过 @ f860522；26.3.0.37-beta 实测可解析。注：C 盘近满，隔离缓存已迁 `O:\clawwork\.gradle-handyshulkers`，后续构建一律用它 |
| versions/1.16.5 | 1.16.5 × Forge | 可构建 | 2026-10-05 BUILD SUCCESSFUL；commit 69cb258（FG4 + forge 1.16.5-36.2.39 + official mappings + JDK 8）。语义差异：无滚动菜单→>54 格拒绝打开（actionbar 提示）；存储走 BlockEntityTag.Items；自定义 item tag `handyshulkers:shulker_boxes` 数据包可扩展。运行时未实测（构建环境无 MC 客户端） |
| versions/1.12.2 | 1.12.2 × Forge | 可构建 | 2026-10-05 BUILD SUCCESSFUL；commit 6502f42（FG2.3 + Gradle 4.9 + JDK 8 + stable_39；Forge 用 14.23.5.2847——2848+ 的 FG2.3 所需 userdev.jar 已 404）。语义差异：无 tags→白名单 extraShulkerBoxes；无滚动界面→>54 格拒绝打开；织布机/切石机/砂轮/制图台/锻造台（1.14+）不存在不做 |
| versions/1.7.10 | 1.7.10 × Forge | 可构建 | 2026-10-05 BUILD SUCCESSFUL；commit f8d347c（anatawa12 FG1.2 fork + Gradle 6.9.4 + JDK 8 + Forge 10.13.4.1614，reobf SRG 产物）。1.7.10 无原版潜影盒：识别层为 extraShulkerBoxes/largeBoxes 白名单 + NBT Items 启发式；>54 槽内容入隐藏区整包写回；织布机等 1.14+ 方块不做。运行时未实测 |
| versions/1.20.1 | 1.20.1 × Fabric + Forge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL；commit bff62b0（Loom 1.17.21 + FG6 + Gradle 8.8/9.8 + JDK 17 toolchain；fabric-api 0.92.12+1.20.1 / forge 47.4.26 均末期版）。无 data components→NBT `BlockEntityTag.Items` 存储（256 钳制等价）；有 item tags→tag+instanceof 双通道；>54 格滚动界面已随版本移植（GuiGraphics 渲染按 1.20.1 改写）。**运行时冒烟通过**：runServer fabric+forge 双端 Done + `SELF-TEST PASS` |
| versions/1.19.2 | 1.19.2 × Fabric + Forge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL；commit 3ad3ad0（Loom 1.17.21 + FG6.0.54 锁版 + Gradle 8.8/9.8 + JDK 17；fabric-api 0.77.0+1.19.2 / forge 43.5.2 均末期）。最大适配：**pre-GuiGraphics**——ScrollingScreen 按 PoseStack 直绘重写（1.19.4 才有 GuiGraphics）；存储走 NBT `BlockEntityTag.Items`；tags 复数目录。**运行时冒烟通过**：runServer fabric+forge 双端 Done + `SELF-TEST PASS`（pre-GuiGraphics 数据路径运行时可用） |
| versions/1.18.2 | 1.18.2 × Fabric + Forge | 可构建 | 2026-10-05 双子项目 BUILD SUCCESSFUL；commit 2ff6560（Loom 1.17.21 + FG5.1.77 + Gradle 9.8/7.6.4 + JDK 17；fabric-api 0.77.0+1.18.2 / forge 40.3.12 末期）。pre-GuiGraphics PoseStack 直绘；NBT 存储；1.18.2 无 `#minecraft:shulker_boxes` item tag（1.19.4+），模组盒走 `#c:` 或数据包扩展。运行时未实测 |

## 剩余候选与推荐顺序

1. **全分支运行时冒烟**——各分支 `runServer` 验证 `SELF-TEST` 日志：第一批（1.20.1×2 / 1.19.2×2 / 主仓 core 4）与第二批（1.18.2×2 / 1.21.4/fabric / 26.2/fabric）进行中；`runClient` 目检滚动界面与睡床交互待做
2. **CI 首轮结果跟进**——main + 全部 14 个 versions/* 分支均已携带 `.github/workflows/build.yml`（JDK/Gradle 矩阵按分支配方定制），跑红的分支按报错修配方
3. **1.21.9 NeoForge beta→正式版升级复验**（21.9.16-beta，截至 2026-10-05 仍无正式版）；26.x beta 同理
4. **更早版本候选**：1.20.4/1.20.6、1.19.4 视需求补
