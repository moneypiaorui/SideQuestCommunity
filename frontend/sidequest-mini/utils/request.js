// utils/request.js
import { bus } from "./bus"

// const BASE_URL = "http://localhost:8080"
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ""

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync("token")

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || "GET",
      data: options.data || {},
      header: {
        Authorization: token ? `Bearer ${token}` : "",
        ...options.header,
      },
      success: (res) => {
        // 兼容业务码(res.data.code)和HTTP状态码(res.statusCode)
        const code = res.data?.code || res.statusCode

        if (code === 200 || res.statusCode === 200) {
          resolve(res.data?.data || res.data)
        } else if (code === 401 || res.statusCode === 401 || code === 403 || res.statusCode === 403) {
          bus.openLogin()
          // 监听登录成功事件，完成后刷新当前页面
          uni.$once("loginSuccess", () => {
            const pages = getCurrentPages()
            const curPage = pages[pages.length - 1]
            if (curPage && typeof curPage.onLoad === "function") {
              curPage.onLoad(curPage.options)
            }
          })
          reject(res.data || { message: "Unauthorized" })
        } else {
          uni.showToast({
            title: res.data.message || "请求失败",
            icon: "none",
          })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: "网络错误",
          icon: "none",
        })
        reject(err)
      },
    })
  })
}

export default request
