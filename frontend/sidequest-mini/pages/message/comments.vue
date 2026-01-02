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
        <text class="title">收到的评论</text>
      </view>
    </view>
    <scroll-view scroll-y class="content-scroll" @scrolltolower="loadMore">
      <view v-for="item in comments" :key="item.id" class="notify-item brutal-card">
        <image :src="item.avatar || 'https://picsum.photos/80/80?random=10'" class="avatar" />
        <view class="info">
          <text class="user">{{ item.title || "评论提醒" }}</text>
          <text class="action">{{ item.content }}</text>
          <text class="time">{{ formatDate(item.createTime) }}</text>
        </view>
      </view>
      <view v-if="loading" class="loading-status">加载中...</view>
      <view v-if="noMore && comments.length > 0" class="loading-status">没有更多了</view>
      <view v-if="!loading && comments.length === 0" class="empty-status">暂无评论通知</view>
      <view class="safe-area-bottom" style="height: 40rpx;" />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from "vue"
import request from "@/utils/request"

const isDark = ref(uni.getStorageSync("isDark") || false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const comments = ref([])
const loading = ref(false)
const noMore = ref(false)
let currentPage = 1

onMounted(() => {
  loadComments(true)
})

const loadComments = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return

  if (reset) {
    currentPage = 1
    noMore.value = false
    comments.value = []
  }

  loading.value = true
  try {
    const res = await request({
      url: `/api/notifications?type=interaction&current=${currentPage}&size=20`,
    })
    const records = res?.records || []
    const filtered = records.filter((item) => {
      const content = (item.content || "").toLowerCase()
      return content.includes("comment")
    })

    if (records.length < 20) {
      noMore.value = true
    }
    comments.value = [...comments.value, ...filtered]
    currentPage += 1

    if (currentPage === 2) {
      await request({ url: "/api/notifications/mark-read?type=interaction", method: "POST" })
    }
  } catch (err) {
    console.error("Load comments failed:", err)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  loadComments()
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
.notify-item {
  margin: 20rpx;
  padding: 24rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  .avatar {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
    border: 2rpx solid #000;
  }
  .info {
    flex: 1;
    .user {
      font-size: 28rpx;
      font-weight: 800;
      display: block;
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
