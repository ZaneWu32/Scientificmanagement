import request from '@/utils/request'
import type { ApiResponse, QueryParams } from './types'

// ── 需求池 ────────────────────────────────────────────────────

/** 获取需求列表（支持关键词、状态、行业、地域等筛选） */
export function getDemands(params?: QueryParams): Promise<ApiResponse<any>> {
  return request({ url: '/demand', method: 'get', params, mock: false })
}

/** 获取单条需求详情 */
export function getDemandDetail(id: string): Promise<ApiResponse<any>> {
  return request({ url: `/demand/${id}`, method: 'get', mock: false })
}

/** 重新计算需求与成果的匹配结果 */
export function rematchDemand(id: string): Promise<ApiResponse<any>> {
  return request({ url: `/demand/${id}/rematch`, method: 'post', mock: false })
}

/** 临时输入需求文本，预览 ES 成果匹配结果，不入库 */
export function previewDemandMatch(data: QueryParams): Promise<ApiResponse<any>> {
  return request({ url: '/demand/match-preview', method: 'post', data, mock: false })
}

// ── 跟进状态 ──────────────────────────────────────────────────

export type DemandStatus = 'new' | 'reviewing' | 'matched' | 'in_follow_up' | 'invalid' | 'archived'

/**
 * 更新需求跟进状态
 * PATCH /demand/:id/status
 */
export function updateDemandStatus(
  id: string,
  status: DemandStatus,
  note?: string
): Promise<ApiResponse<any>> {
  return request({ url: `/demand/${id}/status`, method: 'patch', data: { status, note }, mock: false })
}

// ── 成果匹配确认 ───────────────────────────────────────────────

/**
 * 管理员确认候选成果有效性
 * POST /demand/:id/confirm-match
 */
export function confirmMatch(
  demandId: string,
  resultId: string
): Promise<ApiResponse<any>> {
  return request({
    url: `/demand/${demandId}/confirm-match`,
    method: 'post',
    data: { resultId },
    mock: false
  })
}

// ── 数据源管理 ────────────────────────────────────────────────

/**
 * 获取白名单数据源列表及健康状态
 * GET /demand/sources
 */
export function getDemandSources(): Promise<ApiResponse<any>> {
  return request({ url: '/demand/sources', method: 'get', mock: false })
}
