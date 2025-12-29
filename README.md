一、 项目目录结构 (Monorepo)
项目采用单仓多模块管理，将全栈代码与基础设施配置统一收纳。
SideQuestCommunity/
├── backend/                 # 后端微服务集群 (Java 17 + Spring Boot 3 + DDD)
│   ├── sidequest-common     # 公共基础包 (AOP, 异常处理, 自动上下文)
│   ├── sidequest-config     # 配置中心接入模块 (Nacos Config)
│   ├── sidequest-gateway    # API 网关 (鉴权、路由、限流)
│   ├── sidequest-identity   # 身份与权限服务 (注册、登录、JWT)
│   ├── sidequest-core       # 核心业务服务 (帖子、互动、收藏)
│   ├── sidequest-media      # 媒体与弹幕服务 (OSS直传、Redis弹幕)
│   ├── sidequest-search     # 搜索服务 (ES同步、全文检索)
│   ├── sidequest-moderation # 内容审核服务 (敏感词、AI复审)
│   ├── sidequest-analytics  # 埋点分析服务 (Kafka 消费统计)
│   └── sidequest-mcp        # 机器人运营服务 (MCP Tool Registry)
├── frontend/                # 前端工程 (规划中)
├── infra/                   # 基础设施
│   ├── docker-compose/      # 中间件全家桶 (PG, Redis, Kafka, ES, Nacos, Grafana)
│   └── k8s/                 # Kubernetes 部署清单
└── deployment-guide.md      # 部署手册与 SQL 脚本
二、 微服务功能与核心接口 (API Docs)
所有接口统一通过网关 8080 端口暴露，前缀为 /api/{service-name}。
1. 身份服务 (identity-service)
功能: 用户生命周期管理、密码加密 (BCrypt)、Token 签发。
接口:
POST /api/identity/register: 用户注册。
POST /api/identity/login: 用户登录，返回 JWT Token。
2. 核心服务 (core-service)
功能: 社区内容生产与互动，集成 UserContext 自动识别用户。
接口:
POST /api/core/posts: 发布帖子（自动触发 moderation 审核并同步 search）。
POST /api/core/interactions/comment: 发表评论。
POST /api/core/interactions/rate: 帖子评分 (1-5星)。
POST /api/core/interactions/favorite: 收藏帖子。
3. 媒体服务 (media-service)
功能: 屏蔽 OSS 复杂性，提供高性能弹幕。
接口:
GET /api/media/upload-url: 获取 MinIO/OSS 预签名上传链接。
POST /api/media/danmaku: 发送弹幕（存储至 Redis ZSet）。
GET /api/media/danmaku: 按时间窗口 (fromMs, toMs) 拉取弹幕流。
4. 搜索与审核 (Search & Moderation)
功能: search 消费 Kafka 消息实现搜索；moderation 提供同步审核。
接口:
GET /api/search/query: 全局全文检索。
POST /api/moderation/check: 内容合规性检查（供其他服务内部调用）。
三、 整体系统拓扑 (Architecture Topology)
系统采用典型的分布式驱动架构，各组件各司其职：
流量入口: Nginx -> Gateway (执行 JWT 校验，解析出 UserId 并注入请求头 X-User-Id)。
服务发现与配置: 所有微服务通过 Nacos 实现自动注册与配置动态刷新。
持久化层:
PostgreSQL: 存储所有关系型业务数据（用户、帖子、评论等）。
Redis: 存储 Token 黑名单、系统缓存及 实时弹幕 ZSet。
MinIO: 存储图片和视频原文件。
异步链路: Core Service -> Kafka -> Search/Analytics Service。实现发帖与索引、统计的解耦。
可观测性: Micrometer -> Prometheus -> Grafana。全量监控 JVM 及业务指标。
四、 前端对接指南 (Frontend Integration)
1. 统一响应格式
所有接口均返回以下结构：
{
  "code": 200,   // 200 为成功，500 为业务异常，401 为未授权
  "message": "success",
  "data": { ... } // 业务数据
}
2. 鉴权说明
登录后: 需将返回的 Token 存储（LocalStorage/Cookie）。
请求头: 所有受限接口必须携带 Authorization: Bearer <TOKEN>。
跨域: 网关层已处理 CORS，前端可直接跨域调用。
3. 实时性
弹幕: 前端根据视频播放进度，每隔几秒调用一次 GET /api/media/danmaku 拉取下一段弹幕，实现平滑显示。
五、 后端后续开发重点
业务深化: 完善 core-service 中的分区（Section）和标签广场逻辑。
视频链路: 在 media-service 中接入 FFmpeg 进行异步转码处理。
可观测性落地: 在 Grafana 中配置预定义的 Dashboard。
机器人运营: 完善 mcp-service，使 LLM 能通过定义的 Tool Registry 自动活跃社区。