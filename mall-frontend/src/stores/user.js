import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态 Store
 * 管理登录状态、Token、用户信息
 * 使用 localStorage 持久化，刷新页面后不丢失登录状态
 */
export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复 Token
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 登录成功后保存 Token 和用户信息
   */
  function setLoginInfo(tokenData) {
    token.value = tokenData.accessToken
    userId.value = tokenData.userId
    username.value = tokenData.username
    // 持久化到 localStorage
    localStorage.setItem('token', tokenData.accessToken)
    localStorage.setItem('userId', tokenData.userId)
    localStorage.setItem('username', tokenData.username)
  }

  /**
   * 退出登录，清除所有状态
   */
  function logout() {
    token.value = ''
    userId.value = ''
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
  }

  return { token, userId, username, isLoggedIn, setLoginInfo, logout }
})
