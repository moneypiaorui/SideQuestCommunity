# SideQuestCommunity

SideQuestCommunity 是一个社区内容平台的单仓项目，涵盖用户、内容、互动、媒体、搜索、审核与统计分析等完整链路。

## 目录结构

```
SideQuestCommunity/
├── backend/                 # 后端微服务集群 (Java 17 + Spring Boot 3 + DDD)
│   ├── sidequest-common     # 公共基础包 (AOP, 异常处理, 上下文)
│   ├── sidequest-config     # 配置中心接入模块 (Nacos Config)
│   ├── sidequest-gateway    # API 网关 (鉴权、路由、限流)
│   ├── sidequest-identity   # 身份与权限服务 (注册、登录、JWT)
│   ├── sidequest-core       # 核心业务服务 (帖子、互动、收藏)
│   ├── sidequest-media      # 媒体与弹幕服务 (OSS 直传、Redis 弹幕)
│   ├── sidequest-search     # 搜索服务 (ES 同步、全文检索)
│   ├── sidequest-moderation # 内容审核服务 (敏感词、AI 复审)
│   ├── sidequest-analytics  # 埋点分析服务 (Kafka 消费统计)
│   └── sidequest-mcp        # 机器人运营服务 (MCP Tool Registry)
├── frontend/                # 前端工程
├── infra/                   # 基础设施
│   ├── docker-compose/      # 中间件 (PG, Redis, Kafka, ES, Nacos, Grafana)
│   └── k8s/                 # Kubernetes 部署清单
└── deployment-guide.md      # 部署手册与 SQL 脚本说明
```

## 文档导航

- 需求分析: `doc/requirements-analysis.md`
- 项目时间线: `doc/project-timeline.md`
- 数据库说明: `doc/database.md`
- 微服务拓扑: `doc/microservices-topology.md`
- 接口定义: `doc/openapi.yaml`
- 部署手册: `deployment-guide.md`

## 微服务与核心接口

统一入口
- 网关端口: `8080`
- 接口前缀: `/api/{service-name}`

Identity Service
- 功能: 用户生命周期管理、BCrypt 密码、JWT
- 接口:
  - `POST /api/identity/register`
  - `POST /api/identity/login`

Core Service
- 功能: 社区内容生产与互动
- 接口:
  - `POST /api/core/posts`
  - `POST /api/core/interactions/comment`
  - `POST /api/core/interactions/rate`
  - `POST /api/core/interactions/favorite`

Media Service
- 功能: OSS 直传、弹幕存储与拉取
- 接口:
  - `GET /api/media/upload-url`
  - `POST /api/media/danmaku`
  - `GET /api/media/danmaku`

Search & Moderation
- 功能: 搜索消费 Kafka 同步索引，审核提供同步检查
- 接口:
  - `GET /api/search/query`
  - `POST /api/moderation/check`

## 架构概要

- 流量入口: Nginx -> Gateway (JWT 校验与 UserId 注入)
- 服务发现与配置: Nacos
- 持久化:
  - PostgreSQL: 关系型业务数据
  - Redis: Token 黑名单、缓存、弹幕 ZSet
  - MinIO: 图片与视频文件
- 异步链路: Core -> Kafka -> Search/Analytics
- 可观测性: Micrometer -> Prometheus -> Grafana

## 前端对接指南

统一返回结构
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

鉴权
- 登录后保存 Token（LocalStorage/Cookie）
- 受限接口携带 `Authorization: Bearer <TOKEN>`
- 网关层已处理 CORS

实时弹幕
- 前端按视频进度定时调用 `GET /api/media/danmaku` 拉取弹幕

## 管理端部署说明

- 管理端为静态站点，部署路径 `/admin`
- `frontend/side-quest-manage/next.config.mjs` 中 `basePath` 需与部署路径一致
- 可通过 `ASSET_PREFIX` 配置静态资源前缀

## 后续开发重点

- 完善分区与标签广场逻辑
- media-service 接入 FFmpeg 异步转码
- Grafana 预置 Dashboard
- mcp-service Tool Registry 能力补齐
