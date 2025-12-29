<template>
  <view class="publish-page" :class="{ 'dark': isDark }">
    <view class="fixed-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="15 18 9 12 15 6"></polyline></svg></view>
        <text class="title">发布新笔记</text>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll">
      <view class="media-section">
      <view v-if="tempMedia" class="media-preview-container">
        <view class="media-preview brutal-card">
          <image :src="tempMedia.type === 'video' ? (tempMedia.thumbPath || tempMedia.path) : tempMedia.path" mode="aspectFit" class="preview-img" />
          <view v-if="tempMedia.type === 'video'" class="video-badge">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"></polygon>
            </svg>
          </view>
        </view>
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
        <view class="selector brutal-btn tag-btn" @click="showTagInput = true">
          <text class="tag-display-text">{{ form.tags.length ? form.tags.join(', ') : '添加标签' }}</text>
        </view>
      </view>
    </view>
  </scroll-view>
    
    <view class="footer safe-area-bottom">
      <button class="brutal-btn primary submit-btn" :loading="submitting" @click="submit">
        发布笔记
      </button>
    </view>
    
    <!-- Tag Input Modal -->
    <view v-if="showTagInput" class="modal-mask" @click="showTagInput = false">
      <view class="modal-content brutal-card" @click.stop>
        <text class="modal-title">添加标签</text>
        <view class="input-row">
          <input v-model="newTag" class="brutal-input brutal-card" placeholder="输入标签名..." @confirm="addTag" />
          <view class="add-tag-btn brutal-btn primary" @click="addTag">添加</view>
        </view>
        <view class="tag-list">
          <view v-for="(tag, i) in form.tags" :key="i" class="tag-item brutal-card" @click="removeTag(i)">
            {{ tag }} <text class="close-icon">✕</text>
          </view>
        </view>
        <button class="brutal-btn primary confirm-btn" @click="showTagInput = false">确定</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
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
  videoDuration: 0,
  mediaId: null
})

onMounted(async () => {
  try {
    const data = await request({ url: '/api/core/sections' })
    sections.value = data
  } catch (err) {}
})

const goBack = () => {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/index/index' })
  }
}

const chooseMedia = () => {
  // H5 兼容性处理
  const isH5 = typeof window !== 'undefined'
  
  if (isH5) {
    uni.showActionSheet({
      itemList: ['选择图片', '选择视频'],
      success: (res) => {
        if (res.tapIndex === 0) {
          uni.chooseImage({
            count: 1,
            success: (imgRes) => {
              const file = imgRes.tempFiles[0]
        tempMedia.value = {
          path: file.path,
          type: 'image',
          size: file.size,
          width: 0,
          height: 0
        }
        uni.getImageInfo({
          src: file.path,
          success: (info) => {
            tempMedia.value.width = info.width
            tempMedia.value.height = info.height
          }
        })
            }
          })
        } else {
          uni.chooseVideo({
            count: 1,
            sourceType: ['album', 'camera'],
            success: (vidRes) => {
              tempMedia.value = {
                path: vidRes.tempFilePath,
                thumbPath: vidRes.thumbTempFilePath,
                type: 'video',
                size: vidRes.size,
                duration: vidRes.duration,
                width: vidRes.width,
                height: vidRes.height
              }
            }
          })
        }
      }
    })
  } else {
    uni.chooseMedia({
      count: 1,
      mediaType: ['image', 'video'],
      success: async (res) => {
        const file = res.tempFiles[0]
        tempMedia.value = {
          path: file.tempFilePath,
          thumbPath: file.thumbTempFilePath, // 捕获视频封面/第一帧
          type: file.fileType,
          size: file.size,
          duration: file.duration || 0,
          width: file.width || 0,
          height: file.height || 0
        }
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
    // 定义统一上传函数
    const uploadFile = async (filePath, fileType) => {
      let ext = fileType === 'video' ? 'mp4' : 'jpg'
      
      // 提取真实扩展名
      if (filePath && filePath.includes('.')) {
        const parts = filePath.split('?')[0].split('.')
      const lastPart = parts.pop().toLowerCase()
        if (['jpg', 'jpeg', 'png', 'gif', 'mp4', 'mov', 'webp', 'm4v'].includes(lastPart)) {
        ext = lastPart
      }
      } else if (fileType === 'image') {
        ext = 'jpg'
      } else if (fileType === 'video') {
      ext = 'mp4'
    }
    
      const fileName = `post_${Date.now()}_${Math.floor(Math.random() * 1000)}.${ext}`
    const uploadUrl = await request({
      url: `/api/media/upload-url?fileName=${encodeURIComponent(fileName)}`
    })
    
    const binaryData = await new Promise((resolve, reject) => {
        if (typeof window !== 'undefined' && filePath.startsWith('blob:')) {
          // H5 Blob 处理
          fetch(filePath).then(res => res.arrayBuffer()).then(resolve).catch(reject)
        } else if (typeof window !== 'undefined') {
          // 普通 H5 文件处理
          fetch(filePath).then(res => res.arrayBuffer()).then(resolve).catch(reject)
      } else {
          // 小程序/App 离线文件读取
          uni.getFileSystemManager().readFile({ 
            filePath, 
          success: res => resolve(res.data),
            fail: (err) => {
              console.error('File read failed:', err)
              reject(new Error('无法读取本地文件: ' + filePath))
            } 
        })
      }
    })

    await new Promise((resolve, reject) => {
      uni.request({
        url: uploadUrl,
        method: 'PUT',
        data: binaryData,
        header: {
            'Content-Type': fileType === 'video' ? 'video/mp4' : (ext === 'png' ? 'image/png' : 'image/jpeg') 
        },
        success: (res) => {
          if (res.statusCode === 200 || res.statusCode === 204) resolve(res)
            else reject(new Error('上传到存储服务失败: ' + res.statusCode))
        },
          fail: (err) => reject(new Error('网络请求失败: ' + err.errMsg))
      })
    })
    
      return uploadUrl.split('?')[0]
    }

    // 1. 上传主媒体文件
    const mediaUrl = await uploadFile(tempMedia.value.path, tempMedia.value.type)
    
    // 2. 如果是视频，上传封面
    let coverUrl = ''
    if (tempMedia.value.type === 'video' && tempMedia.value.thumbPath) {
      coverUrl = await uploadFile(tempMedia.value.thumbPath, 'image')
    }

    // 3. Register Media in Media Service
    const registeredMedia = await request({
      url: '/api/media/register',
      method: 'POST',
      data: {
        fileName: mediaUrl.split('/').pop(),
        fileKey: mediaUrl.split('/').pop(),
        fileType: tempMedia.value.type,
        url: mediaUrl
      }
    })

    // 4. Create Post
    if (tempMedia.value.type === 'image') {
      form.imageUrls = [mediaUrl]
    } else {
      form.videoUrl = mediaUrl
      form.videoCoverUrl = coverUrl
      form.videoDuration = Math.round(tempMedia.value.duration)
      form.mediaId = registeredMedia.id // 关联 Media ID
    }
    
    const postId = await request({
      url: '/api/core/posts',
      method: 'POST',
      data: {
        ...form,
        tags: form.tags,
        imageUrls: form.imageUrls,
        videoCoverUrl: form.videoCoverUrl,
        mediaId: form.mediaId // 发送 Media ID
      }
    })
    
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      if (postId) {
        uni.redirectTo({ url: `/pages/post-detail/post-detail?id=${postId}` })
      } else {
        uni.navigateBack()
      }
    }, 1500)
  } catch (err) {
    console.error(err)
    uni.showToast({ title: '发布失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.publish-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-main);
}

.fixed-header { 
  background: var(--surface); 
  border-bottom: 4rpx solid #000; 
  z-index: 100; 
  .nav-bar { 
    height: 100rpx; 
    display: flex; 
    align-items: center; 
    padding: 0 30rpx; 
    gap: 30rpx; 
    .back-btn { width: 70rpx; height: 70rpx; border-radius: 16rpx; } 
    .title { font-size: 32rpx; font-weight: 800; color: var(--text-main); } 
  } 
}

.content-scroll {
  flex: 1;
  padding: 30rpx 40rpx 30rpx 30rpx;
  box-sizing: border-box;
  overflow-y: auto;
}

.media-section {
  width: 100%;
  margin-bottom: 40rpx;
  box-sizing: border-box;
  padding-right: 12rpx;
  
  .media-preview-container {
    width: 100%;
    position: relative;
    padding-top: 20rpx; // 为顶部按钮留出空间
    padding-right: 20rpx; // 为右侧按钮留出空间
    box-sizing: border-box;

    .media-preview {
      width: 100%;
      height: 400rpx;
      position: relative;
      box-sizing: border-box;
      
      .preview-img {
        width: 100%;
        height: 100%;
      }

      .video-badge {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        color: rgba(255, 255, 255, 0.8);
        filter: drop-shadow(2px 2px 0px #000);
        pointer-events: none;
      }
    }
    
    .remove-btn {
      position: absolute;
      top: 0;
      right: 0;
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
      z-index: 10;
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
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  box-sizing: border-box;
  padding-right: 12rpx;
  
  .brutal-input {
    width: 100%;
    height: 100rpx;
    padding: 0 30rpx;
    font-size: 32rpx;
    font-weight: 800;
    box-sizing: border-box;
  }
  
  .brutal-textarea {
    width: 100%;
    height: 300rpx;
    padding: 30rpx;
    font-size: 28rpx;
    font-weight: 600;
    box-sizing: border-box;
  }
}

.selector-group {
  display: flex;
  gap: 20rpx;
  margin-bottom: 40rpx;
  
  picker {
    flex: 2; // 分区占更多空间
  }
  
  .selector {
    width: 100%;
    height: 90rpx;
    font-size: 26rpx;
    box-sizing: border-box;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding: 0 20rpx;
  }
  
  .tag-btn {
    flex: 3;
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

  .input-row {
    display: flex;
    gap: 20rpx;
    .brutal-input { flex: 1; height: 90rpx; padding: 0 20rpx; }
    .add-tag-btn { width: 120rpx; height: 90rpx; font-size: 26rpx; }
  }
  
  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    min-height: 100rpx;
    
    .tag-item {
      padding: 10rpx 20rpx;
      font-size: 24rpx;
      background: var(--primary);
      color: #fff;
      display: flex;
      align-items: center;
      gap: 10rpx;
      .close-icon { font-size: 20rpx; opacity: 0.8; }
    }
  }

  .confirm-btn {
    height: 90rpx;
    width: 100%;
  }
}

.tag-display-text {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}
</style>

