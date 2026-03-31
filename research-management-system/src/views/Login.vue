<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-badge">科研成果管理系统</div>
      <h1 class="hero-title">欢迎回来，科研人</h1>
      <p class="hero-sub">统一成果 · 高效协同 · 智能洞察</p>
      <div class="hero-stats">
        <div class="stat glass">
          <div class="stat-label">系统状态</div>
          <div class="stat-value">✓</div>
          <div class="stat-desc">正在处理登录...</div>
        </div>
        <div class="stat glass">
          <div class="stat-label">进度</div>
          <div class="stat-value">3/3</div>
          <div class="stat-desc">{{ message }}</div>
        </div>
      </div>
    </div>

    <div class="login-card glass">
      <div class="card-header">
        <div>
          <div class="card-title">科研成果管理系统</div>
          <div class="card-sub">登录以查看你的进展和任务</div>
        </div>
      </div>

      <div class="loading-container">
        <div class="spinner-wrapper">
          <el-icon class="spinning">
            <Loading />
          </el-icon>
        </div>
        <p class="loading-text">正在处理登录...</p>
        <p class="loading-step">{{ message }}</p>
        <div class="progress-bar">
          <div class="progress-fill"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { exchangeLoginTicket, parseUserFromToken, resolveUserProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { redirectToLoginPortal } from '@/utils/portalConfig'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = ref('正在验证身份...')

onMounted(async () => {
  try {
    const params = new URLSearchParams(window.location.search)
    const ticket = params.get('ticket')
    const state = params.get('state')

    if (!ticket) {
      const redirect = route.query.redirect as string
      const target = redirect && isValidRedirect(redirect) ? redirect : '/dashboard'
      const redirectUri = encodeURIComponent(`${window.location.origin}${window.location.pathname}?redirect=${encodeURIComponent(target)}`)
      message.value = '正在跳转到统一登录门户...'
      await redirectToLoginPortal(redirectUri)
      return
    }

    const tokenRes = await exchangeLoginTicket(ticket, state || undefined)
    const accessToken = tokenRes.access_token
    const refreshToken = tokenRes.refresh_token

    message.value = '正在解析用户信息...'

    // 解析 JWT 获取基本用户信息
    const partialUser = parseUserFromToken(accessToken)
    if (!partialUser) {
      throw new Error('Token 解析失败，请重试')
    }

    message.value = '正在完整化用户数据...'

    if (!refreshToken) {
      throw new Error('未找到 refresh token，请重新登录')
    }

    // 新认证流程下采用“可降级补全”：verify -> current -> JWT fallback
    const fullUser = await resolveUserProfile(accessToken, partialUser)

    // 保存 token 和用户信息到 store
    const success = userStore.login(accessToken, refreshToken, fullUser, false)
    if (!success) {
      throw new Error('保存登录信息失败')
    }

    message.value = '登录成功，跳转中...'

    // 等待 store 更新完成
    await new Promise((resolve) => setTimeout(resolve, 500))

    // 获取重定向目标
    const redirectFromQuery = route.query.redirect as string
    const redirect = (redirectFromQuery && isValidRedirect(redirectFromQuery)) ? redirectFromQuery : getRedirectTarget()

    const cleanUrl = window.location.origin + window.location.pathname
    window.history.replaceState({}, document.title, cleanUrl)

    router.replace(redirect)
  } catch (error) {
    console.error('登录回调处理失败:', error)
    message.value = `错误：${error instanceof Error ? error.message : '未知错误'}`
    ElMessage.error(`登录失败：${error instanceof Error ? error.message : '未知错误'}`)

    // 3秒后回门户重登
    setTimeout(() => {
      const redirect = route.query.redirect as string
      const target = redirect && isValidRedirect(redirect) ? redirect : '/dashboard'
      const redirectUri = encodeURIComponent(`${window.location.origin}${window.location.pathname}?redirect=${encodeURIComponent(target)}`)
      redirectToLoginPortal(redirectUri)
    }, 3000)
  }
})

/**
 * 获取重定向目标
 */
function getRedirectTarget(): string {
  // 优先使用 redirect 参数
  const redirect = route.query.redirect as string
  if (redirect && isValidRedirect(redirect)) {
    return redirect
  }

  // 根据角色获取默认路由
  if (!userStore.userInfo) return '/dashboard'

  if (userStore.isAdmin) {
    return '/admin/dashboard'
  } else if (userStore.isExpert) {
    return '/expert/reviews'
  }

  return '/dashboard'
}

/**
 * 验证重定向 URL 是否有效（防止开放重定向）
 */
function isValidRedirect(url: string): boolean {
  if (!url || url.startsWith('http')) return false
  try {
    const resolved = router.resolve(url)
    return resolved.matched.length > 0
  } catch {
    return false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 440px;
  gap: 32px;
  align-items: center;
  padding: 60px;
  background: radial-gradient(circle at 20% 20%, rgba(29, 91, 255, 0.12), transparent 30%),
    radial-gradient(circle at 80% 10%, rgba(15, 69, 216, 0.12), transparent 28%),
    #f7f9fc;
}

.login-hero {
  padding: 32px;
  color: #0f172a;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  background: #e5edff;
  color: #1d5bff;
  border-radius: 12px;
  font-weight: 700;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 34px;
  font-weight: 800;
  margin: 0 0 8px;
}

.hero-sub {
  color: #6b7280;
  margin-bottom: 18px;
  font-size: 16px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.stat {
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #eef2f7;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  color: #1d5bff;
}

.stat-desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.login-card {
  background: #fff;
  padding: 28px;
  border-radius: 18px;
  box-shadow: 0 16px 40px rgba(17, 24, 39, 0.12);
  border: 1px solid #eef2f7;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
  gap: 12px;
}

.card-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.card-sub {
  color: #6b7280;
  margin-top: 4px;
  font-size: 13px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 24px 0;
}

.spinner-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e5edff 0%, #f0f4ff 100%);
}

.spinning {
  font-size: 32px;
  color: #1d5bff;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.loading-step {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  min-height: 20px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #eef2f7;
  border-radius: 2px;
  overflow: hidden;
  margin-top: 12px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1d5bff 0%, #667eea 100%);
  border-radius: 2px;
  animation: progress-animate 2s ease-in-out infinite;
}

@keyframes progress-animate {
  0% {
    width: 0%;
  }

  50% {
    width: 80%;
  }

  100% {
    width: 100%;
  }
}

.glass {
  backdrop-filter: blur(10px);
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 32px 20px;
  }

  .login-hero {
    order: 2;
  }

  .login-card {
    order: 1;
  }

  .hero-title {
    font-size: 24px;
  }

  .login-card {
    padding: 24px;
  }
}
</style>
