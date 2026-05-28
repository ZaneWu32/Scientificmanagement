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
