# Phase 1 架构基线

生效日期：2026-04-08

当前有效架构基线由以下文件共同组成：

- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/loom-java-package-structure.md`
- `docs/loom-phase1-contract-freeze.md`

本文件不再承载旧版内容，改为作为当前有效基线入口。

## 当前冻结结论

- 产品核心：会话优先、项目原生、可见 Trace、分层 Context/Memory
- 后端形态：模块化单体
- 前后端共享合同：`packages/contracts/src/index.ts`
- OpenClaw 边界：Phase 1 中仅保留可见入口与配置位，不进入真实主执行链路

## 当前实现对齐状态

- `BE-002`、`BE-003` 已将最小 REST / SSE 骨架落到集成分支
- `FE-002`、`FE-003` 已开始消费这些合同
- `ARC-001` ~ `ARC-005` 的当前可执行冻结产物见 `docs/loom-phase1-contract-freeze.md`
