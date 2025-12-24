<template>
  <view class="header-container" :class="{ 'dark': isDark }">
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
    <view class="nav-bar">
      <view class="tabs">
        <view 
          v-for="(tab, index) in tabs" 
          :key="index"
          class="tab-item"
          :class="{ 'active': activeTab === index }"
          @click="$emit('tab-change', index)"
        >
          <text class="tab-text">{{ tab }}</text>
        </view>
      </view>
      
      <view class="actions">
        <view class="icon-btn" @click="$emit('search')">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
        </view>
        <view class="icon-btn" @click="toggleDark">
          <svg v-if="isDark" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
          </svg>
          <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
          </svg>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  tabs: {
    type: Array,
    default: () => ['关注', '发现', '附近']
  },
  activeTab: {
    type: Number,
    default: 1
  },
  isDark: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['tab-change', 'search', 'toggle-dark'])

const statusBarHeight = ref(0)

onMounted(() => {
  const sys = uni.getSystemInfoSync()
  statusBarHeight.value = sys.statusBarHeight
})

const toggleDark = () => {
  emit('toggle-dark')
}
</script>

<style lang="scss" scoped>
.header-container {
  background-color: var(--bg-main);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 4rpx solid var(--border-color);
}

.nav-bar {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
}

.tabs {
  display: flex;
  gap: 30rpx;
  
  .tab-item {
    padding: 8rpx 16rpx;
    border-radius: 12rpx;
    transition: all 0.2s ease;
    
    .tab-text {
      font-size: 32rpx;
      font-weight: 800;
      color: var(--text-main);
      opacity: 0.6;
    }
    
    &.active {
      background-color: var(--primary);
      border: 3rpx solid var(--border-color);
      box-shadow: 4rpx 4rpx 0px 0px #000;
      
      .tab-text {
        opacity: 1;
        color: #fff;
      }
    }
  }
}

.actions {
  display: flex;
  gap: 20rpx;
}

.icon-btn {
  width: 70rpx;
  height: 70rpx;
  background-color: var(--surface);
  border: 3rpx solid var(--border-color);
  box-shadow: 4rpx 4rpx 0px 0px #000;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:active {
    transform: translate(2rpx, 2rpx);
    box-shadow: 2rpx 2rpx 0px 0px #000;
  }
}
</style>

