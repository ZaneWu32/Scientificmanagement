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

const PORTAL_CONFIG_CACHE_TTL_MS = 30_000;
let cachedPortalConfig: Readonly<PortalConfig> | null = null;
let cacheExpiresAt = 0;
let pendingPortalConfigRequest: Promise<Readonly<PortalConfig>> | null = null;

function buildCachedConfig(config: Partial<PortalConfig>): Readonly<PortalConfig> {
  const normalized = Object.freeze({
    ...DEFAULT_PORTAL_CONFIG,
    ...config,
  });
  cachedPortalConfig = normalized;
  cacheExpiresAt = Date.now() + PORTAL_CONFIG_CACHE_TTL_MS;
  return normalized;
}

function getValidCachedConfig(): Readonly<PortalConfig> | null {
  if (cachedPortalConfig && Date.now() < cacheExpiresAt) {
    return cachedPortalConfig;
  }
  return null;
}

/**
 * 获取门户配置
 * - 开发环境：从环境变量读取
 * - 生产环境：从 /config/portal-config.json 获取
 */
export async function getPortalConfig(): Promise<Readonly<PortalConfig>> {
  const cached = getValidCachedConfig();
  if (cached) {
    return cached;
  }

  // 开发环境从 import.meta.env 读取
  if (import.meta.env.DEV) {
    return buildCachedConfig({
      portalUrl: import.meta.env.VITE_LOGIN_PORTAL_URL || DEFAULT_PORTAL_CONFIG.portalUrl,
    });
  }

  if (pendingPortalConfigRequest) {
    return pendingPortalConfigRequest;
  }

  // 生产环境从 /config/portal-config.json 获取
  pendingPortalConfigRequest = (async () => {
    try {
      const response = await fetch("/config/portal-config.json");
      if (!response.ok) {
        throw new Error(`Failed to fetch portal config: ${response.status}`);
      }
      const serverConfig: Partial<PortalConfig> = await response.json();
      return buildCachedConfig(serverConfig);
    } catch (error) {
      console.error("Failed to load portal config:", error);
      // 返回默认配置
      return buildCachedConfig(DEFAULT_PORTAL_CONFIG);
    } finally {
      pendingPortalConfigRequest = null;
    }
  })();

  return pendingPortalConfigRequest;
}

/**
 * 跳转回 login-portal
 */
export async function redirectToLoginPortal(redirectUri?: string) {
  const { portalUrl } = await getPortalConfig();
  const target = redirectUri
    ? `${portalUrl}?redirect=${encodeURIComponent(redirectUri)}`
    : portalUrl;
  window.location.href = target;
}
