# 前端关键流程验证

## 1. 登录流程

### 流程步骤
1. 用户输入用户名密码 → `POST /api/identity/login`
2. 前端存储token → 后续请求携带token
3. 获取用户信息 → `GET /api/identity/me`

### 问题
- ❌ **缺少token刷新机制**：token过期后需要重新登录
- ❌ **缺少登出接口**：无法主动登出

## 2. 发布帖子流程

### 流程步骤
1. 获取分区列表 → `GET /api/core/sections` ✅
2. 获取热门标签 → `GET /api/core/tags/popular` ✅
3. 上传媒体文件 → `GET /api/media/upload-url` ✅
4. 创建帖子 → `POST /api/core/posts` ✅

### 问题
- ❌ **缺少更新帖子接口**：用户无法编辑已发布的帖子
- ❌ **缺少"我的帖子"列表接口**：用户无法查看自己发布的内容
- ❌ **上传完成后无法通知服务端**：缺少上传完成回调

## 3. 后台审核帖子流程

### 流程步骤
1. 获取待审核帖子列表 → `GET /api/admin/posts?status=0` ✅（STATUS_AUDITING=0）
2. 审核帖子 → `POST /api/admin/posts/{id}/audit?pass=true` ✅

### 问题
- ❌ **缺少审核工单详情接口**：无法查看审核历史
- ❌ **缺少批量审核接口**：无法批量处理

## 4. 后台封禁用户流程

### 流程步骤
1. 获取用户列表 → `GET /api/identity/admin/users` ✅
2. 封禁用户 → `POST /api/identity/admin/users/{id}/ban` ✅

### 问题
- ❌ **缺少解封用户接口**：无法解封用户
- ❌ **封禁操作未记录到ban_record表**：无法追溯封禁历史

## 5. 发送弹幕流程

### 流程步骤
1. 获取帖子详情 → `GET /api/core/posts/{id}` ✅（包含videoUrl）
2. 发送弹幕 → `POST /api/media/danmaku` ✅

### 问题
- ❌ **缺少删除弹幕接口**：用户无法删除自己发送的弹幕
- ❌ **弹幕只存储在Redis**：数据可能丢失

## 6. 观看视频流程

### 流程步骤
1. 获取帖子详情 → `GET /api/core/posts/{id}` ✅
2. 获取弹幕列表 → `GET /api/media/danmaku?videoId={id}&fromMs=0&toMs=60000` ✅

### 问题
- ❌ **缺少视频播放进度上报接口**：无法统计和推荐
- ❌ **视频处理完成后未通知**：帖子中的视频可能一直显示"处理中"

## 总结

所有关键流程基本可用，但存在以下问题：
1. 缺少部分CRUD接口（更新、删除等）
2. 缺少数据持久化（弹幕、审核记录等）
3. 缺少事件通知（视频处理完成等）

