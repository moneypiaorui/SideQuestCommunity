<script>
import { reactive, provide } from 'vue'

export default {
  onLaunch: function() {
    console.log('App Launch')
    // Check system dark mode or stored preference
    const isDark = uni.getStorageSync('isDark') || false
    this.globalData.isDark = isDark
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  },
  globalData: reactive({
    isDark: false,
    toggleDark() {
      this.isDark = !this.isDark
      uni.setStorageSync('isDark', this.isDark)
    }
  })
}
</script>

<style lang="scss">
/* 全局样式在 uni.scss 中定义，这里可以添加一些无法在 uni.scss 中使用的样式 */
</style>

