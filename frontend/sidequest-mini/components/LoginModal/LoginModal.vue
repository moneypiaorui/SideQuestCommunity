<template>
  <view v-if="bus.showLogin" class="modal-mask" @click="close">
    <view class="modal-container" @click.stop>
      <view class="logo-box brutal-card">
        <text class="logo-text">SideQuest</text>
      </view>
      
      <view class="form-box">
        <text class="title">{{ mode === 'login' ? 'IDENTITY CHECK' : 'NEW ADVENTURER' }}</text>
        <view class="input-group">
          <input 
            v-if="mode === 'register'"
            v-model="form.nickname" 
            class="brutal-input brutal-card" 
            placeholder="昵称" 
          />
          <input 
            v-model="form.username" 
            class="brutal-input brutal-card" 
            placeholder="用户名" 
          />
          <input 
            v-model="form.password" 
            type="password"
            class="brutal-input brutal-card" 
            placeholder="密码" 
          />
        </view>
        
        <button class="brutal-btn primary login-btn" :loading="loading" @click="handleSubmit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </button>
        
        <view class="footer-links" @click="toggleMode">
          <text class="link">{{ mode === 'login' ? '还没有账号？去注册' : '已有账号？去登录' }}</text>
        </view>
      </view>
      <view class="close-icon" @click="close">✕</view>
    </view>

    <!-- 成功提示弹窗 (比登录框高一级) -->
    <view v-if="showSuccess" class="success-overlay">
      <view class="success-box brutal-card">
        <view class="check-icon">✓</view>
        <text class="success-text">{{ successMsg }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { bus } from '@/utils/bus'
import request from '@/utils/request'

const loading = ref(false)
const mode = ref('login') // 'login' or 'register'
const showSuccess = ref(false)
const successMsg = ref('')

const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

const close = () => {
  bus.closeLogin()
  mode.value = 'login'
}

const toggleMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

const handleSubmit = async () => {
  if (!form.username || !form.password || (mode.value === 'register' && !form.nickname)) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  
  loading.value = true
  try {
    if (mode.value === 'register') {
      await request({
        url: '/api/identity/register',
        method: 'POST',
        data: {
          username: form.username,
          password: form.password,
          nickname: form.nickname
        }
      })
    }

    const res = await request({
      url: '/api/identity/login',
      method: 'POST',
      data: {
        username: form.username,
        password: form.password
      }
    })
    
    uni.setStorageSync('token', res.token)
    uni.setStorageSync('userInfo', res)
    uni.setStorageSync('userId', res.userId)
    
    // 显示更高级别的成功弹窗
    successMsg.value = mode.value === 'register' ? '注册并登录成功' : '登录成功'
    showSuccess.value = true
    
      setTimeout(() => {
      showSuccess.value = false
      bus.closeLogin()
      // 触发全局登录成功事件
      uni.$emit('loginSuccess')
      // 兼容非 setup 页面
      const pages = getCurrentPages()
      const curPage = pages[pages.length - 1]
      if (curPage && curPage.onLoad) {
        curPage.onLoad(curPage.options)
      }
    }, 1500)
  } catch (err) {
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  z-index: 3000;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.modal-container {
  width: 100%;
  background: var(--bg-main);
  border: 4rpx solid #000;
  border-bottom: none;
  border-radius: 48rpx 48rpx 0 0;
  padding: 60rpx 40rpx calc(60rpx + env(safe-area-inset-bottom));
  animation: slide-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.close-icon {
  position: absolute;
  top: 30rpx;
  right: 40rpx;
  font-size: 40rpx;
  font-weight: 800;
}

.logo-box {
  width: 160rpx;
  height: 160rpx;
  background-color: var(--primary);
  margin: 0 auto 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-5deg);
  
  .logo-text {
    font-size: 28rpx;
    font-weight: 900;
    color: #fff;
    transform: rotate(5deg);
  }
}

.form-box {
  .title {
    font-size: 40rpx;
    font-weight: 900;
    font-style: italic;
    margin-bottom: 40rpx;
    display: block;
    text-align: center;
    color: var(--text-main);
  }
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 40rpx;
  
  .brutal-input {
    height: 90rpx;
    padding: 0 30rpx;
    font-size: 28rpx;
    font-weight: 800;
    background: var(--surface);
    color: var(--text-main);
  }
}

.login-btn {
  width: 100%;
  height: 90rpx;
  font-size: 30rpx;
}

.footer-links {
  margin-top: 30rpx;
  text-align: center;
  
  .link {
    font-size: 22rpx;
    font-weight: 700;
    opacity: 0.6;
    text-decoration: underline;
    color: var(--text-main);
  }
}

.success-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 4000; // 比 modal-mask (3000) 更高
  display: flex;
  align-items: center;
  justify-content: center;
  
  .success-box {
    background: var(--primary);
    padding: 60rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 30rpx;
    transform: rotate(2deg);
    
    .check-icon {
      font-size: 80rpx;
      color: #fff;
      font-weight: 900;
    }
    
    .success-text {
      font-size: 36rpx;
      font-weight: 900;
      color: #fff;
    }
  }
}
</style>

