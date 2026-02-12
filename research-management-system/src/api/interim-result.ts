import request from '@/utils/request'
import type { ApiResponse } from './types'
import type { InterimResult, InterimResultStats } from '@/types'
import { getAdminResult, getResults } from './result'
import { PROCESS_RESULT_TYPE_CODES } from '@/config/resultTypeScope'

type InterimResultListPayload = {
  list: InterimResult[]
  total: number
  page: number
  pageSize: number
}

function mapListItemToInterim(item: any): InterimResult {
  const projectCode = item.projectCode || ''
  const projectName = item.projectName || '未归属项目'
  return {
    id: item.id || item.documentId,
    projectId: projectCode || projectName,
    projectName,
    projectCode,
    projectPhase: '-',
    name: item.title || '未命名成果',
    type: (item.typeCode || item.type || 'other') as any,
    typeLabel: item.type || item.typeName || '其他',
    description: item.summary || '',
    attachments: [],
    submitter: item.createdBy || '',
    submitterDept: '',
    submittedAt: item.createdAt || '',
    syncedAt: item.updatedAt || item.createdAt || '',
    source: 'process_system',
    sourceRef: item.id || item.documentId || '',
    sourceUrl: item.sourceUrl,
    tags: Array.isArray(item.keywords) ? item.keywords : [],
    status: item.status || ''
  }
}

function mapDetailToInterim(item: any): InterimResult {
  const projectCode = item.projectCode || ''
  const projectName = item.projectName || '未归属项目'
  const attachments = Array.isArray(item.attachments)
    ? item.attachments.map((file: any) => ({
        id: file.id,
        name: file.name,
        url: file.url,
        size: file.size,
        ext: file.ext
      }))
    : []

  return {
    id: item.id || item.documentId,
    projectId: projectCode || projectName,
    projectName,
    projectCode,
    projectPhase: '-',
    name: item.title || '未命名成果',
    type: (item.typeCode || item.type || 'other') as any,
    typeLabel: item.typeName || item.type || '其他',
    description: item.abstract || item.summary || '',
    attachments,
    submitter: item.createdBy || '',
    submitterDept: '',
    submittedAt: item.createdAt || '',
    syncedAt: item.updatedAt || item.createdAt || '',
    source: 'process_system',
    sourceRef: item.id || item.documentId || '',
    sourceUrl: item.sourceUrl,
    tags: Array.isArray(item.keywords) ? item.keywords : [],
    status: item.status || ''
  }
}

/**
 * 获取中期成果物统计
 */
export async function getInterimResultStats(): Promise<ApiResponse<InterimResultStats>> {
  const pageSize = 200
  let page = 1
  let all: any[] = []
  let total = 0

  do {
    const res = await getResults(
      {
        page,
        pageSize,
        typeCodes: [...PROCESS_RESULT_TYPE_CODES]
      },
      true
    )
    const pageData = res?.data || { list: [], total: 0 }
    const list = Array.isArray(pageData.list) ? pageData.list : []
    total = Number(pageData.total || 0)
    all = all.concat(list)
    if (!list.length) break
    page += 1
  } while (all.length < total)

  const byType: Record<string, number> = {}
  const byYear: Record<string, number> = {}
  const projects = new Set<string>()
  let recentSyncTime = ''

  all.forEach((item) => {
    const type = item.typeCode || item.type || 'other'
    byType[type] = (byType[type] || 0) + 1

    const time = item.createdAt || item.updatedAt || ''
    const year = typeof time === 'string' ? time.substring(0, 4) : ''
    if (year) byYear[year] = (byYear[year] || 0) + 1

    const projectKey = item.projectCode || item.projectName
    if (projectKey) projects.add(projectKey)

    const currentTs = item.updatedAt || item.createdAt
    if (currentTs && (!recentSyncTime || currentTs > recentSyncTime)) {
      recentSyncTime = currentTs
    }
  })

  return {
    code: 200,
    msg: 'success',
    data: {
      totalProjects: projects.size,
      totalResults: total || all.length,
      byType,
      byYear,
      recentSyncTime
    }
  }
}

/**
 * 获取中期成果物列表
 */
export async function getInterimResults(params?: {
  projectId?: string
  type?: string
  year?: string
  keyword?: string
  page?: number
  pageSize?: number
}): Promise<ApiResponse<InterimResultListPayload>> {
  const res = await getResults(
    {
      page: params?.page ?? 1,
      pageSize: params?.pageSize ?? 10,
      keyword: params?.keyword,
      type: params?.type,
      projectId: params?.projectId,
      projectCode: params?.projectId,
      yearRange: params?.year ? [params.year, params.year] : undefined,
      typeCodes: [...PROCESS_RESULT_TYPE_CODES]
    },
    true
  )

  const pageData = res?.data || { list: [], total: 0, page: 1, pageSize: 10 }
  const list = Array.isArray(pageData.list) ? pageData.list.map(mapListItemToInterim) : []

  return {
    code: 200,
    msg: 'success',
    data: {
      list,
      total: Number(pageData.total || 0),
      page: Number(pageData.page || params?.page || 1),
      pageSize: Number(pageData.pageSize || params?.pageSize || 10)
    }
  }
}

/**
 * 获取中期成果物详情
 */
export async function getInterimResultDetail(id: string): Promise<ApiResponse<InterimResult>> {
  const res = await getAdminResult(id)
  return {
    code: 200,
    msg: 'success',
    data: mapDetailToInterim(res?.data || {})
  }
}

/**
 * 手动同步中期成果物
 */
export function syncInterimResults(projectId?: string): Promise<ApiResponse<{
  syncCount: number
  syncTime: string
}>> {
  return request({
    url: '/interim-results/sync',
    method: 'post',
    data: { projectId }
  })
}

/**
 * 下载附件
 */
export function getAttachmentDownloadUrl(attachmentId: string): string {
  return `${import.meta.env.VITE_API_BASE_URL}/interim-results/attachments/${attachmentId}/download`
}

/**
 * 批量下载（打包）
 */
export function batchDownload(resultIds: string[]): Promise<Blob> {
  return request({
    url: '/interim-results/batch-download',
    method: 'post',
    data: { resultIds },
    responseType: 'blob'
  })
}

/**
 * 导出列表
 */
export function exportInterimResults(params?: {
  projectId?: string
  type?: string
  year?: string
  keyword?: string
}): Promise<Blob> {
  return request({
    url: '/interim-results/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
