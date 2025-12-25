# Side Quest 社区项目部署与数据库指南

## 1. 基础设施与微服务启动 (Docker)

项目提供了一键启动中间件及微服务的 `docker-compose.yml`。

### 1.1 一键启动全量服务
在项目根目录下执行：
```bash
cd infra/docker-compose
docker-compose up -d --build
```
该命令会自动构建所有 Java 微服务镜像并启动中间件。

### 1.2 分阶段启动 (推荐)
如果资源有限，建议先启动中间件，待其就绪后再启动微服务。

**第一步：启动中间件**
```bash
docker-compose up -d postgres nacos redis kafka elasticsearch minio clickhouse
```

**第二步：执行数据库初始化**
参考 [第 2 节](#2-数据库初始化-postgresql) 初始化表结构。

**第三步：启动微服务与前端**
```bash
docker-compose up -d gateway-service identity-service core-service media-service search-service chat-service moderation-service analytics-service mcp-service nginx
```

## 2. 数据库初始化 (PostgreSQL)

连接到 PostgreSQL (`sidequest_db`) 后执行以下 DDL 语句。

### 2.1 用户与身份 (sidequest-identity)
```sql
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    signature VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 0, -- 0:正常, 1:封禁
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    total_liked_count INT DEFAULT 0,
    post_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_follow (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(follower_id, following_id)
);
```

### 2.2 社区核心 (sidequest-core)
```sql
CREATE TABLE IF NOT EXISTS t_post (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    author_name VARCHAR(64),
    title VARCHAR(255),
    content TEXT,
    section_id BIGINT,
    status INT DEFAULT 0, -- 0:发布, 1:草稿, 2:删除
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    view_count INT DEFAULT 0,
    image_urls TEXT,
    video_url VARCHAR(255),
    video_cover_url VARCHAR(255),
    video_duration INT,
    media_id BIGINT,
    tags VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_like (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(post_id, user_id)
);

CREATE TABLE IF NOT EXISTS t_favorite (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    collection_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_section (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    display_name_zh VARCHAR(64),
    display_name_en VARCHAR(64),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 0 -- 0: 正常, 1: 隐藏
);

CREATE TABLE IF NOT EXISTS t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    hit_count BIGINT DEFAULT 0
);
```

### 2.3 媒体处理 (sidequest-media)
```sql
CREATE TABLE IF NOT EXISTS t_media (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_key VARCHAR(255),
    file_type VARCHAR(32), -- image, video
    url VARCHAR(512),
    author_id BIGINT,
    status INT DEFAULT 0, -- 0: PROCESSING, 1: READY, 2: FAILED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_danmaku (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    time_offset_ms BIGINT,
    color VARCHAR(16),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2.4 即时通讯 (sidequest-chat)
```sql
CREATE TABLE IF NOT EXISTS t_chat_room (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64),
    type VARCHAR(20), -- PRIVATE, GROUP
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_chat_room_member (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    last_read_message_id BIGINT DEFAULT 0,
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(room_id, user_id)
);

CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    type VARCHAR(20), -- TEXT, IMAGE, VIDEO
    status INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 3. 环境配置与多环境切换

项目支持 `dev` (开发), `test` (测试), `prod` (生产) 三套环境。

### 3.1 环境切换方式
- **本地启动**: 启动参数增加 `-Dspring.profiles.active=test`
- **Docker/K8s**: 设置环境变量 `SPRING_PROFILES_ACTIVE=prod`

### 3.2 测试环境 (Test) 特殊处理
测试环境建议使用独立的数据库（如 `sidequest_test`）和中间件实例。
- **配置覆盖**: 修改 `application-test.yml` 中的地址。
- **Mock 服务**: 对于依赖的第三方服务（如 OSS），测试环境可以使用 MinIO 的 Mock 模式。

## 4. 配置中心 (Nacos) 使用细节

各服务通过 `bootstrap.yml` 接入配置中心。

**Data ID 规范**: `${spring.application.name}-${spring.profiles.active}.yaml`

核心公共配置示例：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/sidequest_db
    username: sidequest
    password: root
  kafka:
    bootstrap-servers: kafka:9092
```

## 4. 中间件初始化

### 4.1 Kafka Topic
系统启动前需确保 Kafka 中存在以下 Topic：
- `post-topic`: 帖子发布事件 (3 分片, 1 副本)
- `user-events`: 埋点事件 (6 分片, 1 副本)
- `video-process-topic`: 视频处理任务 (3 分片, 1 副本)
- `chat-message-topic`: 聊天消息转发 (3 分片, 1 副本)

### 4.2 Elasticsearch 索引
创建 `posts` 索引并配置 IK 分词器：
```bash
PUT /posts
{
  "settings": {
    "index": { "analysis": { "analyzer": { "default": { "type": "ik_max_word" } } } }
  }
}
```

## 5. (可选) 本地手动构建与启动

如果你不希望使用 Docker 运行微服务，也可以在本地手动启动。

### 5.1 编译打包
在 `backend` 目录下执行 Maven 编译：
```bash
cd backend
mvn clean install -DskipTests
```

### 5.2 启动顺序
建议按照以下顺序启动服务，以确保依赖关系正确：

1.  **sidequest-gateway** (网关, 端口 8080): 统一入口。
2.  **sidequest-identity** (用户/鉴权, 端口 8081): 核心依赖。
3.  **sidequest-core** (社区核心, 端口 8082): 核心业务。
4.  **其他业务服务**:
    *   `sidequest-media` (媒体处理, 端口 8083)
    *   `sidequest-search` (搜索服务, 端口 8084)
    *   `sidequest-moderation` (内容审核, 端口 8085)
    *   `sidequest-analytics` (数据分析, 端口 8086)
    *   `sidequest-chat` (即时通讯, 端口 8087)
    *   `sidequest-mcp` (AI 工具集成, 端口 8087 - *注：本地运行需注意端口冲突*)

### 5.3 启动命令示例
在各服务目录下运行：
```bash
java -jar target/sidequest-xxx-1.0.0-SNAPSHOT.jar
```
或者在 IDE 中直接运行各服务的 `Application` 类。

## 7. 域名与 HTTPS 配置 (生产环境)

如果需要在生产环境配置域名和 HTTPS，请按照以下步骤操作：

### 7.1 准备证书
将你的 SSL 证书文件（`.pem` 和 `.key`）放置在 `infra/docker-compose/nginx/ssl` 目录下：
- `fullchain.pem`
- `privkey.pem`

### 7.2 修改 Nginx 配置
编辑 `infra/nginx/sidequest-mini.conf`：
- 将 `server_name` 修改为你的实际域名。
- 确认 `ssl_certificate` 和 `ssl_certificate_key` 的路径与容器内路径一致（默认为 `/etc/nginx/ssl/...`）。

### 7.3 更新 Docker Compose
确保 `infra/docker-compose/docker-compose.yml` 中的 `nginx` 服务已映射 443 端口并挂载了证书目录。

### 7.4 重启服务
```bash
docker-compose up -d nginx
```

