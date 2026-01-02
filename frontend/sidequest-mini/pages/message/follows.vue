<template>
  <view :class="{ dark: isDark }" class="page-container">
    <view class="custom-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </view>
        <text class="title">新增关注</text>
      </view>
    </view>
    <scroll-view scroll-y class="content-scroll" @scrolltolower="loadMore">
      <view v-for="item in followers" :key="item.id" class="notify-item brutal-card">
        <image :src="item.avatar || 'https://picsum.photos/100/100'" class="avatar brutal-card" />
        <view class="info">
          <text class="user">{{ item.nickname }}</text>
          <text class="action">开始关注你</text>
          <text class="time">{{ formatDate(item.createTime) }}</text>
        </view>
        <view
          v-if="item.id !== currentUserId"
          class="follow-btn brutal-btn"
          :class="{ primary: !item.isFollowing }"
          @click.stop="toggleFollow(item)"
        >
          {{ item.isFollowing ? "已关注" : "回关" }}
        </view>
      </view>
      <view v-if="loading" class="loading-status">加载中...</view>
      <view v-if="noMore && followers.length > 0" class="loading-status">没有更多了</view>
      <view v-if="!loading && followers.length === 0" class="empty-status">暂无新增关注</view>
      <view class="safe-area-bottom" style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue"
import request from "@/utils/request"

const isDark = ref(uni.getStorageSync("isDark") || false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const followers = ref([])
const currentUserId = ref(null)
const loading = ref(false)
const noMore = ref(false)
let currentPage = 1

onMounted(() => {
  loadFollowers(true)
})

const loadFollowers = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return

  if (reset) {
    currentPage = 1
    noMore.value = false
    followers.value = []
  }

  loading.value = true
  try {
    const me = await request({ url: "/api/identity/me" })
    currentUserId.value = me?.id

    const res = await request({
      url: `/api/identity/users/${currentUserId.value}/followers?current=${currentPage}&size=20`,
    })
    const records = res?.records || []

    let followingIds = []
    if (uni.getStorageSync("token")) {
      const followRes = await request({ url: "/api/identity/me/following-ids" })
      followingIds = followRes || []
    }

    const processed = records.map((user) => ({
      ...user,
      isFollowing: followingIds.includes(user.id),
    }))

    if (processed.length < 20) {
      noMore.value = true
    }

    followers.value = [...followers.value, ...processed]
    currentPage += 1
  } catch (err) {
    console.error("Load followers failed:", err)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  loadFollowers()
}

const toggleFollow = async (user) => {
  const token = uni.getStorageSync("token")
  if (!token) {
    uni.showToast({ title: "请先登录", icon: "none" })
    return
  }

  try {
    const url = user.isFollowing ? `/api/identity/users/${user.id}/unfollow` : `/api/identity/users/${user.id}/follow`
    await request({ url, method: "POST" })
    user.isFollowing = !user.isFollowing
    uni.showToast({ title: user.isFollowing ? "已关注" : "已取消关注", icon: "none" })
  } catch (err) {
    if (err.message && err.message.includes("cannot follow yourself")) {
      uni.showToast({ title: "不能关注自己", icon: "none" })
    }
  }
}

const formatDate = (value) => {
  if (!value) return ""
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ""
  return date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" })
}

const goBack = () => uni.navigateBack()
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg-main);
}
.custom-header {
  background: var(--surface);
  border-bottom: 4rpx solid #000;
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
.notify-item {
  margin-bottom: 24rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: var(--surface);
  box-shadow: 6rpx 6rpx 0 #000;
  .avatar {
    width: 90rpx;
    height: 90rpx;
    border-radius: 50%;
    flex-shrink: 0;
  }
  .info {
    flex: 1;
    min-width: 0;
    .user {
      font-size: 30rpx;
      font-weight: 800;
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .action {
      font-size: 24rpx;
      opacity: 0.7;
    }
    .time {
      font-size: 20rpx;
      opacity: 0.4;
      display: block;
    }
  }
  .follow-btn {
    width: 130rpx;
    height: 64rpx;
    font-size: 24rpx;
    flex-shrink: 0;
    box-shadow: 4rpx 4rpx 0 #000;
    &.primary {
      background: var(--primary);
      color: #fff;
    }
  }
}
.loading-status,
.empty-status {
  text-align: center;
  padding: 40rpx;
  font-size: 24rpx;
  color: var(--text-main);
  opacity: 0.5;
}
</style>
