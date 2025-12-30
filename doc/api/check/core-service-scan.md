# Core Service 扫描结果

## 接口完整性检查

### 帖子接口
- ✅ `GET /api/core/posts` - 帖子列表（支持分区、标签筛选）
- ✅ `GET /api/core/posts/{id}` - 帖子详情
- ✅ `POST /api/core/posts` - 创建帖子
- ❌ **缺失**：`PUT /api/core/posts/{id}` - 更新帖子
- ❌ **缺失**：`DELETE /api/core/posts/{id}` - 删除帖子（用户删除自己的）
- ❌ **缺失**：`GET /api/core/posts/my` - 我的帖子列表
- ❌ **缺失**：`GET /api/core/posts/recommended` - 推荐帖子（首页推荐流）

### 分区和标签接口
- ✅ `GET /api/core/sections` - 分区列表
- ✅ `GET /api/core/tags/popular` - 热门标签
- ❌ **缺失**：`GET /api/core/tags` - 所有标签列表
- ❌ **缺失**：`GET /api/core/sections/{id}` - 分区详情

### 互动接口
- ✅ `GET /api/core/interactions/comments` - 获取评论
- ✅ `POST /api/core/interactions/comment` - 添加评论
- ✅ `POST /api/core/interactions/like` - 点赞/取消点赞
- ✅ `POST /api/core/interactions/favorite` - 收藏/取消收藏
- ❌ **缺失**：`DELETE /api/core/interactions/comments/{id}` - 删除评论
- ❌ **缺失**：`POST /api/core/interactions/rating` - 评分（星级评分）
- ❌ **缺失**：`GET /api/core/interactions/comments/{id}/replies` - 评论回复（楼中楼）

### 收藏夹接口
- ✅ `GET /api/core/interactions/favorites` - 我的收藏
- ❌ **缺失**：`POST /api/core/collections` - 创建收藏夹
- ❌ **缺失**：`GET /api/core/collections` - 收藏夹列表
- ❌ **缺失**：`PUT /api/core/collections/{id}` - 更新收藏夹
- ❌ **缺失**：`DELETE /api/core/collections/{id}` - 删除收藏夹
- ❌ **缺失**：`POST /api/core/collections/{id}/items/{postId}` - 移动收藏到收藏夹
- **注意**：虽然`t_favorite`表有`collection_id`字段，但缺少`t_collection`表和相关的CRUD接口

### 管理后台接口
- ✅ `GET /api/admin/posts` - 帖子列表（管理后台）
- ✅ `POST /api/admin/posts/{id}/audit` - 审核帖子
- ✅ `DELETE /api/admin/posts/{id}` - 删除帖子（管理员）
- ❌ **缺失**：`POST /api/admin/posts/{id}/pin` - 置顶
- ❌ **缺失**：`POST /api/admin/posts/{id}/feature` - 加精

## 服务实现检查

### CRUD正确性
- ✅ 发帖时状态设置为`STATUS_AUDITING`（待审核）
- ✅ 审核通过后状态更新为`STATUS_NORMAL`并发送Kafka事件到`post-topic`
- ✅ 审核拒绝后状态更新为`STATUS_BANNED`
- ✅ 点赞/取消点赞使用原子操作更新计数（`setSql("like_count = like_count + 1")`）
- ✅ 评论数、收藏数使用原子操作更新
- ✅ 点赞/评论/收藏操作使用`@Transactional`保证事务一致性
- ✅ 删除帖子时发送`post-delete-topic`事件通知搜索服务

### 性能问题（N+1查询）

#### 严重问题1：帖子列表的点赞/收藏状态查询
**位置**：`PostService.convertToVO()`方法（第72-81行）

**问题描述**：
```java
private PostVO convertToVO(PostDO doItem, String currentUserId) {
    PostVO vo = new PostVO();
    BeanUtils.copyProperties(doItem, vo);
    if (currentUserId != null) {
        Long uid = Long.parseLong(currentUserId);
        // 每个帖子都要查询一次
        vo.setHasLiked(likeMapper.selectCount(...) > 0);
        vo.setHasFavorited(favoriteMapper.selectCount(...) > 0);
    }
    return vo;
}
```

**影响**：
- 如果列表有10个帖子，需要执行20次额外查询（10次点赞查询 + 10次收藏查询）
- 高并发下会导致数据库压力过大

**建议优化**：
1. 批量查询：一次性查询当前用户对所有帖子的点赞/收藏状态
2. 使用Redis缓存：将用户的点赞/收藏关系缓存到Redis Set中

#### 严重问题2：评论列表的用户信息查询
**位置**：`PostService.getComments()`方法（第105-138行）

**问题描述**：
```java
// 虽然代码注释说"模拟批量获取用户信息"，但实际还是逐个调用
Map<Long, IdentityClient.UserDTO> userMap = userIds.stream().collect(Collectors.toMap(
    id -> id,
    id -> {
        try {
            Result<IdentityClient.UserDTO> res = identityClient.getUserById(id);
            return res.getData();
        } catch (Exception e) {
            return null;
        }
    }
));
```

**影响**：
- 如果评论有20条，需要调用20次identity-service
- 网络开销大，响应时间长

**建议优化**：
1. IdentityClient应提供批量接口：`getUsersByIds(List<Long> userIds)`
2. 使用Redis缓存用户信息，减少远程调用

### 缓存使用
- ❌ **问题**：帖子详情**未使用Redis缓存**，每次都查询数据库
- ❌ **问题**：分区列表**未使用Redis缓存**，`getAllSections()`每次都查询数据库
- ❌ **问题**：热门标签**未使用Redis缓存**，`getPopularTags()`每次都查询数据库
- ❌ **问题**：点赞数、评论数、收藏数**未使用Redis计数器**，直接更新数据库

### 消息队列
- ✅ 发帖后发送`post-topic`事件（用于搜索索引）
- ✅ 审核通过后发送`post-topic`事件
- ✅ 删除帖子后发送`post-delete-topic`事件
- ✅ 点赞/评论/收藏操作发送`user-events`事件（用于通知和统计）
- ❌ **问题**：事件格式不统一，部分使用JSON字符串，部分使用简单字符串
- ❌ **问题**：缺少事件版本管理

## 关键问题

### 严重问题（影响生产可用性）

1. **N+1查询问题**：
   - 帖子列表：每个帖子都要查询2次数据库（点赞状态 + 收藏状态）
   - 评论列表：每个评论都要调用1次identity-service
   - **影响**：列表接口性能差，高并发下可能导致数据库和服务间调用压力过大

2. **缺少Redis缓存**：
   - 帖子详情、分区列表、热门标签等热点数据未使用缓存
   - **影响**：数据库压力大，响应时间长

3. **缺少批量接口**：
   - IdentityClient缺少批量获取用户信息接口
   - **影响**：评论列表等场景需要多次调用，性能差

### 功能缺失（影响用户体验）

1. **帖子相关**：
   - 缺少更新帖子接口，用户无法编辑已发布的帖子
   - 缺少"我的帖子"列表接口
   - 缺少推荐流接口，首页无法显示个性化推荐

2. **互动相关**：
   - 缺少删除评论接口
   - 缺少评分功能（计划中有星级评分）
   - 缺少评论回复功能（楼中楼）

3. **收藏夹相关**：
   - 虽然`t_favorite`表有`collection_id`字段，但缺少`t_collection`表
   - 缺少完整的收藏夹管理接口（创建、更新、删除、移动）

4. **管理后台**：
   - 缺少置顶、加精功能

## 优化建议

1. **解决N+1查询问题**：
   - 批量查询点赞/收藏状态：`SELECT post_id FROM t_like WHERE user_id = ? AND post_id IN (...)`
   - 批量查询用户信息：IdentityClient添加`getUsersByIds(List<Long> userIds)`接口

2. **添加Redis缓存**：
   - 帖子详情：`post:detail:{id}`，过期时间1小时
   - 分区列表：`sections:all`，过期时间24小时
   - 热门标签：`tags:popular`，过期时间1小时
   - 点赞/收藏关系：使用Redis Set存储，`user:likes:{userId}`和`user:favorites:{userId}`

3. **使用Redis计数器**：
   - 点赞数、评论数、收藏数使用Redis计数器，定期同步到数据库

4. **补充缺失接口**：
   - 按照计划文档补充所有缺失的接口
   - 创建`t_collection`表并实现收藏夹管理功能

5. **统一事件格式**：
   - 使用统一的JSON事件格式
   - 添加事件版本号字段

