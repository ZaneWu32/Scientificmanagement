import axios from 'axios'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { handleMockRequest } from '@/mocks/handler'
import { ErrorHandler, AppError, ErrorType } from './errorHandler'

const isMockEnabled = import.meta.env.VITE_USE_MOCK === 'true'

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const { token } = storeToRefs(userStore)
    if (token.value) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token.value}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => handleBusinessResponse(response.data),
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      switch (error.response.status) {
        case 401: {
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        }
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误，请稍后重试')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

function handleBusinessResponse(res) {
  if (res?.code !== 200) {
    const error = ErrorHandler.handleBusinessError(
      res?.code,
      res?.message || '请求失败',
      res
    )

    if (res?.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ErrorHandler.handlePermissionError(error)
    } else {
      ErrorHandler.showError(error)
    }

    return Promise.reject(error)
  }

  return res
}

function withAuth(config: any) {
  const cfg = { ...config, headers: { ...(config.headers || {}) } }
  const userStore = useUserStore()
  const { token } = storeToRefs(userStore)
  if (token.value) {
    cfg.headers.Authorization = `Bearer ${token.value}`
  }
  return cfg
}

export default async function request(config: any) {
  const finalConfig = withAuth(config)
  if (isMockEnabled) {
    const mockRes = handleMockRequest(finalConfig)
    if (mockRes) return handleBusinessResponse(mockRes)
  }
  return service(finalConfig)
}
