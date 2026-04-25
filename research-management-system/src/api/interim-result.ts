import request from '@/utils/request'
import type { ApiResponse } from './types'
import type { InterimResult, InterimResultStats } from '@/types'

type InterimResultListPayload = {
  list: InterimResult[]
  total: number
  page: number
  pageSize: number
}

function normalizeAttachment(file: any) {
  if (!file || typeof file !== 'object') {
    return null
  }

  const id = file.id || file.fileId || file.documentId || ''
  const name = file.name || file.fileName || ''
  const url = file.url || file.fileUrl || ''

  if (!id && !name && !url) {
    return null
  }

  return {
    id,
    name: name || '未命名附件',
    url,
    size: typeof file.size === 'number'
      ? file.size
      : typeof file.fileSize === 'number'
        ? file.fileSize
        : undefined,
    ext: file.ext || file.fileType || ''
  }
}

function normalizeAttachments(attachments: any): InterimResult['attachments'] {
  if (Array.isArray(attachments)) {
    return attachments
      .map((file) => normalizeAttachment(file))
      .filter((file): file is NonNullable<ReturnType<typeof normalizeAttachment>> => Boolean(file))
  }

  const nestedFiles = attachments?.data
  if (Array.isArray(nestedFiles)) {
    return nestedFiles.flatMap((record: any) => {
      const rawFiles = record?.files?.data || record?.files || record?.attributes?.files?.data || []
      if (!Array.isArray(rawFiles)) return []
      return rawFiles
        .map((file) => normalizeAttachment(file?.attributes || file))
        .filter((file): file is NonNullable<ReturnType<typeof normalizeAttachment>> => Boolean(file))
    })
  }

  return []
}

function mapInterimResult(item: any): InterimResult {
  const projectId = item.projectId != null && item.projectId !== ''
    ? String(item.projectId)
    : item.projectCode || item.projectName || ''

  return {
    id: String(item.id || item.documentId || ''),
    projectId,
    projectName: item.projectName || '未归属项目',
    projectCode: item.projectCode || '',
    projectPhase: item.projectPhase || '-',
    name: item.name || item.title || '未命名成果',
    type: (item.type || item.typeCode || 'other') as any,
    typeLabel: item.typeLabel || item.typeName || item.type || '其他',
    description: item.description || item.summary || item.abstract || '',
    attachments: normalizeAttachments(item.attachments),
    submitter: item.submitter || item.createdBy || item.creatorName || '',
    submitterDept: item.submitterDept || '',
    submittedAt: item.submittedAt || item.createdAt || '',
    syncedAt: item.syncedAt || item.updatedAt || item.createdAt || '',
    source: item.source || 'process_system',
    sourceRef: String(item.sourceRef || item.id || item.documentId || ''),
    sourceUrl: item.sourceUrl,
    tags: Array.isArray(item.tags) ? item.tags : Array.isArray(item.keywords) ? item.keywords : [],
    status: item.status || ''
  }
}

/**
 * 获取中期成果物统计
 */
export async function getInterimResultStats(): Promise<ApiResponse<InterimResultStats>> {
  return request({
    url: '/interim-results/stats',
    method: 'get'
  })
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
  const res = await request({
    url: '/interim-results',
    method: 'get',
    params: {
      projectId: params?.projectId,
      type: params?.type,
      year: params?.year,
      keyword: params?.keyword,
      page: params?.page ?? 1,
      pageSize: params?.pageSize ?? 10
    }
  })

  const pageData = res?.data || { list: [], total: 0, page: 1, pageSize: 10 }

  return {
    code: Number(res?.code || 200),
    msg: res?.msg || 'success',
    data: {
      list: Array.isArray(pageData.list) ? pageData.list.map(mapInterimResult) : [],
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
  const res = await request({
    url: `/interim-results/${id}`,
    method: 'get'
  })

  return {
    code: Number(res?.code || 200),
    msg: res?.msg || 'success',
    data: mapInterimResult(res?.data || {})
  }
}
