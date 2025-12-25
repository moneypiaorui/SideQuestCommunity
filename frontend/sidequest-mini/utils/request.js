// utils/request.js
import { IS_LOGGED_IN, mockPosts, mockSections, mockUserInfo, mockUnreadCounts, mockChatRooms, mockOtherUser, mockChatMessages, mockComments } from './mock'
import { bus } from './bus'

// const BASE_URL = 'http://localhost:8080'
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const USE_MOCK = false 

const request = (options) => {
  if (USE_MOCK) {
    return new Promise((resolve) => {
      console.log(`[Mock Request]: ${options.url}`)
      
      // 模拟 Token 逻辑
      if (IS_LOGGED_IN && !uni.getStorageSync('token')) {
        uni.setStorageSync('token', 'mock_token_123456')
        uni.setStorageSync('userInfo', mockUserInfo)
      }

      setTimeout(() => {
        if (options.url.includes('/api/core/interactions/comments')) {
          resolve(mockComments)
        } else if (options.url.includes('/api/core/posts/')) {
          const id = parseInt(options.url.split('/').pop())
          const post = mockPosts.find(p => p.id === id) || mockPosts[0]
          resolve(post)
        } else if (options.url.includes('/api/core/posts')) {
          resolve({ records: mockPosts, total: mockPosts.length })
        } else if (options.url.includes('/api/core/sections')) {
          resolve(mockSections)
        } else if (options.url.includes('/api/identity/me')) {
          resolve(mockUserInfo)
        } else if (options.url.includes('/api/identity/users/') && options.url.includes('/public')) {
          resolve(mockOtherUser)
        } else if (options.url.includes('/api/notifications/unread-count')) {
          resolve({ chat: 2, interaction: 5, system: 0 })
        } else if (options.url.includes('/api/chat/rooms') && options.url.includes('/messages')) {
          resolve(mockChatMessages)
        } else if (options.url.includes('/api/chat/rooms')) {
          resolve([
            {
              id: 1,
              recipientNickname: "野兽派小秘书",
              recipientAvatar: "https://picsum.photos/100/100?random=20",
              lastMessage: "欢迎来到新野兽主义的世界！",
              lastMessageTime: new Date().toISOString(),
              unreadCount: 1
            }
          ])
        } else if (options.url.includes('/api/search/posts')) {
          resolve({ content: mockPosts })
        } else if (options.url.includes('/api/search/user/posts')) {
          resolve({ content: mockPosts.filter(p => p.authorId === 102) })
        } else if (options.url.includes('/api/media/upload-url')) {
          resolve('http://localhost:8080/mock-upload-path')
        } else {
          resolve('mock success')
        }
      }, 300)
    })
  }

  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        // 兼容业务码 (res.data.code) 和 HTTP 状态码 (res.statusCode)
        const code = res.data?.code || res.statusCode
        
        if (code === 200 || res.statusCode === 200) {
          resolve(res.data?.data || res.data)
        } else if (code === 401 || res.statusCode === 401 || code === 403 || res.statusCode === 403) {
          bus.openLogin()
          // 监听登录成功事件，完成后刷新当前页面
          uni.$once('loginSuccess', () => {
            const pages = getCurrentPages()
            const curPage = pages[pages.length - 1]
            if (curPage && typeof curPage.onLoad === 'function') {
              curPage.onLoad(curPage.options)
            }
          })
          reject(res.data || { message: 'Unauthorized' })
        } else {
          uni.showToast({
            title: res.data.message || '请求失败',
            icon: 'none'
          })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络错误',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

export default request
