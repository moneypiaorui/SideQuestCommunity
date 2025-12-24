<template>
  <view class="tab-bar-container safe-area-bottom">
    <view class="tab-bar">
      <view 
        v-for="(item, index) in tabs" 
        :key="index"
        class="tab-item"
        :class="{ 'active': activeTab === item.key }"
        @click="switchTab(item.key)"
      >
        <template v-if="item.key === 'publish'">
          <view class="publish-btn">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="4">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
          </view>
        </template>
        <template v-else>
          <text class="tab-label">{{ item.label }}</text>
          <view v-if="activeTab === item.key" class="active-indicator" />
        </template>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  activeTab: {
    type: String,
    default: 'home'
  }
})

const emit = defineEmits(['change'])

const tabs = [
  { label: '首页', key: 'home' },
  { label: '分区', key: 'sections' },
  { label: '', key: 'publish' },
  { label: '消息', key: 'message' },
  { label: '我', key: 'me' }
]

const switchTab = (key) => {
  if (key === 'publish') {
    uni.navigateTo({ url: '/pages/publish/publish' })
    return
  }
  emit('change', key)
  uni.switchTab({ url: `/pages/${key}/${key}` })
}
</script>

<style lang="scss" scoped>
.tab-bar-container {
  position: fixed;
  bottom: 40rpx;
  left: 30rpx;
  right: 30rpx;
  z-index: 1000;
}

.tab-bar {
  display: flex;
  height: 110rpx;
  background-color: var(--surface);
  border: 4rpx solid var(--border-color);
  box-shadow: 8rpx 8rpx 0px 0px #000;
  border-radius: 60rpx;
  align-items: center;
  justify-content: space-around;
  padding: 0 20rpx;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  position: relative;
  
  .tab-label {
    font-size: 28rpx;
    font-weight: 800;
    color: var(--text-main);
    transition: all 0.2s ease;
  }
  
  &.active .tab-label {
    color: var(--primary);
    transform: scale(1.1);
  }
  
  .active-indicator {
    position: absolute;
    bottom: 12rpx;
    width: 30rpx;
    height: 6rpx;
    background-color: var(--text-main);
    transform: skewX(-20deg);
  }
}

.publish-btn {
  width: 84rpx;
  height: 84rpx;
  background-color: var(--primary);
  border: 4rpx solid var(--border-color);
  box-shadow: 4rpx 4rpx 0px 0px #000;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  
  &:active {
    transform: translate(2rpx, 2rpx);
    box-shadow: 2rpx 2rpx 0px 0px #000;
  }
}
</style>

