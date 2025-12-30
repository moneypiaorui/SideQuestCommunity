# SideQuestCommunity

SideQuestCommunity 是一个社区内容平台的单体仓库，覆盖用户、内容、互动、媒体、搜索、审核与统计分析等完整链路。

## 目录结构

```
SideQuestCommunity/
├── backend/                 # 后端微服务集群 (Java 17 + Spring Boot 3 + DDD)
│   ├── sidequest-common     # 公共基础包 (AOP、异常处理、上下文等)
│   ├── sidequest-config     # 配置中心接入模块 (Nacos Config)
│   ├── sidequest-gateway    # API 网关 (鉴权、路由、限流)
│   ├── sidequest-identity   # 身份与权限服务 (注册、登录、JWT)
│   ├── sidequest-core       # 核心业务服务 (帖子、互动、收藏等)
│   ├── sidequest-media      # 媒体与弹幕服务 (OSS 直传、Redis 弹幕)
│   ├── sidequest-search     # 搜索服务 (ES 同步、全文检索)
│   ├── sidequest-moderation # 内容审核服务 (敏感词、AI 复审)
│   ├── sidequest-analytics  # 埋点分析服务 (Kafka 消费统计)
│   └── sidequest-mcp        # 机器人运营服务 (MCP Tool Registry)
├── frontend/                # 前端工程
├── infra/                   # 基础设施
│   ├── docker-compose/      # 中间件 (PG、Redis、Kafka、ES、Nacos 等)
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

## 架构概览

- 流量入口: Nginx -> Gateway (JWT 校验 + UserId 注入)
- 服务发现与配置: Nacos
- 持久化
  - PostgreSQL: 关系型业务数据
  - Redis: Token 黑名单、缓存、弹幕 ZSet
  - MinIO: 图片与视频文件
- 异步链路: Core -> Kafka -> Search/Analytics
- 可观测性: Micrometer -> Prometheus -> Grafana

## 运行依赖

- JDK: 17
- 构建工具: Maven 3.8+
- 容器: Docker 20+ / Docker Compose v2
- 前端: Node.js 18+ (如需本地构建前端)

## 快速启动 (Docker Compose)

1) 启动基础设施与后端服务
```bash
cd infra/docker-compose
docker compose up -d
```

2) 首次启动需要等待中间件初始化 (PostgreSQL/Nacos/Kafka/ES 等)

3) 访问
- 网关: http://localhost:8080
- Nacos 控制台: http://localhost:8848/nacos
- MinIO 控制台: http://localhost:9001
- PgAdmin: http://localhost:5005

## 服务启动顺序 (概念)

1) 中间件: PostgreSQL -> Redis -> Kafka -> Elasticsearch -> MinIO -> ClickHouse
2) 注册中心: Nacos
3) 业务服务: Gateway/Identity/Core/Media/Search/Moderation/Analytics/MCP
4) 网关入口: Nginx

Docker Compose 已配置健康检查与依赖关系，无需手动排序启动。

## 环境变量与配置

后端服务主要通过环境变量指定依赖地址 (示例见 `infra/docker-compose/docker-compose.yml`)：
- Nacos: `SPRING_CLOUD_NACOS_SERVER_ADDR`
- 数据库: `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- Kafka: `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- Redis: `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT`
- Elasticsearch: `SPRING_ELASTICSEARCH_URIS`
- MinIO: `MINIO_ENDPOINT` / `MINIO_PUBLICENDPOINT`

如需本地运行单个微服务，可在其 `application-dev.yml` 中覆盖或使用环境变量注入。

## 常用端口

- 8080: Gateway
- 8081-8088: 业务微服务 (Identity/Core/Media/Search/Moderation/Analytics/MCP)
- 80/443: Nginx
- 5432: PostgreSQL
- 6379: Redis
- 9092: Kafka
- 9200: Elasticsearch
- 8848: Nacos
- 9000/9001: MinIO
- 8123: ClickHouse
- 5005: PgAdmin

## 微服务与核心接口

统一入口
- 网关端口: `8080`
- 接口前缀: `/api/{service-name}`

Identity Service
- 功能: 用户生命周期管理、BCrypt 密码、JWT、RBAC 权限管理
- 接口:
  - `POST /api/identity/register`
  - `POST /api/identity/login`
  - `GET /api/identity/admin/roles`
  - `POST /api/identity/admin/users/{id}/roles`
  - `POST /api/identity/admin/roles/{code}/permissions`

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
- 功能: 搜索消费 Kafka 同步索引，审核提供同步检测
- 接口:
  - `GET /api/search/query`
  - `POST /api/moderation/check`

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
- 登录后保存 Token (LocalStorage/Cookie)
- 受限接口携带 `Authorization: Bearer <TOKEN>`
- 网关层已处理 CORS

实时弹幕
- 前端按视频进度定时调用 `GET /api/media/danmaku` 拉取弹幕

## 前端本地开发

管理端 (Next.js)
```bash
cd frontend/side-quest-manage
npm install
npm run dev
```

小程序/H5 (视工程实际为准)
```bash
cd frontend/sidequest-mini
npm install
npm run dev
```

## 管理端部署说明

- 管理端为静态站点，部署路径 `/admin`
- `frontend/side-quest-manage/next.config.mjs` 的 `basePath` 需与部署路径一致
- 可通过 `ASSET_PREFIX` 配置静态资源前缀

## 常见问题

- 服务起不来: 优先查看 `docker compose ps` 和 `docker compose logs -f <service>`
- Nacos/ES 启动慢: 首次启动会初始化，耐心等待健康检查通过
- 端口冲突: 修改 `infra/docker-compose/docker-compose.yml` 中的端口映射

## 后续开发重点

- 完善分区与标签广场逻辑
- media-service 接入 FFmpeg 异步转码
- Grafana 预置 Dashboard
- mcp-service Tool Registry 能力补齐
