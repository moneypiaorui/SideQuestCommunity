# SideQuestCommunity 大作业报告

## 1. 项目概述
SideQuestCommunity 是一个面向内容生产与互动的社区平台，覆盖发帖、评论、点赞、收藏、私信、弹幕、搜索、审核与统计分析等场景。项目采用前后端分离与微服务架构，后端以多模块服务承载业务能力，前端分别提供用户端与管理端界面。

项目目标
- 面向真实场景设计并实现完整 Web 应用
- 体现 Web 开发、数据库设计与用户体验理解
- 满足课程对需求分析、数据库建模、SQL 查询与报告输出的要求

## 2. 需求分析
### 2.1 角色与核心场景
- 普通用户：注册登录、浏览内容、发布帖子、评论/点赞/收藏、私信聊天、观看弹幕
- 管理员：用户管理、内容管理、审核处理、风险处置
- 内容审核：对帖子与媒体进行合规校验
- 数据分析：查看平台热度、内容分布、用户活跃情况

### 2.2 功能需求
- 身份与账号：注册、登录、JWT 鉴权、角色与状态控制、关注/粉丝关系
- 内容与互动：帖子发布、编辑、删除、状态流转，评论、点赞、收藏、浏览量统计
- 媒体与弹幕：图片/视频上传、媒体元数据管理、弹幕发送与按时间窗口拉取
- 搜索与审核：全文检索、索引同步、内容审核接口对接
- 统计分析：关键事件埋点、基础统计指标
- 聊天：私聊/群聊房间、成员、消息记录

### 2.3 非功能需求
- 可用性：核心接口高可用，关键流程失败可回退
- 安全性：鉴权与权限隔离、敏感内容审核、日志可追溯
- 性能：搜索与弹幕接口支持高并发
- 可维护性：模块边界清晰，服务间接口标准化

需求来源与原文整理：`doc/requirements-analysis.md`

## 3. 系统架构与实现概述
### 3.1 架构形态
系统采用微服务架构，统一入口为 Gateway Service。核心服务包括：
- Identity Service：认证与用户管理
- Core Service：帖子、评论、互动等核心业务
- Media Service：媒体文件与弹幕
- Search Service：全文检索
- Moderation Service：内容审核
- Chat Service：即时聊天
- Analytics Service：统计分析
- MCP Service：AI 工具接口扩展

服务间同步调用使用 Feign，异步事件通过 Kafka 进行解耦。结构化数据存储于 PostgreSQL，搜索使用 Elasticsearch，统计分析使用 ClickHouse，缓存与实时数据使用 Redis。

拓扑说明详见：`doc/microservices-topology.md`

### 3.2 前端与交互
- 用户端：发布、浏览、互动、聊天等功能
- 管理端：用户管理、内容管理、审核流、搜索管理
- UI 设计关注信息层级与主要操作路径的可达性

前端目录参考：
- 用户端：`frontend/sidequest-mini`
- 管理端：`frontend/side-quest-manage`

## 4. 数据库设计
### 4.1 逻辑数据模型（LDM）
实体（不少于 10 个）
- 用户 t_user
- 关注关系 t_follow
- 帖子 t_post
- 评论 t_comment
- 点赞 t_like
- 收藏 t_favorite
- 分区 t_section
- 标签 t_tag
- 帖子-标签关联 t_post_tag
- 媒体 t_media
- 弹幕 t_danmaku
- 聊天房间 t_chat_room
- 聊天成员 t_chat_room_member
- 聊天消息 t_chat_message

关系与基数约束（示例）
- 一对一（可选）：t_post 0..1 -> 1 t_media（帖子可关联 0 或 1 个媒体记录）
- 一对多：t_user 1 -> N t_post（一个用户可发布多篇帖子）
- 多对一：t_post N -> 1 t_section（多篇帖子属于同一分区）
- 多对多：t_user N <-> N t_user，通过 t_follow（关注/粉丝关系）
- 多对多：t_post N <-> N t_tag，通过 t_post_tag（帖子标签关系）
- 多对多：t_chat_room N <-> N t_user，通过 t_chat_room_member（聊天室成员关系）

Mermaid ER 图与 UML 关系图见：`doc/database.md`

### 4.2 物理数据模型（PDM）与规范化
设计要点
- 将多对多关系拆为关联表（t_follow、t_chat_room_member）
- 将可枚举字段设为状态码（status、type）
- 帖子与媒体保持 0..1 关联，避免重复存储媒体信息
- 核心业务表保持第三范式，聚合统计字段（like_count 等）作为性能权衡

索引设计（示例）
- 唯一索引：t_user.username，t_follow(follower_id, following_id)，t_like(post_id, user_id)
- 普通索引：t_danmaku.video_id（用于按视频拉取弹幕）
- 业务需要时可扩展：t_post.create_time，t_comment.post_id

数据表定义详见：`doc/database.md` 与 `infra/docker-compose/postgres/init.sql`

### 4.3 PostgreSQL 脚本生成（示例片段）
注：本项目使用 PostgreSQL，以下为核心表结构示例：

```sql
CREATE TABLE t_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64),
  avatar VARCHAR(255),
  signature VARCHAR(255),
  role VARCHAR(20) DEFAULT 'USER',
  status INT DEFAULT 0,
  follower_count INT DEFAULT 0,
  following_count INT DEFAULT 0,
  total_liked_count INT DEFAULT 0,
  post_count INT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_post (
  id BIGSERIAL PRIMARY KEY,
  author_id BIGINT NOT NULL,
  title VARCHAR(255),
  content TEXT,
  section_id BIGINT,
  status INT DEFAULT 0,
  like_count INT DEFAULT 0,
  comment_count INT DEFAULT 0,
  favorite_count INT DEFAULT 0,
  view_count INT DEFAULT 0,
  image_urls TEXT,
  video_url VARCHAR(255),
  video_cover_url VARCHAR(255),
  video_duration INT DEFAULT 0,
  media_id BIGINT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_post_author ON t_post (author_id);
CREATE INDEX idx_post_section ON t_post (section_id);
CREATE INDEX idx_post_create_time ON t_post (create_time);

CREATE TABLE t_post_tag (
  id BIGSERIAL PRIMARY KEY,
  post_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(post_id, tag_id)
);

CREATE INDEX idx_post_tag_post_id ON t_post_tag (post_id);
CREATE INDEX idx_post_tag_tag_id ON t_post_tag (tag_id);
```

完整脚本可由建模工具导出，或直接参考 `infra/docker-compose/postgres/init.sql`。

## 5. 功能实现与技术栈
### 5.1 技术栈
- 前端：HTML/CSS/JavaScript，用户端与管理端分别实现
- 后端：Java（Spring Boot 3）、MyBatis-Plus
- 数据库：PostgreSQL（可转换为 MySQL 版本）
- 中间件：Redis、Kafka、Elasticsearch、ClickHouse、MinIO、Nacos
- 部署：Docker Compose

### 5.2 功能映射（摘要）
- 用户注册/登录：Identity Service (`AuthController`)
- 内容发布与互动：Core Service (`PostController`, `InteractionController`)
- 媒体上传与弹幕：Media Service (`MediaController`)
- 搜索与索引：Search Service (`SearchController`)
- 审核：Moderation Service (`ModerationController`)
- 聊天：Chat Service (`ChatController`)
- 统计：Analytics Service (`AnalyticsController`)

## 6. SQL 查询能力展示（PostgreSQL 语法）
以下示例覆盖课程要求的查询类型。

### 6.1 单表查询
```sql
SELECT id, username, nickname, create_time
FROM t_user
WHERE status = 0
ORDER BY create_time DESC;
```

### 6.2 连接查询（内连接）
```sql
SELECT p.id, p.title, u.username
FROM t_post p
INNER JOIN t_user u ON p.author_id = u.id
ORDER BY p.create_time DESC;
```

### 6.3 外连接查询（左连接）
```sql
SELECT p.id, p.title, c.id AS comment_id
FROM t_post p
LEFT JOIN t_comment c ON c.post_id = p.id;
```

### 6.4 自连接
```sql
SELECT f1.follower_id, f2.following_id
FROM t_follow f1
JOIN t_follow f2
  ON f1.following_id = f2.follower_id
WHERE f1.follower_id <> f2.following_id;
```

### 6.5 聚合与分组（GROUP BY / ORDER BY）
```sql
SELECT p.author_id, COUNT(*) AS post_count
FROM t_post p
GROUP BY p.author_id
ORDER BY post_count DESC;
```

### 6.6 日期与时间函数
```sql
SELECT DATE(create_time) AS post_date, COUNT(*) AS cnt
FROM t_post
WHERE create_time >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(create_time)
ORDER BY post_date;
```

### 6.7 子查询
```sql
SELECT id, title
FROM t_post
WHERE author_id IN (
  SELECT id FROM t_user WHERE status = 0
);
```

### 6.8 相关子查询
```sql
SELECT p.id, p.title
FROM t_post p
WHERE EXISTS (
  SELECT 1
  FROM t_comment c
  WHERE c.post_id = p.id
);
```

### 6.9 集合运算
PostgreSQL 支持 UNION、INTERSECT、EXCEPT。
```sql
-- UNION
SELECT user_id AS uid FROM t_like
UNION
SELECT user_id AS uid FROM t_favorite;

-- INTERSECT
SELECT user_id FROM t_like
INTERSECT
SELECT user_id FROM t_favorite;

-- EXCEPT
SELECT user_id FROM t_like
EXCEPT
SELECT user_id FROM t_favorite;
```

### 6.10 多表连接查询
```sql
SELECT p.id, p.title, u.username, s.name AS section_name
FROM t_post p
JOIN t_user u ON p.author_id = u.id
JOIN t_section s ON p.section_id = s.id
ORDER BY p.create_time DESC;
```

### 6.11 除法查询（Division）
场景：找出关注了所有“官方用户”的普通用户。
```sql
SELECT u.id, u.username
FROM t_user u
WHERE NOT EXISTS (
  SELECT 1
  FROM t_user official
  WHERE official.role = 'ADMIN'
    AND NOT EXISTS (
      SELECT 1
      FROM t_follow f
      WHERE f.follower_id = u.id
        AND f.following_id = official.id
    )
);
```

## 7. 部署与运行
使用 Docker Compose 一键启动中间件与服务，详情见：
- `deployment-guide.md`
- `infra/docker-compose/docker-compose.yml`

## 8. 用户界面与体验说明
- 信息密度：内容流与管理列表采用分页加载
- 操作路径：发布/互动/管理入口明确，降低操作成本
- 反馈机制：状态提示、加载态、错误提示保证用户感知

建议在答辩与报告中补充界面截图与 SQL 执行结果截图。

## 9. 挑战与解决方案
- 异步链路一致性：搜索与统计通过 Kafka 事件同步，允许短暂延迟
- 业务约束一致性：当前未显式外键，业务层进行逻辑校验
- 媒体处理性能：视频转码与弹幕查询分离，使用异步与缓存优化

## 10. 总结与展望
项目完成了社区类应用的核心链路与扩展功能，覆盖用户管理、内容互动、媒体处理、搜索、审核与统计分析。后续可进一步扩展标签规范化、内容推荐与权限细粒度控制等能力。

## 11. 附录（待补充）
- 界面截图：用户端首页、发帖页、聊天页、管理端列表页
- SQL 执行截图：复杂查询、聚合查询、连接查询
