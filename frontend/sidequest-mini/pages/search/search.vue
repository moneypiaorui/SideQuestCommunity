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
        <view class="search-input brutal-btn">
          <input 
            v-model="keyword" 
            class="input" 
            placeholder="搜索感兴趣的内容..." 
            @confirm="onSearch"
          />
        </view>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll" @scrolltolower="loadMore">
      <view v-if="!results.length && !loading" class="hot-search brutal-card">
        <text class="title">热门搜索</text>
        <view class="hot-tags">
          <view 
            v-for="tag in hotTags" 
            :key="tag" 
            class="hot-tag brutal-btn"
            @click="keyword = tag; onSearch(true)"
          >
            {{ tag }}
          </view>
        </view>
      </view>
      
      <view v-else class="waterfall">
        <view class="column left-column">
          <BrutalCard v-for="post in leftColumnPosts" :key="post.id" :post="post" @click="goToDetail(post.id)" />
        </view>
        <view class="column right-column">
          <BrutalCard v-for="post in rightColumnPosts" :key="post.id" :post="post" @click="goToDetail(post.id)" />
        </view>
      </view>
      
      <view v-if="loading" class="loading-status">加载中...</view>
      <view v-if="noMore" class="loading-status">没有更多了</view>
      <view class="safe-area-bottom" style="height: 100rpx;" />
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/utils/request'
import BrutalCard from '@/components/BrutalCard/BrutalCard.vue'

const isDark = ref(uni.getStorageSync('isDark') || false)
const statusBarHeight = ref(0)
const keyword = ref('')
const hotTags = ['新野兽主义', '机能风', '成都咖啡', 'Vue3实战', '小红书排版']
const results = ref([])
const leftColumnPosts = ref([])
const rightColumnPosts = ref([])
let leftHeight = 0
let rightHeight = 0

const loading = ref(false)
const noMore = ref(false)
let currentPage = 0

onLoad((options) => {
  if (options.keyword) {
    keyword.value = decodeURIComponent(options.keyword)
    onSearch(true)
  }
})

onMounted(() => {
  const sys = uni.getSystemInfoSync()
  statusBarHeight.value = sys.statusBarHeight
})

const onSearch = async (reset = true) => {
  if (!keyword.value) return
  if (loading.value || (noMore.value && !reset)) return
  
  if (reset) {
    currentPage = 0
    noMore.value = false
    leftColumnPosts.value = []
    rightColumnPosts.value = []
    leftHeight = 0
    rightHeight = 0
    results.value = []
  }

  loading.value = true
  try {
    const data = await request({ 
      url: `/api/search/posts?keyword=${encodeURIComponent(keyword.value)}&page=${currentPage}&size=10` 
    })
    const records = data.content || []
    
    if (records.length === 0) {
      noMore.value = true
    } else {
      results.value = [...results.value, ...records]
      distributePosts(records.map(p => {
        let urls = []
        if (p.imageUrls) {
          if (typeof p.imageUrls === 'string') {
            try {
              urls = p.imageUrls.startsWith('[') ? JSON.parse(p.imageUrls) : p.imageUrls.split(',')
            } catch (e) {
              urls = [p.imageUrls]
            }
          } else {
            urls = p.imageUrls
          }
        }
        return { ...p, imageUrls: urls }
      }))
      currentPage++
    }
  } catch (err) {
    console.error('Search failed:', err)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  onSearch(false)
}

const distributePosts = (newPosts) => {
  newPosts.forEach(post => {
    let h = 400
    const match = post.imageUrls[0]?.match(/_w(\d+)_h(\d+)/)
    if (match) {
      const w = parseInt(match[1])
      const h_orig = parseInt(match[2])
      h = 340 * Math.max(0.75, Math.min(1.33, h_orig / w))
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

const goToDetail = (id) => uni.navigateTo({ url: `/pages/post-detail/post-detail?id=${id}` })
const goBack = () => uni.navigateBack()
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
    gap: 20rpx;
    
    .back-btn {
      width: 70rpx;
      height: 70rpx;
      border-radius: 16rpx;
    }
    
    .search-input {
      flex: 1;
      height: 70rpx;
      border-radius: 35rpx;
      padding: 0 30rpx;
      .input {
        width: 100%;
        font-size: 26rpx;
      }
    }
  }
}

.hot-search {
  margin: 30rpx;
  padding: 30rpx;
  .title {
    font-size: 32rpx;
    font-weight: 900;
    display: block;
    margin-bottom: 30rpx;
  }
  .hot-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
    .hot-tag {
      padding: 10rpx 30rpx;
      font-size: 24rpx;
      border-radius: 30rpx;
    }
  }
}

.waterfall {
  display: flex;
  padding: 20rpx;
  gap: 20rpx;
  .column { flex: 1; display: flex; flex-direction: column; gap: 20rpx; }
}

.loading-status {
  text-align: center;
  padding: 20rpx;
  font-size: 24rpx;
  opacity: 0.5;
  color: var(--text-main);
}
</style>

