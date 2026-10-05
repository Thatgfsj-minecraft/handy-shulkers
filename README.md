# Handy Shulkers

一个"从物品栏直接打开潜影盒"的 Minecraft 模组，是 [Advanced Shulkerboxes](https://github.com/henkelmax/advanced-shulkerboxes) 的独立重写版，目标只有一个：**兼容尽量多的其他模组**。

灵感与差异：参考了 [Quick Shulker](https://www.curseforge.com/minecraft/mc-mods/quick-shulker) 的交互模型，但**去掉了它"把盒子戴到头上"的机制**——本模组不占用任何装备栏，头盔上有没有东西、放了什么都完全不影响使用。

## 特性

**核心交互模型（Quick Shulker 式"拿着就用"，且完全没有"戴头上"机制，不占任何装备栏）：**

- **右键（不潜行）= 使用手持的功能性物品**：
  - 潜影盒 → 直接打开（无需放置）
  - **床 → 原地立刻睡觉**（保留原版规则：主世界、夜晚、附近没怪才能睡；可正常跳过夜晚；因为不是真床，不会设置重生点；床物品不会被消耗）
  - **工作台 → 直接打开 3×3 合成界面**
  - **织布机 → 直接打开织布机界面**
  - **附魔台 → 直接打开附魔界面**（站在书架旁仍有加成）
  - **切石机、砂轮、制图台、锻造台 → 各自打开对应界面**
  - **铁砧 → 直接打开铁砧界面**（便携铁砧不消耗耐久——没有方块可损耗）
  - **末影箱 → 直接打开你的末影箱**
- **右键对着带界面的方块（箱子/熔炉/附魔台……）**：方块优先，正常打开那个方块，不会被抢
- **潜行 + 右键 = 放置**：想放床、放工作台、放盒子，潜行右键即可，完全原版手感
- 其余一切物品和方块行为不受任何影响（客户端永不吞包，纯服务端逻辑）
- **启动自检**：服务器每次启动自动验证容器读写回环（日志搜 `SELF-TEST`），数据安全回归无处遁形

**兼容性（本模组的立身之本）：**

- **标签驱动识别**：盒子走 `handyshulkers:shulker_boxes`（内置 17 个原版盒 + 自动纳入 `#c:shulker_boxes`、`#minecraft:shulker_boxes`）；工作台/织布机/床分别走 `crafting_tables`/`looms`/`beds` 标签（均自动纳入对应 `#c:` 通用标签）。**其他模组的物品打了标签即自动支持**，也可用资源包/数据包自行扩展
- **通用容器界面**：行数按盒子实际容量自动推断（1~6 行）
- **超大容器滚动界面**：超过 54 格的大容器（如 [compressed-blocks](https://github.com/Thatgfsj-minecraft/compressed-blocks) 的压缩潜影盒，9×27=243 格）自动切换为 6 行可视 + 滚轮/滚动条翻行的滚动界面。架构与主流大容器模组（AE2 / Sophisticated Storage）一致：菜单固定暴露全部格子（槽位永不重映射），滚动只是客户端取景窗移动——零滚动网络包、零闪烁，Shift+点击覆盖整个容器（含当前藏着的行），拖拽布点无竞态。**兼容任意模组、直到原版 `minecraft:container` 组件的 256 格上限**：空盒/低占用的盒子在 `largeBoxes` 里声明容量即可，超出声明容量的最后一行会显示为锁定的空格
- **数据安全**：内容读写走原版 `minecraft:container` 数据组件；只在内容变化时写回；禁止容器套娃；未知存储格式的盒子可配置拒绝打开

- 配置文件 `config/handyshulkers.json`（首次启动自动生成）

## 配置项

| 键 | 默认 | 说明 |
|---|---|---|
| `requireSneak` | `false` | 默认：右键使用手持物品、潜行右键放置。改为 `true` 则反过来（Quick Shulker 式潜行触发） |
| `allowUnknownStorage` | `true` | 允许打开存储格式未知的模组盒子（关闭后更保守、绝不误写数据） |
| `allowFakePlayers` | `false` | 允许自动化假玩家触发使用 |
| `forceRows` | `-1` | 强制盒子界面行数（1-6），`-1` 为自动检测 |
| `largeBoxes` | `{"compressedblocks:compressed_shulker_box": 243}` | 物品 id → 声明容量（格）。列入此表的物品**无需加入任何标签**即可手持打开，且即使为空也直接进滚动界面（空盒子的组件里没有容量信息）；容量同样可来自方块实体真实尺寸或组件内容分布 |

## 版本与加载器

仓库采用 **`main` 主分支 + 多版本分支** 结构：`main` 只保留 4 个核心 CI 子项目（**1.21.1 × Fabric / NeoForge、1.21.11 × Fabric / NeoForge**，Java 21）；其余版本各自在 `versions/*` 分支维护，移植与构建状态见 [PORTING.md](PORTING.md)：

| 位置 | Minecraft | Fabric | NeoForge | Java | 备注 |
|---|---|---|---|---|---|
| `main` | 1.21.1 / 1.21.11 | ✅ | ✅ | 21 | CI 矩阵仅含这 4 个子项目 |
| `versions/1.21.4` ~ `versions/1.21.10` | 1.21.4 / 1.21.5 / 1.21.8 / 1.21.9 / 1.21.10 | ✅ | ✅ | 21 | 快照分支；1.21.9 的 NeoForge 官方仅有 beta（21.9.16-beta） |
| `versions/26.1` ~ `versions/26.3` | 26.1 / 26.2 / 26.3 | ✅ | ✅ | **25** | 26.x 为日期式版本线（去混淆化）；Fabric 侧用 Loom 新插件 id `net.fabricmc.fabric-loom`；26.3 的 NeoForge 仅有 beta（26.3.0.37-beta） |

每个版本目录（`<版本>/<加载器>/`）互相独立，各自有独立的 Gradle 构建脚本。全部 jar 见 [Releases](../../releases)。

## 构建

每个项目目录下单独执行：

```bash
git checkout 1.21.11          # 或任意 versions/* 版本分支
cd 1.21.11/fabric             # 或任意 <版本>/<加载器> 子目录
GRADLE_USER_HOME=~/.gradle-handyshulkers ./gradlew build
# 产物在 build/libs/
```

> 26.x 子项目（在 `versions/26.x` 分支上）需要 JDK 25（Gradle daemon 与编译都在 25 上），Fabric 侧使用 Loom 1.18.2 新插件 id `net.fabricmc.fabric-loom`（无映射行、依赖用 `implementation`）、Gradle ≥9.7。

CI 工作流已启用：`.github/workflows/build.yml`（即原 `ci/build.yml` 移入），每次 push 自动构建 core 4，产物在 Actions 页面的 Artifacts 中下载。各 `versions/*` 分支各自携带对应的 `.github/workflows/build.yml`（矩阵为该分支的子项目）。

> 本地构建提示：如果你的机器配置了全局 Gradle 镜像 init 脚本（如阿里云镜像），NeoForge 相关依赖可能解析失败，务必用独立的 GRADLE_USER_HOME 构建。

## 与原版 Advanced Shulkerboxes 的差异

| | Advanced Shulkerboxes | Handy Shulkers |
|---|---|---|
| 物品识别 | 硬编码 `instanceof ShulkerBoxBlock`（原版类），多数模组盒子无法识别 | 标签驱动，`#c:shulker_boxes` 全模组通用 |
| 界面大小 | 固定 27 格 | 按盒子容量 9~54 格自适应 |
| 数据兼容 | 一律写原版组件，私有存储的盒子有丢数据风险 | 未知存储默认拒绝打开（可配置），不误写 |
| 潜影盒套娃 | 靠运行时判断 | 槽位级禁止容器物品放入 |
| 许可证 | 无 LICENSE（保留所有权利） | GPL-3.0 |

## 许可证

GPL-3.0
