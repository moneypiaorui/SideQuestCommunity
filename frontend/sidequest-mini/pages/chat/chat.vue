<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <!-- 固定页头：移出 scroll-view -->
    <view class="fixed-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
        </view>
        <text class="title">{{ recipientName }}</text>
      </view>
    </view>
    
    <!-- 消息滚动区：flex: 1 确保可滑动 -->
    <scroll-view 
      scroll-y 
      class="chat-scroll" 
      :scroll-into-view="lastMessageId"
      :scroll-with-animation="true"
    >
      <view class="msg-list">
        <view v-for="msg in messages" :key="msg.id" :id="'msg-' + msg.id" class="msg-row" :class="{ 'me': msg.senderId === myId }">
          <image v-if="msg.senderId !== myId" :src="recipientAvatar" class="avatar brutal-card" />
          <view class="bubble-container">
            <view class="msg-bubble brutal-card" :class="{ 'primary': msg.senderId === myId }">
              <text class="msg-text">{{ msg.content }}</text>
            </view>
          </view>
          <image v-if="msg.senderId === myId" :src="myAvatar" class="avatar brutal-card" />
        </view>
        <view id="bottom-anchor" style="height: 20rpx;" />
      </view>
    </scroll-view>
    
    <view class="chat-input-area safe-area-bottom brutal-card">
      <input v-model="inputMsg" class="input-box brutal-btn" placeholder="输入消息..." @confirm="sendMsg" />
      <view class="send-btn brutal-btn primary" @click="sendMsg"><text>发送</text></view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false); const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const myId = ref(0)
const myAvatar = ref('')
const recipientId = ref(0)
const recipientName = ref('')
const recipientAvatar = ref('')
const messages = ref([])
const inputMsg = ref('')
const lastMessageId = ref('')
const roomId = ref(0)

onMounted(async () => {
  const pages = getCurrentPages()
  const options = pages[pages.length - 1].options
  roomId.value = options.roomId || 0
  recipientId.value = options.recipientId || 0
  recipientName.value = options.recipientName || '私信'
  
  // 1. 获取我的信息
  try {
    const me = await request({ url: '/api/identity/me' })
    myId.value = me.id
    myAvatar.value = me.avatar || '/static/default-avatar.png'
  } catch (err) {}

  // 2. 如果没有 roomId，先通过 recipientId 查找或创建房间
  if (!roomId.value && recipientId.value) {
    try {
      const room = await request({ url: `/api/chat/rooms/find?recipientId=${recipientId.value}` })
      roomId.value = room.id
    } catch (err) {
      uni.showToast({ title: '初始化聊天失败', icon: 'none' })
    }
  }

  // 3. 加载消息并标记已读
  if (roomId.value) {
    try {
      await request({ url: `/api/chat/rooms/${roomId.value}/read`, method: 'POST' })
      const res = await request({ url: `/api/chat/rooms/${roomId.value}/messages` })
      messages.value = res
      
      // 提取对方头像
      if (options.recipientAvatar) {
        recipientAvatar.value = options.recipientAvatar
      } else {
        const otherMsg = messages.value.find(m => m.senderId !== myId.value)
        if (otherMsg) {
          // 这里可以进一步优化，从消息列表或房间详情中获取对方头像
        }
      }
    } catch (err) {}
  }
  
  scrollToBottom()
  
  // 开启定时轮询新消息 (实际项目中建议使用 WebSocket)
  startPolling()
})

const pollingTimer = ref(null)
const startPolling = () => {
  pollingTimer.value = setInterval(async () => {
    if (!roomId.value) return
    try {
      const lastId = messages.value.length > 0 ? messages.value[messages.value.length - 1].id : 0
      const newMsgs = await request({ url: `/api/chat/rooms/${roomId.value}/messages?sinceId=${lastId}` })
      if (newMsgs.length > 0) {
        messages.value.push(...newMsgs)
        scrollToBottom()
      }
    } catch (err) {}
  }, 3000)
}

import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (pollingTimer.value) clearInterval(pollingTimer.value)
})

const sendMsg = async () => {
  if (!inputMsg.value) return
  const content = inputMsg.value
  inputMsg.value = ''
  
  try {
    const res = await request({
      url: `/api/chat/rooms/${roomId.value}/send`,
      method: 'POST',
      data: { content }
    })
    messages.value.push(res)
    scrollToBottom()
  } catch (err) {
    uni.showToast({ title: '发送失败', icon: 'none' })
  }
}
const scrollToBottom = () => nextTick(() => lastMessageId.value = 'bottom-anchor')
const goBack = () => uni.navigateBack()
</script>

<style lang="scss" scoped>
.page-container { height: 100vh; display: flex; flex-direction: column; background: var(--bg-main); overflow: hidden; }

.fixed-header { 
  background: var(--surface); border-bottom: 4rpx solid #000; z-index: 1000;
  .nav-bar { height: 100rpx; display: flex; align-items: center; padding: 0 30rpx; gap: 30rpx;
    .back-btn { width: 70rpx; height: 70rpx; border-radius: 16rpx; }
    .title { font-size: 32rpx; font-weight: 800; color: var(--text-main); }
  }
}

.chat-scroll { flex: 1; height: 0; // 关键：确保 flex 布局下的滚动生效
  .msg-list { padding: 40rpx 24rpx 160rpx; }
}

.msg-row { display: flex; gap: 20rpx; margin-bottom: 48rpx; align-items: flex-start; &.me { justify-content: flex-end; } }
.avatar { width: 88rpx; height: 88rpx; border-radius: 24rpx; flex-shrink: 0; box-shadow: 4rpx 4rpx 0 #000; }
.bubble-container { max-width: 65%; 
  .msg-bubble { padding: 24rpx 28rpx; background: var(--surface); border-radius: 28rpx; box-shadow: 6rpx 6rpx 0 #000;
    .msg-text { font-size: 28rpx; line-height: 1.5; color: var(--text-main); word-break: break-all; }
    &.primary { background: var(--primary); box-shadow: -6rpx 6rpx 0 #000; .msg-text { color: #fff !important; } }
  }
}

.chat-input-area { position: fixed; bottom: 0; left: 0; right: 0; height: 140rpx; background: var(--surface); display: flex; align-items: center; padding: 0 30rpx calc(env(safe-area-inset-bottom) + 10rpx); gap: 20rpx; border-radius: 48rpx 48rpx 0 0; border-top: 4rpx solid #000; z-index: 100;
  .input-box { flex: 1; height: 88rpx; padding-left: 30rpx; font-size: 28rpx; background: var(--bg-main); border: 2rpx solid #000; } 
  .send-btn { width: 140rpx; height: 88rpx; font-size: 30rpx; flex-shrink: 0; } 
}
</style>
