# Handy Shulkers

一个"从物品栏直接打开潜影盒"的 Minecraft 模组，是参考 [Advanced Shulkerboxes](https://github.com/henkelmax/advanced-shulkerboxes) 的独立重写版，目标只有一个：**兼容尽量多的其他模组**。

灵感与差异：参考了 [Quick Shulker](https://www.curseforge.com/minecraft/mc-mods/quick-shulker) 的交互模型，但**去掉了它"把盒子戴到头上"的机制**——本模组不占用任何装备栏，头盔上有没有东西、放了什么都完全不影响使用。

## 特性

**核心交互模型（Quick Shulker 式"拿着就用"）：**

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

**兼容性：**

- **标签驱动识别**：盒子走 `handyshulkers:shulker_boxes`（内置 17 个原版盒 + 自动纳入 `#c:shulker_boxes`、`#minecraft:shulker_boxes`）；工作台/织布机/床分别走 `crafting_tables`/`looms`/`beds` 标签（均自动纳入对应 `#c:` 通用标签）。**其他模组的物品打了标签即自动支持**，也可用资源包/数据包自行扩展
- **通用容器界面**：行数按盒子实际容量自动推断（1~6 行），大容量模组盒子也能完整打开
- **数据安全**：内容读写走原版 `minecraft:container` 数据组件；只在内容变化时写回；禁止容器套娃；未知存储格式的盒子可配置拒绝打开；**超过 54 格的特大盘子会拒绝打开并提示**，杜绝截断丢物品

- 配置文件 `config/handyshulkers.json`（首次启动自动生成）

## 配置项

| 键 | 默认 | 说明 |
|---|---|---|
| `requireSneak` | `false` | 默认：右键使用手持物品、潜行右键放置。改为 `true` 则反过来（Quick Shulker 式潜行触发） |
| `allowUnknownStorage` | `true` | 允许打开存储格式未知的模组盒子（关闭后更保守、绝不误写数据） |
| `allowFakePlayers` | `false` | 允许自动化假玩家触发使用 |
| `forceRows` | `-1` | 强制盒子界面行数（1-6），`-1` 为自动检测 |

## 版本与加载器

| 目录 | Minecraft | 加载器 |
|---|---|---|
| `1.21.1/fabric` | 1.21.1 | Fabric |
| `1.21.1/neoforge` | 1.21.1 | NeoForge (21.1.x) |
| `1.21.11/fabric` | 1.21.11 | Fabric |
| `1.21.11/neoforge` | 1.21.11 | NeoForge (21.11.x) |

四个项目互相独立，各自有独立的 Gradle 构建脚本。

## 构建

每个项目目录下单独执行：

```bash
cd 1.21.1/fabric
./gradlew build
# 产物在 build/libs/
```

CI 工作流位于 `ci/build.yml`（构建全部四个项目）。启用方法：把它移动到 `.github/workflows/build.yml` 后 push，之后每次 push 会自动构建，产物在 Actions 页面的 Artifacts 中下载。

> 本地构建提示：如果你的机器配置了全局 Gradle 镜像 init 脚本（如阿里云镜像），NeoForge 相关依赖可能解析失败，建议用独立的 GRADLE_USER_HOME 构建：
> ```bash
> GRADLE_USER_HOME=~/.gradle-handyshulkers ./gradlew build
> ```

## 与原版 Advanced Shulkerboxes 的差异

| | Advanced Shulkerboxes | Handy Shulkers |
|---|---|---|
| 物品识别 | 硬编码 `instanceof ShulkerBoxBlock`（原版类），多数模组盒子无法识别 | 标签驱动，`#c:shulker_boxes` 全模组通用 |
| 界面大小 | 固定 27 格 | 按盒子容量 9~54 格自适应 |
| 数据兼容 | 一律写原版组件，私有存储的盒子有丢数据风险 | 未知存储默认拒绝打开（可配置），不误写 |
| 潜影盒套娃 | 靠运行时判断 | 槽位级禁止容器物品放入 |
| 许可证 | 无 LICENSE（保留所有权利） | GPL-3.0 |

## 许可证 / License

本项目基于 [GPL-3.0](./LICENSE)（GNU 通用公共许可证第 3 版）开源发布。

- 你可以自由地使用、学习、修改和分发本项目的代码；
- 基于本项目修改或二次开发的作品，必须同样以 GPL-3.0 协议开源，并保留相应的版权与许可声明；
- 本项目不提供任何担保，完整条款请参见 [LICENSE](./LICENSE) 文件。
