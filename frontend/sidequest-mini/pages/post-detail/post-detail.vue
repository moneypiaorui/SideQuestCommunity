<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <view class="custom-header sticky-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="15 18 9 12 15 6"></polyline></svg></view>
        <text class="title">{{ post.authorName }}的笔记</text>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll">
      <view class="post-detail brutal-card">
        <!-- 图片容器：支持自适应填充 -->
        <view class="media-box">
          <swiper circular indicator-dots class="media-swiper" :style="{ height: swiperHeight }">
            <swiper-item v-for="(url, i) in post.imageUrls" :key="i">
              <image :src="url" mode="aspectFit" class="post-image" />
            </swiper-item>
          </swiper>
        </view>
        
        <view class="author-row">
          <view class="author-info" @click="goToUserProfile(post.authorId)">
            <image :src="post.authorAvatar" class="avatar brutal-card" />
            <view class="details">
              <text class="nickname">{{ post.authorName }}</text>
              <text class="time">12-24</text>
            </view>
          </view>
          <!-- 自己不显示关注按钮 -->
          <view v-if="post.authorId && String(post.authorId) !== String(currentUserId)" 
                class="follow-btn brutal-btn" 
                :class="{ 'active': post.isFollowing }" 
                @click="handleFollow">
            {{ post.isFollowing ? '已关注' : '关注' }}
          </view>
        </view>
        
        <view class="content-box">
          <text class="post-title">{{ post.title }}</text>
          <text class="post-content">{{ post.content }}</text>
          
          <view v-if="post.tags && post.tags.length" class="tag-list">
            <view v-for="tag in post.tags" :key="tag" class="tag-item brutal-btn" @click="goToTagSearch(tag)">
              # {{ tag }}
            </view>
          </view>
        </view>
      </view>
      
      <!-- 评论区修复 -->
      <view class="comments-section brutal-card">
        <text class="section-title">评论 ({{ comments.length }})</text>
        <view class="comments-list">
          <view v-if="comments.length === 0" class="empty-comments">暂无评论，快来抢沙发~</view>
          <view v-for="c in comments" :key="c.id" class="comment-item">
            <image :src="c.avatar" class="comment-avatar brutal-card" @click="goToUserProfile(c.userId)" />
            <view class="comment-body">
              <text class="comment-user">{{ c.nickname }}</text>
              <text class="comment-text">{{ c.content }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="safe-area-bottom" style="height: 140rpx;" />
    </scroll-view>
    
    <view class="footer safe-area-bottom brutal-card">
      <view class="comment-input brutal-btn" @click="showCommentPopup = true">
        <text class="placeholder">说点什么...</text>
      </view>
      <view class="action-btn" @click="handleLike">
        <svg width="24" height="24" viewBox="0 0 24 24" :fill="post.hasLiked ? 'var(--accent-red)' : 'none'" :stroke="post.hasLiked ? 'var(--accent-red)' : 'currentColor'" stroke-width="3">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
        </svg>
        <text class="count">{{ post.likeCount }}</text>
      </view>
      <view class="action-btn" @click="handleFavorite">
        <svg width="24" height="24" viewBox="0 0 24 24" :fill="post.hasFavorited ? '#FFB800' : 'none'" :stroke="post.hasFavorited ? '#FFB800' : 'currentColor'" stroke-width="3">
          <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
        </svg>
        <text class="count">{{ post.favoriteCount || 0 }}</text>
      </view>
    </view>
    
    <!-- 评论输入弹窗 -->
    <view v-if="showCommentPopup" class="comment-popup-mask" @click="showCommentPopup = false">
      <view class="comment-popup-content brutal-card" @click.stop>
        <textarea 
          v-model="commentContent" 
          class="brutal-textarea" 
          placeholder="既然来了，就留个言吧..." 
          fixed
          auto-focus
          cursor-spacing="40"
        />
        <view class="popup-footer">
          <view class="brutal-btn primary send-btn" :loading="submittingComment" @click="submitComment">发布</view>
        </view>
      </view>
    </view>

    <LoginModal />
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/utils/request'
import { bus } from '@/utils/bus'

const isDark = ref(uni.getStorageSync('isDark') || false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const post = ref({})
const comments = ref([])
const showCommentPopup = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)
const currentUserId = ref(uni.getStorageSync('userId'))

// 动态计算 Swiper 高度，最小 3:4
const swiperHeight = computed(() => {
  const match = post.value.imageUrls?.[0]?.match(/_w(\d+)_h(\d+)/)
  if (match) {
    const w = parseInt(match[1]); const h = parseInt(match[2])
    const ratio = h / w
    // 限制高度范围：最小 75vw (3:4)，最大充满屏幕可用高度
    return `max(75vw, min(80vh, ${100 * ratio}vw))`
  }
  return '800rpx'
})

onMounted(async () => {
  uni.$on('loginSuccess', () => {
    currentUserId.value = uni.getStorageSync('userId')
  })
  const pages = getCurrentPages(); const options = pages[pages.length - 1].options
  const id = options.id || 1
  
  // 支持从列表带入初始状态
  if (options.hasLiked !== undefined) post.value.hasLiked = options.hasLiked === 'true'
  if (options.likeCount !== undefined) post.value.likeCount = parseInt(options.likeCount)
  if (options.hasFavorited !== undefined) post.value.hasFavorited = options.hasFavorited === 'true'
  if (options.favoriteCount !== undefined) post.value.favoriteCount = parseInt(options.favoriteCount)

  try {
    const res = await request({ url: `/api/core/posts/${id}` })
    // 适配后端不同的序列化字段名
    res.isFollowing = res.isFollowing || res.following || false
    post.value = { ...post.value, ...res } // 合并数据，保留初始状态直到加载完成
    comments.value = await request({ url: `/api/core/interactions/comments?postId=${id}` }) || []
  } catch (err) {}
})

const handleLike = async () => {
  const token = uni.getStorageSync('token'); if (!token) { bus.openLogin(); return }
  post.value.hasLiked = !post.value.hasLiked
  post.value.hasLiked ? post.value.likeCount++ : post.value.likeCount--
  uni.$emit('updatePostStatus', { id: post.value.id, hasLiked: post.value.hasLiked, likeCount: post.value.likeCount })
  await request({ url: `/api/core/interactions/like?postId=${post.value.id}`, method: 'POST' })
}

const handleFavorite = async () => {
  const token = uni.getStorageSync('token'); if (!token) { bus.openLogin(); return }
  post.value.hasFavorited = !post.value.hasFavorited
  post.value.hasFavorited ? (post.value.favoriteCount = (post.value.favoriteCount || 0) + 1) : (post.value.favoriteCount--)
  uni.$emit('updatePostStatus', { id: post.value.id, hasFavorited: post.value.hasFavorited, favoriteCount: post.value.favoriteCount })
  await request({ url: `/api/core/interactions/favorite?postId=${post.value.id}`, method: 'POST' })
}

const handleFollow = async () => {
  const token = uni.getStorageSync('token'); if (!token) { bus.openLogin(); return }
  
  const originalState = post.value.isFollowing
  try {
    // 先改状态（为了流畅感，但需要处理失败）
    post.value.isFollowing = !originalState
    await request({ 
      url: post.value.isFollowing ? `/api/identity/users/${post.value.authorId}/follow` : `/api/identity/users/${post.value.authorId}/unfollow`, 
      method: 'POST' 
    })
  } catch (err) {
    // 失败则回退
    post.value.isFollowing = originalState
    if (err.message && err.message.includes('cannot follow yourself')) {
      uni.showToast({ title: '不能关注自己', icon: 'none' })
    }
  }
}

const goToUserProfile = (id) => uni.navigateTo({ url: `/pages/user-profile/user-profile?userId=${id}` })
const goToTagSearch = (tag) => uni.navigateTo({ url: `/pages/search/search?keyword=${encodeURIComponent(tag)}` })
const goBack = () => uni.navigateBack()

const submitComment = async () => {
  if (!commentContent.value.trim()) return
  const token = uni.getStorageSync('token')
  if (!token) { bus.openLogin(); return }

  submittingComment.value = true
  try {
    await request({
      url: '/api/core/interactions/comment',
      method: 'POST',
      data: {
        postId: post.value.id,
        content: commentContent.value
      }
    })
    
    uni.showToast({ title: '评论成功', icon: 'success' })
    commentContent.value = ''
    showCommentPopup.value = false
    
    // 刷新评论列表
    comments.value = await request({ url: `/api/core/interactions/comments?postId=${post.value.id}` })
    post.value.commentCount = (post.value.commentCount || 0) + 1
  } catch (err) {
  } finally {
    submittingComment.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-container { height: 100vh; display: flex; flex-direction: column; background: var(--bg-main); }
.sticky-header { position: sticky; top: 0; z-index: 1000; }
.custom-header { background: var(--surface); border-bottom: 4rpx solid #000; z-index: 100; .nav-bar { height: 100rpx; display: flex; align-items: center; padding: 0 30rpx; gap: 30rpx; .back-btn { width: 70rpx; height: 70rpx; border-radius: 16rpx; } .title { font-size: 32rpx; font-weight: 800; color: var(--text-main); } } }
.content-scroll { flex: 1; }
.post-detail { margin: 20rpx; background: var(--surface); border-radius: 32rpx; overflow: hidden;
  .media-box { background: var(--bg-main); // 自动填充背景边框
    .media-swiper { width: 100%; transition: height 0.3s ease; .post-image { width: 100%; height: 100%; } }
  }
}
.author-row { padding: 30rpx; display: flex; align-items: center; justify-content: space-between; border-bottom: 2rpx solid #eee; .author-info { display: flex; align-items: center; gap: 20rpx; .avatar { width: 84rpx; height: 84rpx; border-radius: 50%; } .nickname { font-size: 30rpx; font-weight: 800; color: var(--text-main); } .time { font-size: 22rpx; opacity: 0.5; color: var(--text-main); } } 
  .follow-btn { 
    width: 140rpx; 
    height: 68rpx; 
    font-size: 26rpx; 
    background: var(--primary); 
    color: #fff; 
    box-shadow: 4rpx 4rpx 0 #000;
    border: 4rpx solid #000;
    transition: all 0.1s;
    
    &.active { 
      background: var(--bg-main); 
      color: #666; 
      box-shadow: 0 0 0 #000; // 保持占位，避免晃动
      transform: translate(2rpx, 2rpx); // 轻微下沉感
      border-color: #ccc;
    }
  }
}
.content-box { padding: 30rpx; .post-title { font-size: 38rpx; font-weight: 900; margin-bottom: 20rpx; color: var(--text-main); } .post-content { font-size: 28rpx; line-height: 1.6; color: var(--text-main); opacity: 0.9; margin-bottom: 20rpx; } 
  .tag-list { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 20rpx;
    .tag-item { padding: 8rpx 20rpx; font-size: 24rpx; background: var(--bg-main); color: var(--primary); border-radius: 12rpx; font-weight: 600; }
  }
}
.comments-section { 
  margin: 20rpx; 
  padding: 30rpx; 
  
  .section-title { 
    font-size: 30rpx; 
    font-weight: 800; 
    margin-bottom: 40rpx; 
    color: var(--text-main); 
    display: block; 
  } 

  .comments-list {
    margin-top: 20rpx;
  }
  
  .empty-comments { text-align: center; padding: 40rpx; opacity: 0.4; font-size: 24rpx; }
  .comment-item { 
    display: flex; 
    gap: 20rpx; 
    margin-bottom: 40rpx; 
    
    .comment-avatar { 
      width: 64rpx; 
      height: 64rpx; 
      border-radius: 50%; 
      flex-shrink: 0; 
    } 
    
    .comment-user { 
      font-size: 26rpx; 
      font-weight: 800; 
      color: var(--text-main); 
      margin-bottom: 12rpx; 
      display: block; 
    } 
    
    .comment-text { 
      font-size: 26rpx; 
      color: var(--text-main); 
      line-height: 1.4; 
    } 
  } 
}
.footer { position: fixed; bottom: 0; left: 0; right: 0; height: 130rpx; background: var(--surface); display: flex; align-items: center; padding: 0 30rpx; gap: 30rpx; border-radius: 48rpx 48rpx 0 0; z-index: 100; border-top: 4rpx solid #000; .comment-input { flex: 1; height: 80rpx; justify-content: flex-start; padding-left: 30rpx; .placeholder { font-size: 26rpx; opacity: 0.5; color: var(--text-main); } } .action-btn { display: flex; flex-direction: column; align-items: center; color: var(--text-main); .count { font-size: 20rpx; font-weight: 800; margin-top: 4rpx; } } }

.comment-popup-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
}

.comment-popup-content {
  width: 100%;
  background: var(--surface);
  padding: 40rpx;
  border-radius: 48rpx 48rpx 0 0;
  border-top: 4rpx solid #000;
  
  .brutal-textarea {
    width: 100%;
    height: 240rpx;
    background: var(--bg-main);
    border: 4rpx solid #000;
    border-radius: 24rpx;
    padding: 24rpx;
    font-size: 28rpx;
    box-sizing: border-box;
    margin-bottom: 30rpx;
  }
  
  .popup-footer {
    display: flex;
    justify-content: flex-end;
    .send-btn {
      width: 160rpx;
      height: 80rpx;
      font-size: 28rpx;
    }
  }
}
</style>
