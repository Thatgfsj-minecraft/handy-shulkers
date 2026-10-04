# PORTING — 多版本分支与移植状态

> 仓库结构：`main` 只保留 4 个核心 CI 子项目（`1.21.1/fabric`、`1.21.1/neoforge`、`1.21.11/fabric`、`1.21.11/neoforge`）；其余版本各自以 `versions/*` 分支维护（快照自主 main）。
> 状态含义：**snapshot** = 快照已建、未动工；**进行中** = 移植/构建修复中；**可构建** = `./gradlew build` 通过并已推送；**BLOCKED** = 受阻（附原因）。
> 规则：只在 `./gradlew build` 通过后才提交推送；构建不了标 BLOCKED 并写明原因，禁止提交半成品。

| 分支 | 版本 / 加载器 | 状态 | 说明 |
|---|---|---|---|
| versions/1.21.4 | 1.21.4 × Fabric + NeoForge | snapshot | 快照自主 main，构建未验证 |
| versions/1.21.5 | 1.21.5 × Fabric + NeoForge | snapshot | 快照自主 main，构建未验证 |
| versions/1.21.8 | 1.21.8 × Fabric + NeoForge | snapshot | 快照自主 main，构建未验证 |
| versions/1.21.9 | 1.21.9 × Fabric + NeoForge | snapshot | 快照自主 main；1.21.9 NeoForge 官方仅 beta（21.9.16-beta） |
| versions/1.21.10 | 1.21.10 × Fabric + NeoForge | snapshot | 快照自主 main，构建未验证 |
| versions/26.1 | 26.1 × Fabric + NeoForge | 进行中 | 已有代码但从未构建；JDK 25 + fabric-loom 新插件 id `net.fabricmc.fabric-loom` |
| versions/26.2 | 26.2 × Fabric + NeoForge | 进行中 | 同上 |
| versions/26.3 | 26.3 × Fabric + NeoForge | snapshot | 已有代码但从未构建；排队等待修复（26.3 NeoForge 仅 26.3.0.37-beta） |
| versions/1.16.5（待建） | 1.16.5 × Forge | snapshot | 计划：新建分支 + `1.16.5/forge` 子项目，从 1.21.x 语义重写 |
| versions/1.12.2（待建） | 1.12.2 × Forge | snapshot | 候选 |
| versions/1.7.10（待建） | 1.7.10 × Forge | snapshot | 候选 |
