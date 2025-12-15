import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserRole } from '@/types'
import { errorLogger, AppError, ErrorType } from '@/utils/errorHandler'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<Record<string, any> | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role)

  // 登录
  function login(newToken: string, user: Record<string, any>) {
    // 先更新内存中的数据 (同步操作,立即生效)
    token.value = newToken
    userInfo.value = user
    
    // 再保存到 localStorage
    const success = safeSetItem('token', newToken) && 
                    safeSetItem('userInfo', JSON.stringify(user))
    
    if (!success) {
      // 如果存储失败,清空内存中的数据
      token.value = ''
      userInfo.value = null
      throw new AppError(
        '登录信息保存失败',
        ErrorType.RUNTIME,
        'LOGIN_SAVE_ERROR'
      )
    }
    
    // 添加日志,帮助调试
    console.log('✅ 登录成功:', {
      username: user?.name,
      role: user?.role,
      token: newToken?.substring(0, 20) + '...'
    })
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = null
    safeRemoveItem('token')
    safeRemoveItem('userInfo')
  }

  // 初始化用户信息
  function initUserInfo() {
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      try {
        userInfo.value = JSON.parse(storedUserInfo)
      } catch (e) {
        const error = new AppError(
          '用户信息已损坏，请重新登录',
          ErrorType.VALIDATION,
          'USER_INFO_PARSE_ERROR',
          e
        )
        errorLogger.log(error)
        ElMessage.warning('用户信息已过期，请重新登录')
        logout()
      }
    }
  }

  // 安全的 localStorage 操作
  function safeSetItem(key: string, value: string) {
    try {
      localStorage.setItem(key, value)
      return true
    } catch (e) {
      const error = new AppError(
        '存储空间不足或浏览器限制',
        ErrorType.RUNTIME,
        'STORAGE_ERROR',
        e
      )
      errorLogger.log(error)
      ElMessage.error('保存用户信息失败，请检查浏览器设置')
      return false
    }
  }

  function safeRemoveItem(key: string) {
    try {
      localStorage.removeItem(key)
    } catch (e) {
      console.error('删除存储失败:', e)
    }
  }

  // 检查权限
  function hasRole(role: string | string[]) {
    if (!userInfo.value) return false
    const roles = Array.isArray(role) ? role : [role]
    return roles.includes(userInfo.value.role)
  }

  // 检查是否为管理员
  const isAdmin = computed(() => hasRole(UserRole.ADMIN))
  const isManager = computed(() => hasRole([UserRole.ADMIN, UserRole.MANAGER]))
  const isExpert = computed(() => hasRole([UserRole.ADMIN, UserRole.EXPERT]))

  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    isAdmin,
    isManager,
    isExpert,
    login,
    logout,
    initUserInfo,
    hasRole
  }
})
