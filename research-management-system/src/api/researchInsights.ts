import request from '@/utils/request'

// ── 类型定义 ──────────────────────────────────────────────────

export interface ResearchTopic {
  id: string
  name: string
  domain: string
  field: string
  stage: 'emerging' | 'strong' | 'cross' | 'gap'
  keywords: string[]
  description: string
  externalHeat: number
  internalStrength: number
  growthRate12m: number
  departments: string[]
  opportunityType: string
  recommendation: string
  explanation: string
  trend: number[] // 12 个月趋势值
  internalResults: string[]
  externalSignals: string[]
}

export interface ExecutiveInsight {
  title: string
  level: '重点关注' | '持续投入' | '组织协同' | '补齐短板'
  description: string
  evidence: string
  action: string
}

export interface TopicFilter {
  stage?: string
}

// ── 真实接口（接口联调后替换 mock） ──────────────────────────

/**
 * 获取研究主题池
 * GET /research-insights/topics
 */
export function getResearchTopics(params?: TopicFilter): Promise<{ data: ResearchTopic[] }> {
  return request({
    url: '/research-insights/topics',
    method: 'get',
    params
  })
}

/**
 * 获取执行摘要（管理层辅助阅读）
 * GET /research-insights/executive-insights
 */
export function getExecutiveInsights(): Promise<{ data: ExecutiveInsight[] }> {
  return request({
    url: '/research-insights/executive-insights',
    method: 'get'
  })
}

/**
 * 获取单个主题近 12 个月趋势
 * GET /research-insights/topics/:id/trend
 */
export function getTopicTrend(topicId: string): Promise<{ data: number[] }> {
  return request({
    url: `/research-insights/topics/${topicId}/trend`,
    method: 'get'
  })
}
