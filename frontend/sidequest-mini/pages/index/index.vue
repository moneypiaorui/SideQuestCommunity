<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <BrutalHeader 
      :isDark="isDark" 
      :activeTab="activeTopTab" 
      @tab-change="onTopTabChange" 
      @toggle-dark="toggleDark"
    />
    
    <scroll-view 
      scroll-y 
      class="content-scroll" 
      @scrolltolower="loadMore"
      refresh-with-animation
      :refresher-enabled="true"
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="waterfall">
        <view class="column left-column">
          <BrutalCard 
            v-for="post in leftColumnPosts" 
            :key="post.id" 
            :post="post" 
            @click="goToDetail(post.id)"
            @like="handleLike"
          />
        </view>
        <view class="column right-column">
          <BrutalCard 
            v-for="post in rightColumnPosts" 
            :key="post.id" 
            :post="post" 
            @click="goToDetail(post.id)"
            @like="handleLike"
          />
        </view>
      </view>
      
      <view v-if="loading" class="loading-state">
        <text class="loading-text">正在加载...</text>
      </view>
      <view v-if="noMore" class="loading-state">
        <text class="loading-text">没有更多了</text>
      </view>
      
      <view class="safe-area-bottom" style="height: 160rpx;" />
    </scroll-view>
    
    <BrutalTabBar activeTab="home" />
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted, inject } from 'vue'
import BrutalHeader from '@/components/BrutalHeader/BrutalHeader.vue'
import BrutalCard from '@/components/BrutalCard/BrutalCard.vue'
import BrutalTabBar from '@/components/BrutalTabBar/BrutalTabBar.vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const activeTopTab = ref(1)
const isRefreshing = ref(false)
const loading = ref(false)
const noMore = ref(false)
const posts = ref([])
const page = ref(1)

const toggleDark = () => {
  isDark.value = !isDark.value
  uni.setStorageSync('isDark', isDark.value)
}

const leftColumnPosts = ref([])
const rightColumnPosts = ref([])
let leftHeight = 0
let rightHeight = 0

const onTopTabChange = (index) => {
  activeTopTab.value = index
  onRefresh()
}

const fetchPosts = async () => {
  if (loading.value || noMore.value) return
  loading.value = true
  
  try {
    const data = await request({
      url: `/api/core/posts?current=${page.value}&size=10`
    })
    
    const newPosts = data.records.map(p => {
      // Parse imageUrls if it's a string
      let urls = p.imageUrls
      if (typeof urls === 'string') {
        try { urls = JSON.parse(urls) } catch (e) { urls = [urls] }
      }
      return { ...p, imageUrls: urls }
    })
    
    if (newPosts.length === 0) {
      noMore.value = true
    } else {
      distributePosts(newPosts)
      page.value++
    }
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
    isRefreshing.value = false
  }
}

const distributePosts = (newPosts) => {
  newPosts.forEach(post => {
    // Try to extract dimensions from filename (e.g., _w1080_h1920)
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

const onRefresh = () => {
  isRefreshing.value = true
  page.value = 1
  noMore.value = false
  leftColumnPosts.value = []
  rightColumnPosts.value = []
  leftHeight = 0
  rightHeight = 0
  fetchPosts()
}

const loadMore = () => {
  fetchPosts()
}

const goToDetail = (id) => {
  uni.navigateTo({ url: `/pages/post-detail/post-detail?id=${id}` })
}

const handleLike = async (id) => {
  try {
    await request({
      url: `/api/core/interactions/like?postId=${id}`,
      method: 'POST'
    })
    // Update local state if needed
  } catch (err) {}
}

onMounted(() => {
  fetchPosts()
})
</script>

<style lang="scss">
.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-main);
}

.content-scroll {
  flex: 1;
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

.loading-state {
  padding: 40rpx;
  text-align: center;
  
  .loading-text {
    font-size: 24rpx;
    font-weight: 800;
    opacity: 0.5;
  }
}
</style>

