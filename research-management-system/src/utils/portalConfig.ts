/**
 * Portal 配置服务
 * 从环境变量或远程获取门户配置
 */

export interface PortalConfig {
  portalUrl: string;
}

const DEFAULT_PORTAL_CONFIG: Readonly<PortalConfig> = {
  portalUrl: "http://localhost:3000",
};

/**
 * 获取门户配置
 * - 开发环境：从环境变量读取
 * - 生产环境：从 /config/portal-config.json 获取
 */
export async function getPortalConfig(): Promise<Readonly<PortalConfig>> {
  // 开发环境从 import.meta.env 读取
  if (import.meta.env.DEV) {
    return {
      portalUrl: import.meta.env.VITE_LOGIN_PORTAL_URL || DEFAULT_PORTAL_CONFIG.portalUrl,
    };
  }

  // 生产环境从 /config/portal-config.json 获取
  try {
    const response = await fetch("/config/portal-config.json");
    if (!response.ok) {
      throw new Error(`Failed to fetch portal config: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error("Failed to load portal config:", error);
    // 返回默认配置
    return DEFAULT_PORTAL_CONFIG;
  }
}

/**
 * 跳转回 login-portal
 */
export async function redirectToLoginPortal(redirectUri?: string) {
  const { portalUrl } = await getPortalConfig();
  const target = redirectUri ? `${portalUrl}?redirect=${encodeURIComponent(redirectUri)}` : portalUrl;
  window.location.href = target;
}
