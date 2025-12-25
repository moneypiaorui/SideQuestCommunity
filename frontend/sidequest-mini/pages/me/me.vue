<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <scroll-view scroll-y class="content-scroll">
      <view class="profile-header">
        <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
        <view class="settings-bar">
          <view class="icon-btn brutal-btn" @click="toggleDark">
            <svg v-if="isDark" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <circle cx="12" cy="12" r="5"></circle>
              <line x1="12" y1="1" x2="12" y2="3"></line>
              <line x1="12" y1="21" x2="12" y2="23"></line>
            </svg>
            <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
            </svg>
          </view>
        </view>
        
        <view class="user-info">
          <image :src="user.avatar || '/static/default-avatar.png'" class="main-avatar brutal-card" />
          <view class="user-details">
            <text class="nickname">{{ user.nickname || '未登录' }}</text>
            <text class="user-id">ID: {{ user.id || '---' }}</text>
          </view>
        </view>

        <view v-if="user.signature" class="signature-box">
          <text class="signature-text">{{ user.signature }}</text>
        </view>
        
        <view class="stats-row">
          <view v-for="s in stats" :key="s.label" class="stat-item" @click="s.type && goToFollowList(s.type)">
            <text class="stat-value">{{ s.value }}</text>
            <text class="stat-label">{{ s.label }}</text>
          </view>
        </view>
        
        <view class="action-row">
          <button v-if="!user.id" class="brutal-btn primary login-btn" @click="goToLogin">去登录</button>
          <button v-else class="brutal-btn edit-btn" @click="goToEdit">编辑资料</button>
        </view>
      </view>
      
      <view class="content-tabs">
        <view 
          v-for="(t, i) in ['笔记', '收藏', '赞过']" 
          :key="i"
          class="tab-item"
          :class="{ 'active': activeContentTab === i }"
          @click="activeContentTab = i"
        >
          <text>{{ t }}</text>
        </view>
      </view>
      
      <view class="waterfall">
        <view class="column left-column">
          <BrutalCard 
            v-for="post in leftColumnPosts" 
            :key="post.id" 
            :post="post" 
          />
        </view>
        <view class="column right-column">
          <BrutalCard 
            v-for="post in rightColumnPosts" 
            :key="post.id" 
            :post="post" 
          />
        </view>
      </view>
      
      <view class="safe-area-bottom" style="height: 160rpx;" />
    </scroll-view>
    
    <BrutalTabBar activeTab="me" />
    <LoginModal />
  </view>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import BrutalCard from '@/components/BrutalCard/BrutalCard.vue'
import BrutalTabBar from '@/components/BrutalTabBar/BrutalTabBar.vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const statusBarHeight = ref(0)
const activeContentTab = ref(0)
const user = ref({})
const posts = ref([])

const stats = computed(() => [
  { label: '关注', value: user.value.followingCount || 0, type: 'following' },
  { label: '粉丝', value: user.value.followerCount || 0, type: 'followers' },
  { label: '获赞与收藏', value: (user.value.totalLikedCount || 0) + (user.value.totalFavoritedCount || 0) }
])

const goToFollowList = (type) => {
  if (!user.value.id) return
  uni.navigateTo({ url: `/pages/user-profile/follow-list?userId=${user.value.id}&type=${type}` })
}

const leftColumnPosts = ref([])
const rightColumnPosts = ref([])
let leftHeight = 0
let rightHeight = 0

const toggleDark = () => {
  isDark.value = !isDark.value
  uni.setStorageSync('isDark', isDark.value)
}

onMounted(async () => {
  const sys = uni.getSystemInfoSync()
  statusBarHeight.value = sys.statusBarHeight
  
  try {
    const userInfo = await request({ url: '/api/identity/me' })
    user.value = userInfo
    
    // Fetch user posts
    const data = await request({ 
      url: `/api/search/user/posts?userId=${userInfo.id}&size=20` 
    })
    distributePosts(data.content.map(p => {
      let urls = p.imageUrls
      if (typeof urls === 'string') {
        try { urls = JSON.parse(urls) } catch (e) { urls = [urls] }
      }
      return { ...p, imageUrls: urls || [] }
    }))
  } catch (err) {}
})

const distributePosts = (newPosts) => {
  newPosts.forEach(post => {
    let h = 400
    const match = post.imageUrls[0]?.match(/_w(\d+)_h(\d+)/)
    if (match) {
      const w = parseInt(match[1])
      const h_orig = parseInt(match[2])
      const ratio = h_orig / w
      const constrainedRatio = Math.max(0.75, Math.min(1.33, ratio))
      h = 340 * constrainedRatio
    }
    
    if (leftHeight <= rightHeight) {
      leftColumnPosts.value.push(post)
      leftHeight += h
    } else {
      rightColumnPosts.value.push(post)
      rightHeight += h
    }
  })
}

const goToLogin = () => {
  bus.openLogin()
}

const goToEdit = () => {
  uni.navigateTo({ url: '/pages/me/edit' })
}
</script>

<style lang="scss" scoped>
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-main);
}

.content-scroll {
  flex: 1;
}

.profile-header {
  background-color: var(--primary);
  padding: 0 40rpx 40rpx;
  border-bottom: 4rpx solid #000;
}

.settings-bar {
  height: 80rpx;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.icon-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 30rpx;
  margin-top: 20rpx;
  
  .main-avatar {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    background-color: #fff;
  }
  
  .user-details {
    .nickname {
      font-size: 40rpx;
      font-weight: 900;
      color: #000;
      display: block;
    }
    .user-id {
      font-size: 22rpx;
      font-weight: 600;
      opacity: 0.6;
    }
  }
}

.signature-box {
  margin-top: 20rpx;
  .signature-text {
    font-size: 24rpx;
    font-weight: 600;
    opacity: 0.8;
    color: #000;
    line-height: 1.4;
  }
}

.stats-row {
  display: flex;
  gap: 40rpx;
  margin-top: 40rpx;
  
  .stat-item {
    display: flex;
    flex-direction: column;
    
    .stat-value {
      font-size: 32rpx;
      font-weight: 900;
    }
    .stat-label {
      font-size: 20rpx;
      font-weight: 700;
      opacity: 0.7;
    }
  }
}

.action-row {
  margin-top: 40rpx;
  
  .login-btn, .edit-btn {
    width: 240rpx;
    height: 70rpx;
    font-size: 24rpx;
  }
}

.content-tabs {
  display: flex;
  justify-content: center;
  gap: 60rpx;
  background-color: var(--surface);
  height: 100rpx;
  align-items: center;
  border-bottom: 2rpx solid #eee;
  
  .tab-item {
    font-size: 28rpx;
    font-weight: 800;
    opacity: 0.5;
    position: relative;
    
    &.active {
      opacity: 1;
      
      &::after {
        content: '';
        position: absolute;
        bottom: -10rpx;
        left: 50%;
        transform: translateX(-50%) skewX(-20deg);
        width: 40rpx;
        height: 6rpx;
        background-color: var(--primary);
      }
    }
  }
}

.waterfall {
  display: flex;
  padding: 20rpx;
  gap: 20rpx;
  
  .column {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 20rpx;
  }
}
</style>

