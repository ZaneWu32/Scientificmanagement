import request from '@/utils/request'
import type { ApiResponse } from './types'

export interface ReportTaskStatus {
  taskId: string
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED'
  progress: number
  errorMsg?: string
}

export function generateReport(achievementDocIds: string[]): Promise<ApiResponse<{ taskId: string }>> {
  return request({
    url: '/admin/report/generate',
    method: 'post',
    data: { achievementDocIds }
  })
}

export function getReportTask(taskId: string): Promise<ApiResponse<ReportTaskStatus>> {
  return request({
    url: `/admin/report/task/${taskId}`,
    method: 'get'
  })
}

export function getReportContent(taskId: string): Promise<ApiResponse<{ html: string }>> {
  return request({
    url: `/admin/report/task/${taskId}/content`,
    method: 'get'
  })
}

export function exportReport(taskId: string, format: 'pdf' | 'word', html?: string): Promise<Blob> {
  return request({
    url: `/admin/report/task/${taskId}/export`,
    method: 'post',
    data: { format, html },
    responseType: 'blob'
  })
}
