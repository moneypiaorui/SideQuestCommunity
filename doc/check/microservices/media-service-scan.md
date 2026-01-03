# Media Service 扫描结果

## 接口完整性检查

### 媒体上传接口
- ✅ `GET /api/media/upload-url` - 获取上传签名URL
- ✅ `GET /api/media/list` - 获取我的媒体列表
- ✅ `GET /api/media/status/{id}` - 获取处理状态
- ❌ **缺失**：`POST /api/media/complete` - 上传完成回调（通知服务端）
- ❌ **缺失**：`GET /api/media/{id}` - 获取媒体详情
- ❌ **缺失**：`DELETE /api/media/{id}` - 删除媒体

### 弹幕接口
- ✅ `POST /api/media/danmaku` - 发送弹幕
- ✅ `GET /api/media/danmaku` - 获取弹幕（按时间范围）
- ❌ **缺失**：`DELETE /api/media/danmaku/{id}` - 删除弹幕（用户删除自己的）

## 服务实现检查

### CRUD正确性
- ✅ 上传URL生成使用MinIO预签名URL，15分钟有效期
- ✅ 视频处理状态更新逻辑正确（PROCESSING → READY/FAILED）
- ✅ 弹幕存储使用Redis ZSet，score为时间偏移（`danmaku:{videoId}`）
- ❌ **问题**：弹幕**只存储在Redis中，没有持久化到数据库**
- ❌ **问题**：缺少上传完成后的处理流程（创建Media记录、触发转码）

### 缓存使用
- ✅ 弹幕使用Redis ZSet存储（热数据），符合设计
- ❌ **问题**：弹幕**缺少冷数据归档机制**（MySQL/ClickHouse），Redis数据可能丢失
- ❌ **问题**：媒体信息**未使用Redis缓存**，`getStatus`每次都查询数据库

### 消息队列
- ✅ 视频处理使用Kafka事件驱动（`video-process-topic`）
- ✅ 视频处理消费者正确实现（`VideoProcessConsumer`）
- ❌ **问题**：视频处理完成后**未发送Kafka事件**通知其他服务（如core-service更新帖子中的视频URL）
- ❌ **问题**：上传完成后**未发送`media.uploaded`事件**（计划文档中要求）

## 关键问题

### 严重问题（影响生产可用性）

1. **弹幕数据丢失风险**：
   - 弹幕只存储在Redis中，没有持久化
   - Redis重启或内存不足时数据会丢失
   - **影响**：用户发送的弹幕可能永久丢失

2. **缺少上传完成回调**：
   - 客户端上传完成后无法通知服务端
   - 服务端无法创建Media记录，无法触发转码流程
   - **影响**：上传流程不完整

3. **视频处理完成未通知**：
   - 转码完成后只更新了状态，未发送Kafka事件
   - 其他服务（如core-service）无法感知视频已就绪
   - **影响**：帖子中的视频可能一直显示"处理中"

### 功能缺失（影响用户体验）

1. **媒体管理**：
   - 缺少获取媒体详情接口
   - 缺少删除媒体接口

2. **弹幕管理**：
   - 缺少删除弹幕接口（用户无法删除自己发送的弹幕）

3. **视频播放**：
   - 缺少视频播放进度上报接口（用于统计和推荐）

## 优化建议

1. **弹幕持久化**：
   - 添加`t_danmaku`表存储弹幕
   - 实现双写：Redis（热数据）+ MySQL（冷数据）
   - 或使用ClickHouse存储大量弹幕数据

2. **完善上传流程**：
   - 添加上传完成回调接口：`POST /api/media/complete`
   - 回调时创建Media记录，触发转码流程
   - 发送`media.uploaded`事件

3. **视频处理完成通知**：
   - 转码完成后发送Kafka事件：`media.processed`，包含视频URL、封面URL等信息
   - 供core-service等消费，更新帖子中的视频信息

4. **添加媒体缓存**：
   - 媒体详情使用Redis缓存：`media:detail:{id}`
   - 媒体状态使用Redis缓存：`media:status:{id}`

5. **补充缺失接口**：
   - 添加媒体详情、删除接口
   - 添加弹幕删除接口
   - 添加视频播放进度上报接口

