<template>
  <view class="login-page" :class="{ 'dark': isDark }">
    <view class="login-container">
      <view class="logo-box brutal-card">
        <text class="logo-text">SideQuest</text>
      </view>
      
      <view class="form-box">
        <text class="title">HELLO AGAIN!</text>
        <view class="input-group">
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
        
        <button class="brutal-btn primary login-btn" :loading="loading" @click="handleLogin">
          登录
        </button>
        
        <view class="footer-links">
          <text class="link" @click="goToRegister">还没有账号？去注册</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import request from '@/utils/request'

const isDark = ref(uni.getStorageSync('isDark') || false)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  
  loading.value = true
  try {
    const res = await request({
      url: '/api/identity/login',
      method: 'POST',
      data: form
    })
    
    uni.setStorageSync('token', res.token)
    uni.setStorageSync('userInfo', res)
    
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 1500)
  } catch (err) {
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  // Implementation omitted for brevity
}
</script>

<style lang="scss" scoped>
.login-page {
  height: 100vh;
  background-color: var(--bg-main);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.login-container {
  width: 100%;
  max-width: 600rpx;
}

.logo-box {
  width: 200rpx;
  height: 200rpx;
  background-color: var(--primary);
  margin: 0 auto 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: rotate(-5deg);
  
  .logo-text {
    font-size: 32rpx;
    font-weight: 900;
    color: #fff;
    transform: rotate(5deg);
  }
}

.form-box {
  background-color: var(--surface);
  padding: 60rpx 40rpx;
  border: 4rpx solid #000;
  box-shadow: 12rpx 12rpx 0px 0px #000;
  border-radius: 32rpx;
  
  .title {
    font-size: 48rpx;
    font-weight: 900;
    font-style: italic;
    margin-bottom: 60rpx;
    display: block;
    text-align: center;
  }
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  margin-bottom: 60rpx;
  
  .brutal-input {
    height: 100rpx;
    padding: 0 30rpx;
    font-size: 30rpx;
    font-weight: 800;
  }
}

.login-btn {
  width: 100%;
  height: 100rpx;
  font-size: 32rpx;
}

.footer-links {
  margin-top: 40rpx;
  text-align: center;
  
  .link {
    font-size: 24rpx;
    font-weight: 700;
    opacity: 0.6;
    text-decoration: underline;
  }
}
</style>

