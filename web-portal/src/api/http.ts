import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('rv_portal_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const url = String(error.config?.url || '')
    const isAuthRequest = url.includes('/auth/login') || url.includes('/auth/register')
    if (status === 401) {
      if (isAuthRequest) {
        ElMessage.error(error.response?.data?.message || '用户名或密码错误')
      } else {
        localStorage.removeItem('rv_portal_token')
        localStorage.removeItem('rv_portal_refresh')
        ElMessage.warning('请先登录')
        if (location.pathname !== '/login') {
          location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
        }
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default http
