# 游戏分享社区（小红书式）项目企划书

## 0. 文档信息

* 项目名称：Side Quest 社区
* 形态：Web + 小程序用户端；Web 管理后台；支持 MCP/机器人运营
* 核心定位：面向玩家的“内容种草 + 资源分享 + 社交互动 + 视频/弹幕”的社区产品
* 关键工程目标：可观测、可灰度、可 A/B、可扩展、可运维、可对接推荐算法与 LLM

---

## 1. 项目背景与目标

### 1.1 背景

需要开发一个“类似朋友圈的多媒体展示小软件”，并扩展为更偏“小红书式”的社区：以帖子为核心承载图文/视频内容与互动，具备可运营、可审核、可统计的数据与系统能力。

### 1.2 项目目标

1. 形成可用的游戏分享社区闭环：发帖—浏览—互动—收藏—检索—管理
2. 支持资源分享（游戏包/Mod/存档等）并通过 CDN 高效分发
3. 支持视频流处理与弹幕，提升内容消费体验
4. 支持埋点与日志统计，为后续推荐算法提供数据底座
5. 支持 MCP 适配：提供 LLM 友好接口，允许运营定制机器人模拟真实用户执行操作（发帖、评论等），并具备审计与风控
6. 工程化要求：界面良好、格式工整、代码风格良好；架构采用 DDD；接入 Kafka、Redis 等中间件；支持灰度发布与 A/B

---

## 2. 角色与使用场景

### 2.1 角色

* 普通用户：浏览、发帖、互动、收藏、聊天
* 内容作者：发布图文/视频/资源，维护帖子
* 管理员/运营：内容治理、用户管理、配置运营策略、看数据
* 机器人（Bot 用户）：通过 MCP 工具以“拟人化”方式活跃社区（受限速、审计、风控）

### 2.2 关键用户旅程

* 浏览：首页推荐/关注/最新 → 帖子详情 → 评论/评分/收藏 → 分享
* 创作：选择分区 → 输入内容 → 上传图片/视频/资源 → 标签选择/自动建议 → 发布
* 检索：按日期/分区/标签/关键词/媒体类型搜索
* 运营：后台查看内容与用户 → 审核/下架/封禁 → 看板分析 → 调整策略与开关

---

## 3. 功能需求范围

## 3.1 用户端（Web + 小程序）

### 3.1.1 账号与安全

* 注册/登录/退出
* 用户可设置与修改登录密码（找回/重置流程）
* 设备与会话管理（基础版本：刷新 token；增强：设备指纹与黑名单）

### 3.1.2 内容（帖子）

* 发布：文本、图片、视频、资源附件（可选）
* 查看：信息流/分区流/标签流/帖子详情
* 修改/删除：仅限本人内容
* 标签体系与分区体系并行：

  * 分区（Section）：强约束（建议必选）
  * 标签（Tag）：弱约束（可多选，可由系统建议）

### 3.1.3 互动

* 评分（星级/数值）
* 评论（朋友圈式评论；可扩展楼中楼）
* 点赞（可选，但建议保留以支撑推荐与热度）
* 举报、拉黑（建议纳入第一或第二阶段）

### 3.1.4 收藏与收藏夹

* 收藏帖子
* 创建收藏夹、移动/整理收藏、排序与批量操作

### 3.1.5 搜索与筛选

* 关键词搜索（帖子/用户/标签/资源）
* 筛选维度：日期、分区、标签、媒体类型（图文/视频/资源）

### 3.1.6 聊天室（现代实时设计，且非 WebSocket）

* 房间列表（按分区或主题）
* 房间消息流
* 发送消息、拉取新消息（长轮询/增量游标）
* Web 端可增强：SSE 单向推送（小程序优先长轮询）

### 3.1.7 视频与弹幕

* 视频播放：多码率/自适应（HLS/DASH）
* 弹幕发送与回放（按时间片拉取 + 预取缓存）

---

## 3.2 管理后台（Web Admin）

### 3.2.1 用户管理

* 查询、封禁/解封、删除/注销（按权限控制）
* 用户画像（基础：注册时间、活跃、违规次数；增强：设备/IP）

### 3.2.2 内容管理

* 帖子/评论/弹幕/资源：查询、删除、下架、置顶、加精
* 分区与标签配置管理（含多语言显示名）

### 3.2.3 审核与风控

* 敏感词命中记录、风险分级
* 举报工单：流转、处置、留痕
* LLM 复审结果（摘要化展示、可解释字段）

### 3.2.4 数据看板（加分项）

* DAU/留存、内容供给（发帖量）、互动率（评论/收藏/评分）、热门分区/标签
* 风控趋势（举报率、违规率）
* 实验指标（A/B 组别曝光与转化）

---

## 4. 信息架构与页面规划

## 4.1 用户端页面（Web/小程序统一规划）

1. 登录/注册/找回密码
2. 首页信息流：推荐 / 关注 / 最新
3. 分区页：分区列表 → 分区详情流
4. 标签广场：标签列表 → 标签详情流
5. 搜索页：综合结果 + 筛选（日期/分区/标签/媒体类型）
6. 帖子详情：内容区（图文/视频）、评分、评论、收藏、分享、资源下载区
7. 发帖页：标题（可空）、正文、媒体上传、分区必选、标签多选、资源上传
8. 个人中心：我的帖子、我的评论、我的收藏、收藏夹管理、设置
9. 消息通知：评论回复、系统通知（建议）
10. 聊天室：房间列表、房间详情（长轮询/SSE）

## 4.2 管理后台页面

* 后台登录
* Dashboard 总览看板
* 用户管理（列表/详情/封禁/删除）
* 内容管理（帖子/评论/弹幕/资源）
* 审核中心（敏感词/LLM/举报工单）
* 运营配置（分区/标签/i18n 文案、Feature Flags、A/B 实验）
* 日志审计（管理员操作记录、机器人 tool call 记录）

---

## 5. 总体架构设计（DDD + 事件驱动 + 可演进微服务）

### 5.1 架构原则

* DDD 分层：Interfaces（Controller/DTO）→ Application（UseCase）→ Domain（聚合/领域服务）→ Infrastructure（Repo/MQ/Cache）
* 服务数量受控：满足“模块独立开发”但不“过度微服务化”
* 事件驱动：关键变更写入 Kafka，解耦搜索索引、统计、媒体处理、审核复审
* 多端适配：Web/小程序以统一契约接口为基础，建议引入 BFF（可选）
* 可观测：统一 TraceID、结构化日志、指标与告警

---

## 6. 微服务拆分（不需要很多个微服务）

采用“**6 个核心服务 + 2 个可选服务**”的规模控制方案：

### 6.1 核心服务（建议必须）

1. **gateway-service（API 网关）**

* 鉴权校验、路由、限流、灰度与 A/B 分流、CORS、审计、Trace 注入
* 不承载业务，不访问业务库

2. **config-service（配置中心）**

* 基于 Nacos Config / Apollo 统一管理各微服务配置，支持动态刷新
* 存储各服务的数据库连接、中间件地址、Feature Flags、业务开关

3. **identity-service（账号与权限）**

* 用户、密码、会话、RBAC、封禁、管理员体系

3. **core-service（社区核心：帖子 + 互动 + 收藏夹 + 分区/标签配置 + 通知 + 聊天模块）**

* 帖子：发布/编辑/删除/详情/列表
* 互动：评论/评分/点赞（可选）
* 收藏：收藏与收藏夹
* 配置：分区/标签（含 i18n）
* 通知中心（建议纳入，收益大且复杂度适中）
* 聊天室（第一阶段放这里，后续可拆）

4. **media-service（媒体与资源 + 弹幕）**

* 上传签名/STS、元数据、CDN 地址生成
* 转码编排（HLS/DASH、多码率、封面）
* 资源分享（附件）
* 弹幕：写入/拉取/回放（热缓存 + 冷存储）

5. **search-service（检索与索引）**

* OpenSearch/ES 索引维护与搜索 API
* 消费 Kafka 事件同步索引（避免 core 直连 ES）

6. **moderation-service（审核与风控）**

* 分词敏感词过滤、规则分级、举报工单、发布前校验与发布后复审
* 统一承接机器人风控策略（限速、内容风险）

### 6.2 可选服务（按阶段引入）

7. **analytics-service（埋点与统计）**

* Kafka → 清洗 → ClickHouse → 看板查询 API

8. **mcp-service（MCP Server 与机器人运营）**

* Tool registry、schema 版本、权限 scope、限流、审计、幂等
* 调用 core/media/search/moderation 完成机器人操作

---

## 7. 网关管理（治理细化）

### 7.1 网关能力清单

* 路由：/api/user、/api/admin、/api/mcp、/api/public、/api/internal
* 鉴权：JWT 校验、scope 区分用户/管理员/MCP
* 限流：按 IP/user_id/token 维度；发帖/评论/上传签名/聊天发送更严格
* 灰度发布：按权重放量 + 白名单 + 一键回滚
* A/B 分流：按 user_id hash 固定分桶，保证一致性
* 安全：CORS、WAF/黑名单、关键接口签名校验
* 可观测：TraceID 注入、访问日志结构化、网关指标输出

### 7.2 入口分流策略（建议统一在网关层做“入口一致性”）

* 灰度：10% 用户进入 v2（按 user_id hash）
* A/B：进入 v2 的用户再按 experiment_id 分桶，注入 `X-Variant`

---

## 8. 数据与存储设计

### 8.1 存储与中间件选型

* MySQL：核心业务 OLTP
* Redis：会话、热点缓存、计数器、限流、聊天室热消息、弹幕热分片
* Kafka：事件总线（索引同步、媒体处理、埋点、审核复审）
* OpenSearch/ES：全文检索与聚合
* 分析数据库：ClickHouse（已确定，用于埋点与大吞吐量日志）
* 可观测性：Grafana + Prometheus + Loki + Tempo（已确定，LGTM 方案）
* 对象存储：MinIO（已确定，支持 S3 协议）
* 视频处理：FFmpeg 容器化异步处理（已确定）

### 8.2 核心数据模型（摘要）

* identity：user、credential、role/permission、ban_record
* core：post、post_media_ref、comment、rating、favorite、collection、collection_item、notification、chat_room、chat_message（或消息归档表）
* media：media、resource、transcode_task、danmaku
* moderation：sensitive_rule、hit_record、moderation_case、report_ticket
* analytics（OLAP）：event_log（按天分区）

### 8.3 一致性策略

* 强一致（事务内）：帖子与其媒体引用、收藏夹与条目写入
* 最终一致：

  * post/interaction 事件 → search-service 更新索引
  * media 上传 → 转码事件 → 更新可播放地址
  * 埋点与统计 → OLAP 入库延迟可接受

---

## 9. 媒体链路（视频流 + CDN + 弹幕）

### 9.1 上传与分发

1. 客户端申请上传签名（media-service）
2. 直传对象存储（避免业务服务转发大文件）
3. 上传完成 → Kafka `media.uploaded`
4. 转码任务：生成 HLS/DASH 多码率、封面、时长等元信息
5. 回写 media 元数据，生成 CDN 播放地址

### 9.2 弹幕

* 写入：danmaku.create（关联 video_id、time_offset_ms）
* 拉取：按时间窗批量获取（客户端预取与缓存）
* 存储：Redis ZSET（热）+ MySQL（冷归档）或 ClickHouse（量大时）

---

## 10. 聊天室实时方案（明确非 WebSocket）

### 10.1 设计原则

* 小程序优先使用长轮询 + 游标（since_id）
* Web 可增强：SSE（单向推送）+ HTTP 发送消息
* 后端：消息有序、可回放、可限速、可审计

### 10.2 服务端要点

* room 分片与消息 ID 单调递增
* Redis Streams 或 Redis List 做热消息队列
* MySQL 做归档
* 防刷：频率限制、敏感词过滤、风控策略

---

## 11. 审核与敏感词过滤（分词 + 分级 + LLM）

### 11.1 敏感词分词过滤

* 分词：中文分词（词典可运营维护）
* 词库分级：

  * S0：必杀（直接拦截/下架）
  * S1：高风险（发布后复审/人工审核）
  * S2：提示/降权（可展示但降低推荐）
* 规则增强：变体处理（同音、插符号）、正则模板

### 11.2 LLM 识别与内容精炼（用于筛查与推荐预备）

* 自动摘要：便于运营快速筛查
* 自动标题补全：用户未起好标题时补全
* 自动标签建议：在标签不全时补齐结构化主题
* 输出结构化特征（可扩展 embedding）：为后续推荐召回与相似内容提供基础

---

## 12. 埋点与日志统计（推荐算法预备）

### 12.1 埋点事件规范

* 公共字段：user_id/device_id/session_id/client/app_version/ts/trace_id
* 核心事件：

  * feed_impression、feed_click、post_create、comment_create、rating_submit、favorite_add、video_play/video_complete、resource_download、chat_send
* 采集链路：客户端 SDK → gateway → Kafka → analytics 清洗 → ClickHouse → 看板

### 12.2 日志与可观测

* 结构化日志（JSON）
* TraceID 全链路贯穿（网关注入）
* 关键告警：错误率、P95、Kafka lag、转码队列积压、限流命中异常、机器人异常行为峰值

---

## 13. i18n（国际化）

### 13.1 范围

* Web/小程序/后台 UI 文案国际化：zh-CN、en-US（可扩展）
* 分区/标签展示名支持多语言（运营配置）
* 错误码结构化：前端根据语言包映射文案（推荐一致性）

### 13.2 工程规范

* 文案 key 命名统一（按模块 namespace）
* 禁止业务代码硬编码中文（lint/CI 约束）
* 语言包按模块拆分按需加载

---

## 14. 灰度发布、Feature Flags 与 A/B 实验

### 14.1 Feature Flags（功能开关）

* 粒度：全局/按端/按分区/按用户桶/白名单
* 下发策略：BFF 或 core-service 拉取配置（Redis 缓存），并在响应中透传给前端

### 14.2 A/B 实验

* 分桶：hash(user_id, experiment_id) 固定落组，保证一致性与可复现
* 必须埋点：exposure 与 conversion 事件携带 experiment_id/variant_id
* 运营后台提供实验配置 CRUD 与基础看板（ClickHouse）

### 14.3 灰度发布

* 网关层按权重/白名单把用户路由到 v2
* 支持回滚：保留 v1 版本与镜像，异常自动/手动回滚

---

## 15. MCP 适配与机器人运营（LLM 友好接口）

### 15.1 MCP 服务职责

* Tool 注册与 schema 管理（版本化）
* 权限 scope、限流配额、审计留痕
* 幂等支持（idempotency_key 防止重复发帖/重复评论）
* 机器人策略（可选）：素材库、节奏控制、拟人化行为约束

### 15.2 Tool 设计（第一期建议控制在 20 个以内）

* user：get_profile、follow/unfollow、block/unblock（可选）
* content：create_post、update_post、attach_media、delete_post（默认禁用）
* interaction：comment_create/reply、rating_submit、favorite_add/remove、collection_create/add/move
* search：query_posts、get_post_detail_llm_view、query_tags/users
* moderation：check_text、summarize_for_review
* chat：send_message、fetch_messages（严格限速）

### 15.3 安全与治理（必须）

* MCP Token 与 scope 最小化
* 限流：per-token/per-bot/per-IP
* 审计：保存每次 tool call 输入输出摘要、trace_id、操作者与时间，支持回放定位问题
* 风控：机器人内容必须走 moderation 流程（同真人一致或更严格）

---

## 16. 工程框架与规范（确保后期高效稳定）

### 16.1 仓库与契约

* 推荐 Monorepo
* contracts 目录维护 OpenAPI（REST）与 AsyncAPI（Kafka event schema）
* 前端 TS client 自动生成，减少联调摩擦

### 16.2 DDD 代码分层统一

* interfaces / application / domain / infrastructure
* 统一错误码规范、统一日志字段与 Trace 传递

### 16.3 异步事件规范（Kafka）

* Topic 命名：`domain.entity.event.v1`
* payload 版本化：变更必须升版本，兼容旧消费者
* 事件用于：索引同步、媒体处理、统计、审核复审、计数器纠偏

---

## 17. 部署规划（Docker 集群化 + K8s）

## 17.1 Docker（开发与可复现部署）

* dev：docker-compose 一键启动全套依赖（mysql/redis/kafka/es/clickhouse + 服务）
* staging（可选）：docker-compose 多副本或 Docker Swarm
* 关键：healthcheck、初始化脚本（建库、建 topic、建索引）、统一 .env 管理

## 17.2 K8s 部署（主规划）

### 17.2.1 环境与命名空间

* dev/staging/prod 使用 namespace 隔离
* ResourceQuota/LimitRange 防止资源抢占

### 17.2.2 入口与网络

* Ingress Controller（Nginx/Kong）
* 推荐链路：Ingress → gateway-service → 各内部服务（ClusterIP）
* NetworkPolicy：仅允许 gateway 访问对外服务入口；internal API 不对公网暴露

### 17.2.3 工作负载类型

* 无状态服务：Deployment（gateway/identity/core/media/search/moderation/(analytics)/(mcp)）
* 中间件优先云托管；若演示自建则 StatefulSet（MySQL/Redis/Kafka/ES/ClickHouse）

### 17.2.4 灰度发布落地

* 方案：Argo Rollouts（Canary/Blue-Green）或 Ingress Canary（简化版）
* 指标守护：错误率、P95、举报率、发帖失败率、Kafka lag

### 17.2.5 自动伸缩

* HPA：core/media/search/moderation 按 CPU/请求数扩容
* 转码 worker：按任务积压（第二阶段）

### 17.2.6 配置与密钥

* ConfigMap：非敏感配置、默认开关、实验参数
* Secret：JWT 密钥、DB 密码、对象存储密钥
* 重要运行参数（探针/超时/重试）标准化模板化

---

## 18. 安全、合规与风控（必要要求）

* 认证鉴权：JWT + refresh token；后台强 RBAC
* 写操作频控：发帖/评论/聊天/上传签名限流
* 资源分享：白名单后缀/大小限制、hash 校验（可选：病毒扫描）
* 审计：管理员操作日志、MCP tool call 日志
* 内容治理：敏感词分词 + 规则分级 + 举报工单 + 复审链路

---

## 19. 测试与质量保障

* 单元测试：domain/application 层为主
* 契约测试：OpenAPI/事件 schema 兼容性校验
* 集成测试：关键链路（发帖—索引—搜索—互动—统计）
* 压测关注点：信息流、搜索、媒体播放首帧、长轮询连接数、Kafka 消费 lag
* 代码规范：lint + formatter + CI 门禁

---

## 20. 里程碑计划（建议）

### 阶段 1：MVP（可答辩闭环）

* 登录/改密
* 分区 + 发帖（图文/基础视频上传）+ 我的内容
* 评论 + 评分
* 收藏 + 收藏夹
* 后台：用户/帖子/评论基础管理
* 基础埋点（曝光/点击/发帖/评论/收藏）

### 阶段 2：增强体验与治理

* 视频转码 HLS、多码率、封面
* 弹幕
* 搜索（ES）与标签广场
* 审核：分词敏感词 + 举报工单
* 聊天室：长轮询，小程序可用；Web 增强 SSE

### 阶段 3：加分项与平台化

* LLM 摘要/标题补全/标签建议（用于筛查与推荐预备）
* analytics-service 看板完善 + 实验指标
* MCP 接入 + 机器人编排（限速/审计/风控）

---

# 21. 技术栈总结（建议方案）

> 以下给出“主线推荐栈”，并标注可替换项。目标是：落地成本适中、生态成熟、便于课程/团队协作与后期演进。

## 21.1 前端

* Web 用户端：React + TypeScript（或 Vue3 + TS）
* 小程序：微信小程序原生 / Taro（视团队偏好）
* 管理后台：React Admin / Ant Design Pro（或 Vue 管理模板）
* i18n：i18next（React）/ vue-i18n（Vue）；小程序语言包 JSON

## 21.2 后端（DDD 主线）

* 主服务（gateway/identity/core/media/search/moderation/config）：

  * Java Spring Boot（推荐）+ 分层 DDD 结构
  * 网关：APISIX/Kong（云原生）或 Spring Cloud Gateway（Spring 体系）
* 配置中心：Nacos Config（已确定）
  * 注册中心：Nacos Discovery（已确定）
* LLM/内容理解（如独立）：Python FastAPI（可选，若不拆则 core/moderation 内调用外部模型服务）

## 21.3 中间件与存储

* MySQL：核心业务数据
* Redis：缓存/会话/限流/计数器/聊天室热数据/弹幕热分片
* Kafka：事件总线（索引、媒体、埋点、审核）
* OpenSearch/Elasticsearch：搜索与聚合
* ClickHouse：埋点与报表（OLAP）
* 对象存储：S3/OSS/MinIO（演示）
* CDN：用于图片/视频/资源分发

## 21.4 可观测与运维

* Trace：OpenTelemetry
* Metrics：Prometheus + Grafana
* Logs：Loki（轻量）或 ELK（完整）
* CI/CD：GitHub Actions / Jenkins + 镜像仓库

## 21.5 部署

* 开发联调：docker-compose
* 集群化（可选）：Docker Swarm（强调“纯 Docker 集群”）
* 主规划：Kubernetes（Ingress + Deployments + HPA + 灰度发布组件如 Argo Rollouts）

## 21.6 灰度与 A/B

* 灰度：网关分流 + K8s Rollouts（Canary/Blue-Green）
* Feature Flags：config-service（可选）或运营后台配置 + Redis 缓存
* A/B：按 user_id 固定分桶 + 埋点归因 + ClickHouse 看板

## 21.7 MCP

* MCP Server：mcp-service（Tool schema 版本化、审计、限流、幂等）
* 对外以 tool 形式暴露：发帖/评论/评分/收藏/搜索/审核/聊天（受控）