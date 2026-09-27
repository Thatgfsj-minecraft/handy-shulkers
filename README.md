# Handy Shulkers

一个"从物品栏直接打开潜影盒"的 Minecraft 模组，是 [Advanced Shulkerboxes](https://github.com/henkelmax/advanced-shulkerboxes) 的独立重写版，目标只有一个：**兼容尽量多的其他模组**。

灵感与差异：参考了 [Quick Shulker](https://www.curseforge.com/minecraft/mc-mods/quick-shulker) 的交互模型，但**去掉了它"把盒子戴到头上"的机制**——本模组不占用任何装备栏，头盔上有没有东西、放了什么都完全不影响使用。

## 特性

- **潜行 + 右键**：打开潜影盒（Quick Shulker 同款触发方式），空中对着哪里都行
- **正常右键 = 原版行为**：拿着盒子时一切照旧——床能睡觉、工作台/织布机能用、按钮能按、门能开、箱子能开；对地面右键照常放置盒子。**方块交互永远优先，绝不吞交互**
- **标签驱动识别**（兼容性的关键）：
  - 内置 17 个原版潜影盒
  - 自动纳入 `#c:shulker_boxes`（跨模组通用约定标签）和 `#minecraft:shulker_boxes`——其他模组的潜影盒只要加入了这两个标签，**装上即支持，无需本模组适配**
  - 可用资源包/数据包向 `handyshulkers:shulker_boxes` 标签追加任意物品
- **通用容器界面**：行数按盒子实际容量自动推断（1~6 行），大容量模组盒子也能完整打开
- **数据安全**：
  - 内容读写走原版 `minecraft:container` 数据组件（1.20.5+ 模组的标准做法）
  - 禁止把容器物品（包括空盒）塞进打开的盒子里，杜绝套娃
  - 盒子本体在打开期间无法被移动进自身
  - 只在内容变化时写回，不会给"私有存储格式"的盒子凭空写组件（可用配置收紧保护）
- **服务端逻辑，客户端可选**：纯服务端安装即可使用（客户端逻辑始终放行原版行为，不会吞包）
- 配置文件 `config/handyshulkers.json`（首次启动自动生成）

## 配置项

| 键 | 默认 | 说明 |
|---|---|---|
| `requireSneak` | `true` | 潜行+右键打开盒子（Quick Shulker 同款）。改为 `false` 则直接右键打开，但床/按钮等无界面方块在手持盒子时会被占用 |
| `openInAir` | `true` | 允许对空右键（潜行时）打开盒子 |
| `openOnBlocks` | `true` | 允许对着方块潜行右键打开盒子 |
| `allowUnknownStorage` | `true` | 允许打开存储格式未知的模组盒子（关闭后更保守、绝不误写数据） |
| `allowFakePlayers` | `false` | 允许自动化假玩家触发打开 |
| `forceRows` | `-1` | 强制界面行数（1-6），`-1` 为自动检测 |

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

GitHub Actions 会在每次 push 时自动构建全部四个项目，产物在 Actions 页面的 Artifacts 中下载。

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
| 许可证 | 无 LICENSE（保留所有权利） | MIT |

## 许可证

MIT
