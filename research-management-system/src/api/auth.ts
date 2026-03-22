import request from "@/utils/request";
import type { ApiResponse } from "./types";

/**
 * Keycloak token 请求负载（密码授权流程）
 */
export type KeycloakTokenRequest = {
  username: string;
  password: string;
};

/**
 * Keycloak token 响应
 */
export interface KeycloakTokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
  scope: string;
}

/**
 * JWT Claims（用于提取用户信息）
 */
export interface JwtClaims {
  sub: string;
  email_verified: boolean;
  realm_access: {
    roles: string[];
  };
  iss: string;
  name: string;
  preferred_username: string;
  given_name: string;
  family_name: string;
  email: string;
  iat: number;
  exp: number;
}

/**
 * 用户简要信息
 * 来自 Keycloak JWT Token
 */
export interface UserProfile {
  /** 用户ID */
  id: number;
  /** 用户UUID */
  uuid: string;
  /** 用户名 */
  username: string;
  /** 真实姓名 */
  name: string;
  /** 邮箱 */
  email: string;
  /** 角色列表 */
  roles: string[];
}

interface BackendUserProfile {
  id?: number | string;
  uuid?: string;
  username?: string;
  name?: string;
  realName?: string;
  email?: string;
  roles?: string[];
}

const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;

/**
 * 刷新 Keycloak token
 * @param refreshToken 刷新令牌
 */
export async function refreshKeycloakToken(refreshToken: string): Promise<KeycloakTokenResponse> {
  const res = (await request({
    url: "/auth/refresh",
    method: "post",
    skipAuth: true,
    data: {
      refreshToken,
      clientId,
    },
  })) as ApiResponse<KeycloakTokenResponse>;

  return res.data;
}

/**
 * 使用授权码交换 token（Authorization Code + PKCE）
 */
export async function exchangeAuthorizationCode(
  code: string,
  codeVerifier: string,
  redirectUri: string,
): Promise<KeycloakTokenResponse> {
  const res = (await request({
    url: "/auth/exchange-code",
    method: "post",
    skipAuth: true,
    data: {
      code,
      redirectUri,
      codeVerifier,
      clientId,
    },
  })) as ApiResponse<KeycloakTokenResponse>;

  return res.data;
}

/**
 * 使用 one-time ticket 交换 token
 */
export async function exchangeLoginTicket(
  ticket: string,
  state?: string,
): Promise<KeycloakTokenResponse> {
  const res = (await request({
    url: "/auth/exchange-ticket",
    method: "post",
    skipAuth: true,
    data: {
      ticket,
      state,
      clientId,
    },
  })) as ApiResponse<KeycloakTokenResponse>;

  return res.data;
}

/**
 * 从 JWT access_token 中提取用户信息
 * @param accessToken JWT token
 */
export function parseUserFromToken(accessToken: string): UserProfile | null {
  try {
    const payload = accessToken.split(".")[1];
    if (!payload) return null;

    const decoded = JSON.parse(atob(payload)) as JwtClaims;

    // 从 realm_access.roles 中过滤业务角色
    const businessRoles = (decoded.realm_access?.roles || []).filter(
      (role) =>
        !role.startsWith("default-") && role !== "offline_access" && role !== "uma_authorization",
    );

    return {
      id: 0, // 暂时为 0，需要后端补全
      uuid: decoded.sub,
      username: decoded.preferred_username || "",
      name: decoded.name || "",
      email: decoded.email || "",
      roles: businessRoles,
    };
  } catch (e) {
    console.error("Failed to parse JWT token:", e);
    return null;
  }
}

// ============ Backend 认证接口 ============

/**
 * 验证 JWT token 并获取完整用户信息（包含系统 ID）
 * 前端登录后调用此接口，补全用户的系统 ID
 */
export function verifyToken(accessToken: string) {
  return request({
    url: "/auth/verify",
    method: "post",
    skipAuth: true,
    silent: true,
    data: { accessToken },
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  }) as Promise<ApiResponse<UserProfile>>;
}

/**
 * 登出
 */
export function logout() {
  return request({
    url: "/auth/logout",
    method: "post",
  }) as Promise<ApiResponse<true>>;
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: "/auth/current",
    method: "get",
  }) as Promise<ApiResponse<UserProfile>>;
}

export function getCurrentUserByToken(accessToken: string) {
  return request({
    url: "/auth/current",
    method: "get",
    skipAuth: true,
    silent: true,
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  }) as Promise<ApiResponse<BackendUserProfile>>;
}

function normalizeUserProfile(backendUser: BackendUserProfile, fallback: UserProfile): UserProfile {
  const normalizedId = Number(backendUser.id);
  return {
    id: Number.isFinite(normalizedId) ? normalizedId : fallback.id,
    uuid: backendUser.uuid || fallback.uuid,
    username: backendUser.username || fallback.username,
    name: backendUser.name || backendUser.realName || fallback.name,
    email: backendUser.email || fallback.email,
    roles: backendUser.roles && backendUser.roles.length > 0 ? backendUser.roles : fallback.roles,
  };
}

/**
 * 在新认证流程下补全用户信息：
 * 1) /auth/verify（兼容旧设计）
 * 2) /auth/current（直接取当前用户）
 * 3) 回退到 JWT 解析出的基础信息
 */
export async function resolveUserProfile(
  accessToken: string,
  fallback: UserProfile,
): Promise<UserProfile> {
  try {
    const verifyRes = await verifyToken(accessToken);
    if (verifyRes?.data) {
      return normalizeUserProfile(verifyRes.data as BackendUserProfile, fallback);
    }
  } catch (e) {
    console.warn("verifyToken failed, fallback to current user endpoint:", e);
  }

  try {
    const currentRes = await getCurrentUserByToken(accessToken);
    if (currentRes?.data) {
      return normalizeUserProfile(currentRes.data, fallback);
    }
  } catch (e) {
    console.warn("getCurrentUserByToken failed, use JWT parsed user:", e);
  }

  return fallback;
}
