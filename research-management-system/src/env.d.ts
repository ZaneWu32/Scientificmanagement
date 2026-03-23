/// <reference types="vite/client" />

interface ViteTypeOptions {
  strictImportMetaEnv: unknown;
}

/**
 * Vite 环境变量类型定义
 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_STRAPI_BASE_URL: string;
  readonly VITE_USE_MOCK: string;

  // 仅开发环境存在
  readonly VITE_LOGIN_PORTAL_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
