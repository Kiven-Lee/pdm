import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * Axios 请求封装
 * 统一处理：请求头注入 Token、响应拦截、错误提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动在请求头中添加 Authorization Token
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器：统一处理业务错误和 HTTP 错误
request.interceptors.response.use(
  response => {
    const data = response.data
    // 业务状态码 200 = 成功
    if (data.code === 200) {
      return data
    }
    // 401 = 未登录或 Token 过期，跳转到登录页
    if (data.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
      return Promise.reject(new Error(data.message))
    }
    // 其他业务错误，弹出错误提示
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message))
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
