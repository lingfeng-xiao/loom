# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.2] - 2026-02-28

### Changed
- 简化健康检查配置
  - 移除显式的 livenessstate 和 readinessstate 配置（使用 Spring Boot 默认值）
  - 部署脚本添加成功日志输出

## [1.1.1] - 2026-02-28

### Changed
- CI/CD 流水线优化
  - 拆分构建和部署为独立 Job
  - 部署后自动健康检查
  - 验证 `/actuator/health` 端点确保应用正常启动

## [1.1.0] - 2026-02-28

### Added
- 后端健康检查端点配置
  - 集成 Spring Boot Actuator
  - 暴露 `/actuator/health` 健康检查端点
  - 支持 Kubernetes 存活探针和就绪探针
  - 暴露 `/actuator/info` 和 `/actuator/metrics` 端点

## [1.0.0] - 2026-02-28

### Added
- 项目初始版本
- 后端基础架构
  - Spring Boot 3.2.0 基础框架
  - REST API 示例接口
- 前端基础架构
  - React + TypeScript 项目结构
- Docker 容器化支持
  - Dockerfile 配置
  - docker-compose 编排
- CI/CD 流水线
  - GitHub Actions 自动构建与发布
  - GHCR 镜像仓库集成

[Unreleased]: https://github.com/loom-project/loom/compare/v1.1.2...HEAD
[1.1.2]: https://github.com/loom-project/loom/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/loom-project/loom/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/loom-project/loom/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/loom-project/loom/releases/tag/v1.0.0
