<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <view class="custom-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </view>
        <text class="title">{{ type === 'following' ? '关注列表' : '粉丝列表' }}</text>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll" @scrolltolower="loadMore">
      <view v-for="item in userList" :key="item.id" class="user-item brutal-card" @click="goToProfile(item.id)">
        <image :src="item.avatar || 'https://picsum.photos/100/100'" class="avatar brutal-card" />
        <view class="info">
          <text class="nickname">{{ item.nickname }}</text>
          <text class="signature">{{ item.signature || '这个人很懒，什么都没有留下' }}</text>
        </view>
        <!-- 仅在他人的关注/粉丝列表中显示关注按钮，或者如果是自己的列表，显示关注状态 -->
        <view v-if="item.id !== currentUserId" class="follow-btn brutal-btn" 
              :class="{ 'primary': !item.isFollowing }" 
              @click.stop="toggleFollow(item)">
          {{ item.isFollowing ? '已关注' : '关注' }}
        </view>
      </view>
      
      <view v-if="loading" class="loading-status">加载中...</view>
      <view v-if="noMore && userList.length > 0" class="loading-status">没有更多了</view>
      <view v-if="!loading && userList.length === 0" class="empty-status">
        <text>暂无数据</text>
      </view>
      <view class="safe-area-bottom" style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const userList = ref([])
const loading = ref(false)
const noMore = ref(false)
const type = ref('following')
const userId = ref(null)
const currentUserId = ref(uni.getStorageSync('userId'))
let currentPage = 1

onMounted(() => {
  const pages = getCurrentPages()
  const options = pages[pages.length - 1].options
  userId.value = options.userId
  type.value = options.type || 'following'
  
  loadData()
})

const loadData = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return
  
  if (reset) {
    currentPage = 1
    noMore.value = false
    userList.value = []
  }
  
  loading.value = true
  try {
    const url = `/api/identity/users/${userId.value}/${type.value}?current=${currentPage}&size=20`
    const res = await request({ url })
    const records = res.records || []
    
    // 获取当前用户的关注列表，用于标记列表中用户的关注状态
    let followingIds = []
    if (uni.getStorageSync('token')) {
      const followRes = await request({ url: '/api/identity/me/following-ids' })
      followingIds = followRes || []
    }

    const processedRecords = records.map(user => ({
      ...user,
      isFollowing: followingIds.includes(user.id)
    }))

    if (processedRecords.length < 20) {
      noMore.value = true
    }
    
    userList.value = [...userList.value, ...processedRecords]
    currentPage++
  } catch (err) {
    console.error('Load follow list failed:', err)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  loadData()
}

const toggleFollow = async (user) => {
  const token = uni.getStorageSync('token')
  if (!token) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  
  try {
    const method = 'POST'
    const url = user.isFollowing ? `/api/identity/users/${user.id}/unfollow` : `/api/identity/users/${user.id}/follow`
    await request({ url, method })
    user.isFollowing = !user.isFollowing
    uni.showToast({ title: user.isFollowing ? '已关注' : '已取消关注', icon: 'none' })
  } catch (err) {
    if (err.message && err.message.includes('cannot follow yourself')) {
      uni.showToast({ title: '不能关注自己', icon: 'none' })
    }
  }
}

const goToProfile = (id) => {
  uni.navigateTo({ url: `/pages/user-profile/user-profile?userId=${id}` })
}

const goBack = () => uni.navigateBack()
const goHome = () => uni.reLaunch({ url: '/pages/index/index' })
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-main);
}

.custom-header {
  background-color: var(--surface);
  border-bottom: 4rpx solid var(--border-color);
  .nav-bar {
    height: 100rpx;
    display: flex;
    align-items: center;
    padding: 0 30rpx;
    gap: 30rpx;
    .back-btn {
      width: 70rpx;
      height: 70rpx;
      border-radius: 16rpx;
    }
    .title {
      font-size: 32rpx;
      font-weight: 800;
    }
  }
}

.content-scroll {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
  background-color: var(--surface);
  gap: 20rpx;
  
  .avatar {
    width: 100rpx;
    height: 100rpx;
    border-radius: 50%;
    flex-shrink: 0;
  }
  
  .info {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    .nickname {
      font-size: 30rpx;
      font-weight: 900;
      margin-bottom: 6rpx;
      color: var(--text-main);
    }
    .signature {
      font-size: 22rpx;
      color: var(--text-main);
      opacity: 0.6;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
  
  .follow-btn {
    width: 140rpx;
    height: 64rpx;
    font-size: 24rpx;
    flex-shrink: 0;
    &.primary {
      background-color: var(--primary);
      color: #fff;
    }
  }
}

.loading-status, .empty-status {
  text-align: center;
  padding: 40rpx;
  font-size: 24rpx;
  color: var(--text-main);
  opacity: 0.5;
}
</style>

