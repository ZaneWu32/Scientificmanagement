import request from '@/utils/request'
import type { ApiResponse, QueryParams } from './types'
import type { DemandItem, DemandStats } from '@/types'

// 获取需求列表
export function getDemands(params?: QueryParams): Promise<ApiResponse<{ list: DemandItem[]; total: number; page: number; pageSize: number }>> {
  return request({
    url: '/demand',
    method: 'get',
    params
  })
}

export function getDemandStats(): Promise<ApiResponse<DemandStats>> {
  return request({
    url: '/demand/stats',
    method: 'get'
  })
}

// 获取需求详情
export function getDemandDetail(id: string): Promise<ApiResponse<DemandItem>> {
  return request({
    url: `/demand/${id}`,
    method: 'get'
  })
}

// 重新匹配需求
export function rematchDemand(id: string): Promise<ApiResponse<any>> {
  return request({
    url: `/demand/${id}/rematch`,
    method: 'post'
  })
}
