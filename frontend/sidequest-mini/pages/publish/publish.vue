<template>
  <view class="publish-page" :class="{ 'dark': isDark }">
    <view class="media-section">
      <view v-if="tempMedia" class="media-preview brutal-card">
        <image :src="tempMedia.path" mode="aspectFit" class="preview-img" />
        <view class="remove-btn" @click="tempMedia = null">✕</view>
      </view>
      <view v-else class="upload-placeholder brutal-btn" @click="chooseMedia">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <text class="placeholder-text">上传图片/视频</text>
      </view>
    </view>
    
    <view class="form-section">
      <input 
        v-model="form.title" 
        class="brutal-input brutal-card" 
        placeholder="输入标题..." 
        placeholder-class="placeholder"
      />
      <textarea 
        v-model="form.content" 
        class="brutal-textarea brutal-card" 
        placeholder="分享你的游戏趣事..." 
        placeholder-class="placeholder"
      />
      
      <view class="selector-group">
        <picker :range="sections" range-key="displayNameZh" @change="onSectionChange">
          <view class="selector brutal-btn">
            <text>{{ selectedSectionName || '选择分区' }}</text>
          </view>
        </picker>
        <view class="selector brutal-btn" @click="showTagInput = true">
          <text>{{ form.tags.length ? form.tags.join(', ') : '添加标签' }}</text>
        </view>
      </view>
    </view>
    
    <view class="footer safe-area-bottom">
      <button class="brutal-btn primary submit-btn" :loading="submitting" @click="submit">
        发布笔记
      </button>
    </view>
    
    <!-- Tag Input Modal -->
    <view v-if="showTagInput" class="modal-mask" @click="showTagInput = false">
      <view class="modal-content brutal-card" @click.stop>
        <text class="modal-title">添加标签</text>
        <input v-model="newTag" class="brutal-input" placeholder="输入标签名..." @confirm="addTag" />
        <view class="tag-list">
          <view v-for="(tag, i) in form.tags" :key="i" class="tag-item brutal-card" @click="removeTag(i)">
            {{ tag }} ✕
          </view>
        </view>
        <button class="brutal-btn primary" @click="showTagInput = false">确定</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const tempMedia = ref(null)
const submitting = ref(false)
const showTagInput = ref(false)
const newTag = ref('')
const sections = ref([])
const selectedSectionName = ref('')

const form = reactive({
  title: '',
  content: '',
  sectionId: '',
  tags: [],
  imageUrls: [],
  videoUrl: '',
  videoCoverUrl: '',
  videoDuration: 0
})

onMounted(async () => {
  try {
    const data = await request({ url: '/api/core/sections' })
    sections.value = data
  } catch (err) {}
})

const chooseMedia = () => {
  uni.chooseMedia({
    count: 1,
    mediaType: ['image', 'video'],
    success: async (res) => {
      const file = res.tempFiles[0]
      tempMedia.value = {
        path: file.tempFilePath,
        type: file.fileType,
        size: file.size,
        duration: file.duration || 0,
        width: file.width || 0,
        height: file.height || 0
      }
      
      // If it's an image and width/height are missing, get them
      if (file.fileType === 'image' && (!file.width || !file.height)) {
        uni.getImageInfo({
          src: file.tempFilePath,
          success: (info) => {
            tempMedia.value.width = info.width
            tempMedia.value.height = info.height
          }
        })
      }
    }
  })
}

const onSectionChange = (e) => {
  const index = e.detail.value
  form.sectionId = sections.value[index].id
  selectedSectionName.value = sections.value[index].displayNameZh
}

const addTag = () => {
  if (newTag.value && !form.tags.includes(newTag.value)) {
    form.tags.push(newTag.value)
    newTag.value = ''
  }
}

const removeTag = (index) => {
  form.tags.splice(index, 1)
}

const submit = async () => {
  if (!form.title || !form.content) {
    uni.showToast({ title: '请填写完整内容', icon: 'none' })
    return
  }
  if (!tempMedia.value) {
    uni.showToast({ title: '请上传媒体文件', icon: 'none' })
    return
  }
  
  submitting.value = true
  try {
    // 1. Get Upload URL
    // Carry size info in filename or metadata as per requirement
    const ext = tempMedia.value.path.split('.').pop()
    const fileName = `post_${Date.now()}_w${tempMedia.value.width}_h${tempMedia.value.height}.${ext}`
    
    const uploadUrl = await request({
      url: `/api/media/upload-url?fileName=${encodeURIComponent(fileName)}`
    })
    
    // 2. Upload to MinIO
    await new Promise((resolve, reject) => {
      uni.uploadFile({
        url: uploadUrl,
        filePath: tempMedia.value.path,
        name: 'file',
        success: resolve,
        fail: reject
      })
    })
    
    // 3. Create Post
    const mediaUrl = uploadUrl.split('?')[0]
    if (tempMedia.value.type === 'image') {
      form.imageUrls = [mediaUrl]
    } else {
      form.videoUrl = mediaUrl
      form.videoDuration = Math.round(tempMedia.value.duration)
    }
    
    await request({
      url: '/api/core/posts',
      method: 'POST',
      data: {
        ...form,
        tags: JSON.stringify(form.tags),
        imageUrls: JSON.stringify(form.imageUrls)
      }
    })
    
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      uni.navigateBack()
    }, 1500)
  } catch (err) {
    console.error(err)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  min-height: 100vh;
  background-color: var(--bg-main);
  padding: 30rpx;
}

.media-section {
  margin-bottom: 40rpx;
  
  .media-preview {
    width: 100%;
    height: 400rpx;
    position: relative;
    
    .preview-img {
      width: 100%;
      height: 100%;
    }
    
    .remove-btn {
      position: absolute;
      top: -20rpx;
      right: -20rpx;
      width: 50rpx;
      height: 50rpx;
      background: var(--accent-red);
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 4rpx solid #000;
      font-weight: 800;
    }
  }
  
  .upload-placeholder {
    width: 100%;
    height: 400rpx;
    flex-direction: column;
    gap: 20rpx;
    color: var(--text-main);
    opacity: 0.5;
  }
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  
  .brutal-input {
    height: 100rpx;
    padding: 0 30rpx;
    font-size: 32rpx;
    font-weight: 800;
  }
  
  .brutal-textarea {
    height: 300rpx;
    padding: 30rpx;
    font-size: 28rpx;
    font-weight: 600;
  }
}

.selector-group {
  display: flex;
  gap: 20rpx;
  
  .selector {
    flex: 1;
    height: 80rpx;
    font-size: 24rpx;
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30rpx;
  background-color: var(--surface);
  border-top: 4rpx solid var(--border-color);
  
  .submit-btn {
    width: 100%;
    height: 100rpx;
    font-size: 32rpx;
  }
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.modal-content {
  width: 100%;
  background: var(--surface);
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  
  .modal-title {
    font-size: 36rpx;
    font-weight: 800;
  }
  
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    
    .tag-item {
      padding: 10rpx 20rpx;
      font-size: 24rpx;
      background: var(--primary);
      color: #fff;
    }
  }
}
</style>

