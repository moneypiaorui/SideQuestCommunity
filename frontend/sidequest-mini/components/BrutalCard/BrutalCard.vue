<template>
  <view class="brutal-card" @click="$emit('click')">
    <view class="media-container" :style="{ height: cardHeight + 'rpx' }">
      <image 
        :src="post.imageUrls[0]" 
        mode="aspectFill" 
        class="main-image"
        @load="onImageLoad"
      />
      <view v-if="post.videoUrl" class="video-badge">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <polygon points="5 3 19 12 5 21 5 3"></polygon>
        </svg>
      </view>
    </view>
    
    <view class="content">
      <text class="title">{{ post.title }}</text>
      <view class="footer">
        <view class="author">
          <image :src="post.authorAvatar || '/static/default-avatar.png'" class="avatar" />
          <text class="author-name">{{ post.authorName }}</text>
        </view>
        <view class="likes" @click.stop="toggleLike">
          <svg width="28" height="28" viewBox="0 0 24 24" 
               :fill="post.hasLiked ? 'var(--accent-red)' : 'none'" 
               :stroke="post.hasLiked ? 'var(--accent-red)' : 'currentColor'" 
               stroke-width="3"
               :class="{ 'animate-heart': post.hasLiked }">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
          </svg>
          <text class="like-count" :class="{ 'liked': post.hasLiked }">{{ post.likeCount }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click', 'like'])

// Image size handling
const imgWidth = ref(0)
const imgHeight = ref(0)

const onImageLoad = (e) => {
  imgWidth.value = e.detail.width
  imgHeight.value = e.detail.height
}

const cardHeight = computed(() => {
  // Constraints: 3:4 to 4:3
  // 3:4 = 0.75, 4:3 = 1.33
  if (!imgWidth.value || !imgHeight.value) return 400 // default height
  
  const ratio = imgHeight.value / imgWidth.value
  const constrainedRatio = Math.max(0.75, Math.min(1.33, ratio))
  
  // Base width for card is roughly 340rpx (half screen minus margins)
  return 340 * constrainedRatio
})

const toggleLike = () => {
  emit('like', props.post.id)
}
</script>

<style lang="scss" scoped>
.media-container {
  width: 100%;
  background-color: #eee;
  position: relative;
  overflow: hidden;
  
  .main-image {
    width: 100%;
    height: 100%;
    display: block;
  }
  
  .video-badge {
    position: absolute;
    top: 20rpx;
    right: 20rpx;
    color: #fff;
    filter: drop-shadow(2px 2px 0px #000);
  }
}

.content {
  padding: 16rpx;
  
  .title {
    font-size: 26rpx;
    font-weight: 800;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    margin-bottom: 16rpx;
  }
  
  .footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .author {
      display: flex;
      align-items: center;
      
      .avatar {
        width: 36rpx;
        height: 36rpx;
        border-radius: 50%;
        border: 2rpx solid var(--border-color);
        margin-right: 8rpx;
      }
      
      .author-name {
        font-size: 20rpx;
        font-weight: 600;
        opacity: 0.7;
      }
    }
    
    .likes {
      display: flex;
      align-items: center;
      
      .like-count {
        font-size: 22rpx;
        font-weight: 800;
        margin-left: 4rpx;
        
        &.liked {
          color: var(--accent-red);
        }
      }
    }
  }
}

@keyframes heart-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.4); }
  100% { transform: scale(1); }
}

.animate-heart {
  animation: heart-pop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
</style>

