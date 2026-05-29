import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface PolicyItem {
  id: number
  crawlerId: string
  title: string
  publishDate: string
  sourceUrl: string
  contentPreview: string
  hrefs: string[]
  matchScore?: number
  matchReason?: string
}

export interface CrawlerStatus {
  id: string
  name: string
  syncStatus: string
  crawlerStatus: string
  policyCount: number
}

export function getRelatedPolicies(
  achievementDocId: string,
  limit = 5
): Promise<ApiResponse<PolicyItem[]>> {
  return request({
    url: `/policy/achievement/${achievementDocId}/related`,
    method: 'get',
    params: { limit }
  })
}

export function getCrawlerStatus(): Promise<ApiResponse<CrawlerStatus[]>> {
  return request({
    url: '/policy/crawlers',
    method: 'get'
  })
}

export function triggerCrawlerSync(crawlerId: string): Promise<ApiResponse<string>> {
  return request({
    url: `/policy/crawler/${crawlerId}/sync`,
    method: 'post'
  })
}

export function triggerSyncAll(): Promise<ApiResponse<string>> {
  return request({
    url: '/policy/sync',
    method: 'post'
  })
}

export function triggerMatch(): Promise<ApiResponse<string>> {
  return request({
    url: '/policy/match',
    method: 'post'
  })
}

export interface PolicyQuery {
  page?: number
  pageSize?: number
  keyword?: string
  crawlerId?: string
  startDate?: string
  endDate?: string
}

export function getPolicyList(
  query: PolicyQuery = {}
): Promise<ApiResponse<{ records: PolicyItem[]; total: number; current: number; size: number }>> {
  return request({
    url: '/policy/list',
    method: 'get',
    params: { page: 1, pageSize: 20, ...query }
  })
}
