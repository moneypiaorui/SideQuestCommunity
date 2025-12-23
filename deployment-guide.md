# Side Quest 社区项目部署与数据库指南

## 1. 基础设施准备 (Docker)

使用项目根目录下的 `infra/docker-compose/docker-compose.yml` 启动中间件：

```bash
cd infra/docker-compose
docker-compose up -d
```

包含服务：
- **PostgreSQL 15**: 核心业务库 (端口 5432)
- **Redis 7.0**: 缓存与实时弹幕 (端口 6379)
- **Kafka 3.4**: 事件驱动总线 (端口 9092)
- **Elasticsearch 7.17**: 全文检索 (端口 9200)
- **Nacos 2.2.3**: 配置中心与服务发现 (端口 8848)
- **MinIO**: 对象存储 (端口 9000/9001)
- **ClickHouse**: 分析型数据库 (端口 8123)
- **Grafana LGTM Stack**: 可观测性 (Prometheus: 9090, Grafana: 3000, Loki: 3100, Tempo: 3200)

## 2. 数据库初始化 (PostgreSQL)

连接到 PostgreSQL (`sidequest_db`) 后执行以下 DDL 语句。

### 2.1 用户与身份 (sidequest-identity)
```sql
CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    avatar VARCHAR(255),
    status INT DEFAULT 0, -- 0:正常, 1:封禁
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 社区核心 (sidequest-core)
```sql
CREATE TABLE t_post (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    section_id BIGINT,
    status INT DEFAULT 0, -- 0:发布, 1:草稿, 2:删除
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_comment (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_rating (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score INT CHECK (score >= 1 AND score <= 5),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_section (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    display_name_zh VARCHAR(64),
    display_name_en VARCHAR(64),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 0 -- 0: 正常, 1: 隐藏
);

CREATE TABLE t_tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    hit_count BIGINT DEFAULT 0
);
```

### 2.3 媒体处理 (sidequest-media)
```sql
CREATE TABLE t_media (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_key VARCHAR(255),
    file_type VARCHAR(32),
    author_id BIGINT,
    status INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE t_danmaku (
    id BIGSERIAL PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT,
    time_offset_ms BIGINT,
    color VARCHAR(16),
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

## 5. 微服务启动顺序
1. `sidequest-config` (Nacos)
2. `sidequest-gateway`
3. `sidequest-identity`
4. 其他业务服务 (`core`, `media`, `search`, `moderation`, `analytics`, `mcp`)

