# 接口文档扫描报告

## 扫描概述

本报告详细记录了所有微服务REST API接口的输入参数、输出格式和鉴权要求。

## 鉴权机制

### 网关层鉴权（AuthFilter）
- **JWT Token验证**：所有接口（除开放接口外）需要`Authorization: Bearer <token>`请求头
- **开放接口**（无需Token）：
  - `POST /api/identity/login`
  - `POST /api/identity/register`
  - `/api/public/**`
- **管理接口**（需要ADMIN角色）：
  - `/api/admin/**`
- **网关转发**：验证通过后添加`X-User-Id`和`X-User-Role`请求头到下游服务

### 服务层用户上下文
- 各服务通过`UserContextInterceptor`从`X-User-Id`获取当前用户ID
- 使用`UserContext.getUserId()`获取用户ID（可能为null，表示未登录用户）

### 统一响应格式
```json
{
  "code": 200,        // 200=成功, 其他=错误码
  "message": "success", // "success"或错误信息
  "data": {}         // 响应数据，类型根据接口而定
}
```

---

## 接口列表

### 1. Identity Service (`/api/identity`)

#### 1.1 认证接口

##### POST /api/identity/register - 用户注册
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **请求体**：
```json
{
  "username": "string",    // 必填，用户名
  "password": "string",    // 必填，密码
  "nickname": "string"     // 必填，昵称
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Registration successful"
}
```

##### POST /api/identity/login - 用户登录
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **请求体**：
```json
{
  "username": "string",    // 必填，用户名
  "password": "string"     // 必填，密码
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "string",        // JWT Token
    "expireIn": 86400,        // 过期时间（秒）
    "userId": 1,              // 用户ID
    "nickname": "string",     // 昵称
    "avatar": "string"        // 头像URL
  }
}
```

#### 1.2 用户接口

##### GET /api/identity/me - 获取当前用户信息
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "string",
    "password": "string",      // 注意：实际应隐藏密码字段
    "nickname": "string",
    "avatar": "string",
    "role": "USER",            // USER, ADMIN
    "status": 0,               // 0: NORMAL, 1: BANNED, 2: DELETED
    "followerCount": 0,
    "followingCount": 0,
    "totalLikedCount": 0,
    "postCount": 0,
    "createTime": "2024-01-01T00:00:00"
  }
}
```
- **问题**：响应中包含密码字段，存在安全隐患

##### GET /api/identity/users/{id}/public - 获取用户公开信息
- **鉴权要求**：可选Token（未登录也可访问，但无法获取关注状态）
- **请求头**：`Authorization: Bearer <token>`（可选）
- **路径参数**：
  - `id` (Long): 用户ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "nickname": "string",
    "avatar": "string",
    "role": "USER",
    "followerCount": 0,
    "followingCount": 0,
    "totalLikedCount": 0,
    "postCount": 0,
    "isFollowing": false       // 仅当已登录且有关注关系时返回
  }
}
```

##### POST /api/identity/users/{id}/follow - 关注用户
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 要关注的用户ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Followed"
}
```

##### POST /api/identity/users/{id}/unfollow - 取消关注
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 要取消关注的用户ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Unfollowed"
}
```

##### GET /api/identity/users/{id} - 获取用户详情（管理接口）
- **鉴权要求**：需要Token（注意：代码中未检查管理员权限，但路径不在/admin下）
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 用户ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "string",
    "password": "string",      // 注意：包含敏感信息
    "nickname": "string",
    "avatar": "string",
    "role": "USER",
    "status": 0,
    "followerCount": 0,
    "followingCount": 0,
    "totalLikedCount": 0,
    "postCount": 0,
    "createTime": "2024-01-01T00:00:00"
  }
}
```
- **问题**：响应包含密码字段，存在安全隐患

##### PUT /api/identity/profile - 更新个人资料
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "nickname": "string",    // 可选
  "avatar": "string"       // 可选
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Profile updated"
}
```

##### GET /api/identity/admin/users - 获取用户列表（管理后台）
- **鉴权要求**：需要Token + ADMIN角色
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `current` (int, 默认1): 当前页码
  - `size` (int, 默认10): 每页数量
  - `status` (Integer, 可选): 用户状态筛选
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "string",
        "password": "string",      // 注意：包含敏感信息
        "nickname": "string",
        "avatar": "string",
        "role": "USER",
        "status": 0,
        "followerCount": 0,
        "followingCount": 0,
        "totalLikedCount": 0,
        "postCount": 0,
        "createTime": "2024-01-01T00:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```
- **问题**：响应包含密码字段，存在安全隐患

##### POST /api/identity/admin/users/{id}/ban - 封禁用户（管理后台）
- **鉴权要求**：需要Token + ADMIN角色
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 用户ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "User banned"
}
```
- **问题**：代码注释显示"应该检查管理员角色"，但实际未实现角色验证

#### 1.3 通知接口

##### GET /api/notifications/unread-count - 获取未读通知数量
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "chat": 0,           // 私信未读
    "interaction": 0,    // 互动提醒（点赞、评论、收藏）
    "system": 0          // 系统公告
  }
}
```

##### POST /api/notifications/mark-read - 标记通知为已读
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `type` (String): 通知类型
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Marked as read"
}
```

---

### 2. Core Service (`/api/core`)

#### 2.1 帖子接口

##### GET /api/core/posts - 获取帖子列表
- **鉴权要求**：可选Token（未登录也可访问，但无法获取点赞/收藏状态）
- **请求头**：`Authorization: Bearer <token>`（可选）
- **查询参数**：
  - `current` (int, 默认1): 当前页码
  - `size` (int, 默认10): 每页数量
  - `sectionId` (Long, 可选): 分区ID筛选
  - `tag` (String, 可选): 标签筛选
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "authorId": 1,
        "authorName": "string",
        "title": "string",
        "content": "string",
        "sectionId": 1,
        "status": 1,              // 0: AUDITING, 1: NORMAL, 2: BANNED, 3: DELETED
        "likeCount": 0,
        "commentCount": 0,
        "favoriteCount": 0,
        "viewCount": 0,
        "createTime": "2024-01-01T00:00:00",
        "updateTime": "2024-01-01T00:00:00",
        "imageUrls": "string",     // JSON字符串数组
        "videoUrl": "string",
        "videoCoverUrl": "string",
        "videoDuration": 0,
        "tags": "string",          // JSON字符串数组
        "hasLiked": false,         // 仅当已登录时返回
        "hasFavorited": false      // 仅当已登录时返回
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

##### GET /api/core/posts/{id} - 获取帖子详情
- **鉴权要求**：可选Token（未登录也可访问，但无法获取点赞/收藏状态）
- **请求头**：`Authorization: Bearer <token>`（可选）
- **路径参数**：
  - `id` (Long): 帖子ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "authorId": 1,
    "authorName": "string",
    "title": "string",
    "content": "string",
    "sectionId": 1,
    "status": 1,
    "likeCount": 0,
    "commentCount": 0,
    "favoriteCount": 0,
    "viewCount": 0,
    "createTime": "2024-01-01T00:00:00",
    "updateTime": "2024-01-01T00:00:00",
    "imageUrls": "string",
    "videoUrl": "string",
    "videoCoverUrl": "string",
    "videoDuration": 0,
    "tags": "string",
    "hasLiked": false,
    "hasFavorited": false
  }
}
```

##### POST /api/core/posts - 创建帖子
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "title": "string",              // 必填
  "content": "string",            // 必填
  "sectionId": 1,                // 可选
  "tags": ["tag1", "tag2"],      // 可选，标签数组
  "imageUrls": ["url1", "url2"], // 可选，图片URL数组
  "videoUrl": "string",          // 可选，视频URL
  "videoCoverUrl": "string",     // 可选，视频封面URL
  "videoDuration": 0             // 可选，视频时长（秒）
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Post created successfully"
}
```
- **说明**：创建后会自动触发审核和搜索索引同步

##### GET /api/core/sections - 获取分区列表
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "string",
      "displayNameZh": "string",
      "displayNameEn": "string",
      "status": 0                 // 0: Normal, 1: Hidden
    }
  ]
}
```

##### GET /api/core/tags/popular - 获取热门标签
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "string",
      "hit_count": 100
    }
  ]
}
```

#### 2.2 互动接口

##### GET /api/core/interactions/comments - 获取评论列表
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **查询参数**：
  - `postId` (Long): 帖子ID（必填）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "postId": 1,
      "userId": 1,
      "content": "string",
      "parentId": null,           // 父评论ID，null表示顶级评论
      "createTime": "2024-01-01T00:00:00",
      "nickname": "string",       // 用户昵称（从identity-service获取）
      "avatar": "string"          // 用户头像（从identity-service获取）
    }
  ]
}
```

##### POST /api/core/interactions/comment - 添加评论
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `postId` (Long): 帖子ID（必填）
- **请求体**：
```json
"string"                         // 评论内容（直接是字符串，不是JSON对象）
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Comment added"
}
```
- **问题**：请求体格式不一致，应该使用JSON对象

##### POST /api/core/interactions/like - 点赞/取消点赞
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `postId` (Long): 帖子ID（必填）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Operation successful"
}
```
- **说明**：重复调用会切换点赞状态

##### POST /api/core/interactions/favorite - 收藏/取消收藏
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `postId` (Long): 帖子ID（必填）
  - `collectionId` (Long, 可选): 收藏夹ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Favorited successfully"
}
```
- **说明**：重复调用会切换收藏状态

##### GET /api/core/interactions/favorites - 获取我的收藏
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "postId": 1,
      "userId": 1,
      "collectionId": null,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

#### 2.3 管理后台接口

##### GET /api/admin/posts - 获取帖子列表（管理后台）
- **鉴权要求**：需要Token + ADMIN角色
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `current` (int, 默认1): 当前页码
  - `size` (int, 默认10): 每页数量
  - `status` (Integer, 可选): 状态筛选（0: AUDITING, 1: NORMAL, 2: BANNED, 3: DELETED）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "authorId": 1,
        "authorName": "string",
        "title": "string",
        "content": "string",
        "sectionId": 1,
        "status": 0,
        "likeCount": 0,
        "commentCount": 0,
        "favoriteCount": 0,
        "viewCount": 0,
        "createTime": "2024-01-01T00:00:00",
        "updateTime": "2024-01-01T00:00:00",
        "imageUrls": "string",
        "videoUrl": "string",
        "videoCoverUrl": "string",
        "videoDuration": 0,
        "tags": "string"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

##### POST /api/admin/posts/{id}/audit - 审核帖子（管理后台）
- **鉴权要求**：需要Token + ADMIN角色
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 帖子ID
- **查询参数**：
  - `pass` (boolean): 是否通过审核
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Post approved"  // 或 "Post rejected"
}
```

##### DELETE /api/admin/posts/{id} - 删除帖子（管理后台）
- **鉴权要求**：需要Token + ADMIN角色
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 帖子ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Post deleted"
}
```

---

### 3. Media Service (`/api/media`)

##### GET /api/media/list - 获取用户的媒体列表
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `authorId` (Long): 作者ID（必填）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "fileName": "string",
      "fileKey": "string",
      "fileType": "image",        // image, video
      "url": "string",
      "authorId": 1,
      "status": 1,                // 0: PROCESSING, 1: READY, 2: FAILED
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

##### GET /api/media/upload-url - 获取上传URL
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **查询参数**：
  - `fileName` (String): 文件名（必填）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "https://oss.example.com/presigned-url"
}
```
- **说明**：返回OSS预签名URL，用于前端直传

##### GET /api/media/status/{id} - 获取媒体处理状态
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `id` (Long): 媒体ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": 1                      // 0: PROCESSING, 1: READY, 2: FAILED
}
```

##### POST /api/media/danmaku - 发送弹幕
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "videoId": 1,                  // 必填，视频ID
  "userId": 1,                   // 必填，用户ID（应从Token获取，不应由客户端传递）
  "content": "string",           // 必填，弹幕内容
  "timeOffsetMs": 1000,          // 必填，时间偏移（毫秒）
  "color": "#FFFFFF"             // 可选，弹幕颜色
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Danmaku sent"
}
```
- **问题**：请求体中包含userId，应该从Token中获取，存在安全隐患
- **说明**：弹幕存储在Redis ZSet中，key为`danmaku:{videoId}`，score为时间偏移

##### GET /api/media/danmaku - 获取弹幕列表
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **查询参数**：
  - `videoId` (Long): 视频ID（必填）
  - `fromMs` (Long): 起始时间（毫秒，必填）
  - `toMs` (Long): 结束时间（毫秒，必填）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "videoId": 1,
      "userId": 1,
      "content": "string",
      "timeOffsetMs": 1000,
      "color": "#FFFFFF"
    }
  ]
}
```

---

### 4. Search Service (`/api/search`)

##### GET /api/search/posts - 搜索帖子
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **查询参数**：
  - `keyword` (String): 搜索关键词（必填）
  - `page` (int, 默认0): 页码（从0开始）
  - `size` (int, 默认10): 每页数量
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": "1",
        "title": "string",
        "content": "string",
        "authorName": "string",
        "sectionId": 1,
        "status": 1,
        "likeCount": 0,
        "commentCount": 0,
        "favoriteCount": 0,
        "viewCount": 0,
        "createTime": 1704067200000
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0
  }
}
```
- **说明**：使用Elasticsearch全文搜索，按创建时间倒序排列

##### GET /api/search/user/posts - 搜索用户的帖子
- **鉴权要求**：公开接口，无需Token
- **请求头**：无特殊要求
- **查询参数**：
  - `userId` (Long): 用户ID（必填）
  - `page` (int, 默认0): 页码（从0开始）
  - `size` (int, 默认10): 每页数量
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": "1",
        "title": "string",
        "content": "string",
        "authorName": "string",
        "sectionId": 1,
        "status": 1,
        "likeCount": 0,
        "commentCount": 0,
        "favoriteCount": 0,
        "viewCount": 0,
        "createTime": 1704067200000
      }
    ],
    "totalElements": 10,
    "totalPages": 1,
    "size": 10,
    "number": 0
  }
}
```

---

### 5. Moderation Service (`/api/moderation`)

##### POST /api/moderation/check - 文本审核
- **鉴权要求**：需要Token（通常由其他服务内部调用）
- **请求头**：`Authorization: Bearer <token>`
- **请求体**：
```json
"string"                         // 待审核的文本内容（直接是字符串，不是JSON对象）
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": true                   // true=通过, false=不通过
}
```
- **问题**：请求体格式不一致，应该使用JSON对象
- **说明**：使用敏感词匹配进行审核，敏感词从配置文件读取

---

### 6. Chat Service (`/api/chat`)

##### GET /api/chat/rooms - 获取聊天室列表
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "string",              // 聊天室名称或对方昵称
      "type": "PRIVATE",              // PRIVATE, GROUP
      "lastMessage": "string",        // 最后一条消息
      "lastMessageTime": "2024-01-01T00:00:00",
      "unreadCount": 0,
      "recipientNickname": "string",  // 对方昵称（私聊时）
      "recipientAvatar": "string"     // 对方头像（私聊时）
    }
  ]
}
```

##### GET /api/chat/rooms/{roomId}/messages - 获取消息列表
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `roomId` (Long): 聊天室ID
- **查询参数**：
  - `sinceId` (Long, 可选): 起始消息ID（用于增量获取）
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "roomId": 1,
      "senderId": 1,
      "content": "string",
      "type": "TEXT",                // TEXT, IMAGE, etc.
      "status": 1,                    // 0: UNREAD, 1: READ
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

##### POST /api/chat/rooms/{roomId}/send - 发送消息
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `roomId` (Long): 聊天室ID
- **请求体**：
```json
{
  "content": "string"                // 必填，消息内容
}
```
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "roomId": 1,
    "senderId": 1,
    "content": "string",
    "type": "TEXT",
    "status": 0,
    "createTime": "2024-01-01T00:00:00"
  }
}
```

##### POST /api/chat/rooms/{roomId}/read - 标记消息为已读
- **鉴权要求**：需要Token
- **请求头**：`Authorization: Bearer <token>`
- **路径参数**：
  - `roomId` (Long): 聊天室ID
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "Marked as read"
}
```

---

### 7. Analytics Service (`/api/analytics`)

##### GET /api/analytics/dashboard/stats - 获取统计概览
- **鉴权要求**：需要Token + ADMIN角色（建议）
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total_events": 1000
  }
}
```
- **说明**：从ClickHouse查询事件统计

##### GET /api/analytics/dashboard/top-posts - 获取热门帖子
- **鉴权要求**：需要Token + ADMIN角色（建议）
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**：无
- **响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "event_data": "{\"postId\":1}",
      "count": 100
    }
  ]
}
```
- **说明**：返回浏览量Top 10的帖子

---

### 8. MCP Service (`/api/mcp`)

##### POST /api/mcp/rpc - JSON-RPC接口
- **鉴权要求**：需要Token（建议）
- **请求头**：`Authorization: Bearer <token>`
- **请求体**：
```json
{
  "jsonrpc": "2.0",
  "method": "list_tools",        // 或 "call_tool"
  "params": {},                   // 方法参数
  "id": "1"                       // 请求ID
}
```
- **响应**：
```json
{
  "jsonrpc": "2.0",
  "result": {},                  // 方法返回结果
  "error": null,                 // 错误信息（如有）
  "id": "1"
}
```
- **支持的方法**：
  - `list_tools`: 列出所有可用工具
  - `call_tool`: 调用工具
    - 参数：
      - `name` (String): 工具名称（如 "create_post", "add_comment"）
      - `arguments` (Object): 工具参数
- **示例调用create_post**：
```json
{
  "jsonrpc": "2.0",
  "method": "call_tool",
  "params": {
    "name": "create_post",
    "arguments": {
      "title": "string",
      "content": "string",
      "sectionId": 1
    }
  },
  "id": "1"
}
```
- **示例调用add_comment**：
```json
{
  "jsonrpc": "2.0",
  "method": "call_tool",
  "params": {
    "name": "add_comment",
    "arguments": {
      "postId": 1,
      "content": "string"
    }
  },
  "id": "1"
}
```

---

## 鉴权总结

### 开放接口列表（无需Token）
- `POST /api/identity/register` - 用户注册
- `POST /api/identity/login` - 用户登录
- `GET /api/core/sections` - 获取分区列表
- `GET /api/core/tags/popular` - 获取热门标签
- `GET /api/core/posts` - 获取帖子列表（可选Token）
- `GET /api/core/posts/{id}` - 获取帖子详情（可选Token）
- `GET /api/core/interactions/comments` - 获取评论列表
- `GET /api/search/posts` - 搜索帖子
- `GET /api/search/user/posts` - 搜索用户的帖子
- `GET /api/media/danmaku` - 获取弹幕列表

### 需要认证的接口列表（需要Token）
- `GET /api/identity/me` - 获取当前用户信息
- `GET /api/identity/users/{id}/public` - 获取用户公开信息（可选Token）
- `POST /api/identity/users/{id}/follow` - 关注用户
- `POST /api/identity/users/{id}/unfollow` - 取消关注
- `GET /api/identity/users/{id}` - 获取用户详情
- `PUT /api/identity/profile` - 更新个人资料
- `GET /api/notifications/unread-count` - 获取未读通知数量
- `POST /api/notifications/mark-read` - 标记通知为已读
- `POST /api/core/posts` - 创建帖子
- `POST /api/core/interactions/comment` - 添加评论
- `POST /api/core/interactions/like` - 点赞/取消点赞
- `POST /api/core/interactions/favorite` - 收藏/取消收藏
- `GET /api/core/interactions/favorites` - 获取我的收藏
- `GET /api/media/list` - 获取用户的媒体列表
- `GET /api/media/upload-url` - 获取上传URL
- `GET /api/media/status/{id}` - 获取媒体处理状态
- `POST /api/media/danmaku` - 发送弹幕
- `POST /api/moderation/check` - 文本审核
- `GET /api/chat/rooms` - 获取聊天室列表
- `GET /api/chat/rooms/{roomId}/messages` - 获取消息列表
- `POST /api/chat/rooms/{roomId}/send` - 发送消息
- `POST /api/chat/rooms/{roomId}/read` - 标记消息为已读
- `GET /api/analytics/dashboard/stats` - 获取统计概览
- `GET /api/analytics/dashboard/top-posts` - 获取热门帖子
- `POST /api/mcp/rpc` - JSON-RPC接口

### 需要管理员角色的接口列表（需要ADMIN角色）
- `GET /api/identity/admin/users` - 获取用户列表（管理后台）
- `POST /api/identity/admin/users/{id}/ban` - 封禁用户（管理后台）
- `GET /api/admin/posts` - 获取帖子列表（管理后台）
- `POST /api/admin/posts/{id}/audit` - 审核帖子（管理后台）
- `DELETE /api/admin/posts/{id}` - 删除帖子（管理后台）

---

## 问题汇总

### 严重安全问题

1. **密码字段泄露**
   - **接口**：`GET /api/identity/me`、`GET /api/identity/users/{id}`、`GET /api/identity/admin/users`
   - **问题**：响应中包含用户密码字段，存在严重安全隐患
   - **影响**：密码泄露可能导致账户被盗用
   - **建议**：使用DTO类过滤敏感字段，不要在响应中返回密码

2. **用户ID由客户端传递**
   - **接口**：`POST /api/media/danmaku`
   - **问题**：请求体中包含userId，应该从Token中获取
   - **影响**：用户可以伪造其他用户的身份发送弹幕
   - **建议**：从`UserContext.getUserId()`获取用户ID

3. **管理员权限验证不完整**
   - **接口**：`POST /api/identity/admin/users/{id}/ban`
   - **问题**：代码注释显示"应该检查管理员角色"，但实际未实现角色验证
   - **影响**：普通用户可能可以执行管理员操作
   - **建议**：在Controller层添加`@PreAuthorize("hasRole('ADMIN')")`注解或手动验证角色

### 接口设计问题

4. **请求体格式不一致**
   - **接口**：
     - `POST /api/core/interactions/comment` - 请求体直接是字符串
     - `POST /api/moderation/check` - 请求体直接是字符串
   - **问题**：应该使用JSON对象，便于扩展和维护
   - **建议**：统一使用JSON对象格式，如：
     ```json
     {
       "content": "string"
     }
     ```

5. **缺少参数验证**
   - **问题**：大部分接口缺少参数验证注解（如`@NotNull`、`@NotBlank`、`@Min`、`@Max`等）
   - **影响**：可能接收到无效数据，导致业务逻辑错误
   - **建议**：在DTO类中添加Bean Validation注解

6. **缺少分页参数验证**
   - **问题**：分页接口未验证`current`和`size`的范围
   - **影响**：可能传入负数或超大值，导致性能问题
   - **建议**：添加`@Min(1)`和`@Max(100)`等验证

### 鉴权问题

7. **可选Token接口的鉴权逻辑不清晰**
   - **接口**：`GET /api/core/posts`、`GET /api/core/posts/{id}`、`GET /api/identity/users/{id}/public`
   - **问题**：这些接口支持可选Token，但代码中通过`UserContext.getUserId()`判断，如果为null则无法获取用户相关状态
   - **建议**：明确文档说明哪些功能需要登录，哪些不需要

8. **Analytics接口缺少鉴权要求**
   - **接口**：`GET /api/analytics/dashboard/stats`、`GET /api/analytics/dashboard/top-posts`
   - **问题**：统计接口应该需要管理员权限，但代码中未明确
   - **建议**：添加管理员角色验证

### 数据格式问题

9. **JSON字符串字段**
   - **接口**：`GET /api/core/posts`、`GET /api/core/posts/{id}`
   - **问题**：`imageUrls`和`tags`字段返回的是JSON字符串，而不是数组
   - **影响**：前端需要额外解析，增加复杂度
   - **建议**：直接返回数组类型

10. **时间戳格式不一致**
    - **问题**：部分接口返回`LocalDateTime`（ISO格式），部分返回时间戳（毫秒）
    - **影响**：前端处理时间格式不统一
    - **建议**：统一使用ISO 8601格式或统一使用时间戳

### 功能缺失

11. **缺少接口文档中提到的功能**
    - 根据之前的扫描报告，缺少以下接口：
      - 帖子更新、删除接口
      - 评论删除接口
      - 敏感词管理接口
      - 举报功能接口
      - Token刷新接口
      - 密码重置接口

### 建议改进

1. **统一错误响应格式**：确保所有错误响应都使用`Result.error()`格式
2. **添加接口版本控制**：考虑使用`/api/v1/`前缀
3. **添加请求ID追踪**：在响应头中添加请求ID，便于问题排查
4. **完善接口文档**：考虑使用Swagger/OpenAPI自动生成文档
5. **添加接口限流**：对敏感接口添加限流保护
6. **统一日期时间格式**：所有接口统一使用ISO 8601格式
7. **添加接口监控**：记录接口调用次数、响应时间、错误率等指标

