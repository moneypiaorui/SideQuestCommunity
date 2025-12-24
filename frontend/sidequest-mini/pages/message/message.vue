<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <BrutalHeader 
      :isDark="isDark" 
      :activeTab="1" 
      :tabs="['消息中心']"
      @toggle-dark="toggleDark"
    />
    
    <scroll-view scroll-y class="content-scroll">
      <view class="interaction-tabs">
        <view v-for="(t, i) in interactionTypes" :key="i" class="type-item" @click="goToNotify(t.path)">
          <view class="icon-box brutal-btn" :style="{ backgroundColor: t.color }">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3">
              <path :d="t.icon"></path>
            </svg>
            <view v-if="unreadCounts[t.key] > 0" class="badge">{{ unreadCounts[t.key] }}</view>
          </view>
          <text class="type-label">{{ t.label }}</text>
        </view>
      </view>
      
      <view class="chat-list">
        <view v-if="rooms.length === 0" class="empty-state">
          <text class="empty-text">暂无私信消息</text>
        </view>
        <view 
          v-for="room in rooms" 
          :key="room.id" 
          class="chat-item brutal-card"
          @click="goToChat(room)"
        >
          <image :src="room.recipientAvatar || '/static/default-avatar.png'" class="avatar" />
          <view class="chat-info">
            <view class="chat-header">
              <text class="nickname">{{ room.recipientNickname }}</text>
              <text class="time">{{ formatTime(room.lastMessageTime) }}</text>
            </view>
            <text class="last-msg">{{ room.lastMessage }}</text>
          </view>
          <view v-if="room.unreadCount > 0" class="unread-dot" />
        </view>
      </view>
      
      <view class="safe-area-bottom" style="height: 160rpx;" />
    </scroll-view>
    
    <BrutalTabBar activeTab="message" />
    <LoginModal />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BrutalHeader from '@/components/BrutalHeader/BrutalHeader.vue'
import BrutalTabBar from '@/components/BrutalTabBar/BrutalTabBar.vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const unreadCounts = ref({ chat: 0, interaction: 0, system: 0 })
const rooms = ref([])

const interactionTypes = [
  { label: '赞和收藏', key: 'interaction', color: '#FF2442', icon: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z', path: '/pages/message/interactions' },
  { label: '新增关注', key: 'follow', color: '#3B82F6', icon: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', path: '/pages/message/follows' },
  { label: '收到的评论', key: 'comment', color: '#10B981', icon: 'M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z', path: '/pages/message/comments' }
]

onMounted(async () => {
  try {
    const counts = await request({ url: '/api/notifications/unread-count' })
    unreadCounts.value = counts
    const chatRooms = await request({ url: '/api/chat/rooms' })
    rooms.value = chatRooms
  } catch (err) {}
})

const goToNotify = (path) => uni.navigateTo({ url: path })
const goToChat = (room) => {
  uni.navigateTo({ 
    url: `/pages/chat/chat?roomId=${room.id}&recipientName=${room.recipientNickname}&recipientAvatar=${room.recipientAvatar}` 
  })
}
const formatTime = (t) => t ? t.split('T')[0].slice(5) : ''
const toggleDark = () => { isDark.value = !isDark.value; uni.setStorageSync('isDark', isDark.value) }
</script>

<style lang="scss" scoped>
.page-container { height: 100vh; display: flex; flex-direction: column; background-color: var(--bg-main); }
.content-scroll { flex: 1; }
.interaction-tabs { display: flex; justify-content: space-around; padding: 40rpx 0; background-color: var(--surface); border-bottom: 2rpx solid #eee;
  .type-item { display: flex; flex-direction: column; align-items: center; gap: 16rpx;
    .icon-box { width: 100rpx; height: 100rpx; border-radius: 30rpx; position: relative; }
    .badge { position: absolute; top: -10rpx; right: -10rpx; background: var(--accent-red); color: #fff; font-size: 20rpx; padding: 0 10rpx; border-radius: 20rpx; border: 3rpx solid #000; font-weight: 800; }
    .type-label { font-size: 24rpx; font-weight: 700; color: var(--text-main); }
  }
}
.chat-list { padding: 30rpx; display: flex; flex-direction: column; gap: 24rpx;
  .chat-item { padding: 24rpx; display: flex; align-items: center; gap: 20rpx; position: relative; background: var(--surface);
    .avatar { width: 100rpx; height: 100rpx; border-radius: 50%; border: 3rpx solid var(--border-color); }
    .chat-info { flex: 1; overflow: hidden;
      .chat-header { display: flex; justify-content: space-between; margin-bottom: 8rpx;
        .nickname { font-size: 30rpx; font-weight: 800; color: var(--text-main); }
        .time { font-size: 20rpx; opacity: 0.5; color: var(--text-main); }
      }
      .last-msg { font-size: 24rpx; opacity: 0.6; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: var(--text-main); }
    }
    .unread-dot { width: 16rpx; height: 16rpx; background: var(--accent-red); border-radius: 50%; border: 2rpx solid #000; position: absolute; right: 30rpx; bottom: 30rpx; }
  }
}
.empty-state { padding: 100rpx 0; text-align: center; opacity: 0.3; .empty-text { font-size: 32rpx; font-weight: 800; color: var(--text-main); } }
</style>
