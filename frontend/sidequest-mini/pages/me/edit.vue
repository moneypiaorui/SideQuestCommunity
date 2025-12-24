<template>
  <view :class="{ 'dark': isDark }" class="page-container">
    <view class="fixed-header">
      <view class="status-bar" :style="{ height: statusBarHeight + 'px' }" />
      <view class="nav-bar">
        <view class="back-btn brutal-btn" @click="goBack"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="15 18 9 12 15 6"></polyline></svg></view>
        <text class="title">编辑个人资料</text>
      </view>
    </view>
    
    <scroll-view scroll-y class="content-scroll">
      <view class="form-container">
        <view class="avatar-edit brutal-card" @click="changeAvatar">
          <image :src="user.avatar" class="main-avatar brutal-card" />
          <view class="edit-badge brutal-btn primary">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="4"><path d="M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
          </view>
          <text class="hint">点击更换头像</text>
        </view>
        
        <view class="input-group">
          <view class="input-item">
            <text class="label">昵称</text>
            <input v-model="user.nickname" class="brutal-input brutal-card" placeholder="起个酷点的名字" />
          </view>
          
          <view class="input-item">
            <text class="label">个人简介 (最多50字)</text>
            <textarea v-model="user.signature" class="brutal-textarea brutal-card" placeholder="向大家介绍一下你自己..." maxlength="50" />
          </view>
        </view>
        
        <button class="brutal-btn primary save-btn" @click="save">保存所有修改</button>
      </view>
    </scroll-view>
    <LoginModal />
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false); const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight)
const user = ref({ nickname: '', avatar: '', signature: '' })

onMounted(async () => { 
  const res = await request({ url: '/api/identity/me' })
  user.value = res
})

const changeAvatar = () => {
  uni.chooseImage({
    count: 1,
    success: async (res) => {
      const tempPath = res.tempFilePaths[0]
      try {
        // 1. 获取上传链接
        const fileName = `avatar_${Date.now()}.jpg`
        const uploadUrl = await request({
          url: `/api/media/upload-url?fileName=${encodeURIComponent(fileName)}`
        })

        // 2. 将图片转为 ArrayBuffer (更兼容二进制发送)
        const binaryData = await new Promise((resolve, reject) => {
          if (typeof window !== 'undefined') {
            // H5 环境
            fetch(tempPath)
              .then(r => r.arrayBuffer())
              .then(resolve)
              .catch(reject)
          } else {
            // 小程序/App 环境
            uni.getFileSystemManager().readFile({
              filePath: tempPath,
              success: r => resolve(r.data),
              fail: reject
            })
          }
        })

        await new Promise((resolve, reject) => {
          uni.request({
            url: uploadUrl,
            method: 'PUT',
            data: binaryData,
            header: { 
              'Content-Type': 'image/jpeg'
            },
            success: (r) => {
              if (r.statusCode === 200 || r.statusCode === 204) resolve(r)
              else reject(new Error('Upload failed: ' + r.statusCode))
            },
            fail: reject
          })
        })

        // 3. 更新本地显示
        user.value.avatar = uploadUrl.split('?')[0]
      } catch (err) {
        console.error('Upload error:', err)
        uni.showToast({ title: '头像上传失败: ' + (err.message || ''), icon: 'none' })
      }
    }
  })
}

const save = async () => {
  try {
    await request({ 
      url: '/api/identity/profile', 
      method: 'PUT', 
      data: {
        nickname: user.value.nickname,
        avatar: user.value.avatar,
        signature: user.value.signature
      } 
    })
    uni.showToast({ title: '资料已更新', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (err) {}
}
const goBack = () => uni.navigateBack()
</script>

<style lang="scss" scoped>
.page-container { height: 100vh; display: flex; flex-direction: column; background: var(--bg-main); }
.fixed-header { background: var(--surface); border-bottom: 4rpx solid #000; z-index: 100; .nav-bar { height: 100rpx; display: flex; align-items: center; padding: 0 30rpx; gap: 30rpx; .back-btn { width: 70rpx; height: 70rpx; border-radius: 16rpx; } .title { font-size: 32rpx; font-weight: 800; } } }
.content-scroll { flex: 1; }
.form-container { padding: 40rpx; .avatar-edit { width: 220rpx; height: 220rpx; margin: 40rpx auto 60rpx; position: relative; background: var(--surface); display: flex; flex-direction: column; align-items: center; justify-content: center;
    .main-avatar { width: 160rpx; height: 160rpx; border-radius: 50%; }
    .edit-badge { position: absolute; bottom: 10rpx; right: 10rpx; width: 50rpx; height: 50rpx; border-radius: 50%; }
    .hint { font-size: 20rpx; margin-top: 10rpx; opacity: 0.5; font-weight: 700; }
  }
}
.input-group { display: flex; flex-direction: column; gap: 40rpx; margin-bottom: 60rpx;
  .label { font-size: 28rpx; font-weight: 800; margin-bottom: 16rpx; display: block; color: var(--text-main); }
  .brutal-input { height: 100rpx; padding: 0 30rpx; font-size: 28rpx; background: var(--surface); }
  .brutal-textarea { height: 200rpx; padding: 30rpx; font-size: 28rpx; width: 100%; box-sizing: border-box; background: var(--surface); }
}
.save-btn { width: 100%; height: 100rpx; font-size: 32rpx; }
</style>
