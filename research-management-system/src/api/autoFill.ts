import request from '@/utils/request'

export interface AutoFillField {
  key: string
  label: string
  value: string
  confidence: number // 0~1
  sourceSnippet: string // 原文依据片段
  needsConfirm: boolean // 是否为待确认项
}

export interface AutoFillResult {
  fileId: string
  fileName: string
  recognizedAt: string
  fields: AutoFillField[]
  pendingConfirmations: string[] // 文字性待确认提示
}

/**
 * 上传附件并触发 AI 识别（multipart）
 * POST /auto-fill/from-attachment
 */
export function autoFillFromAttachment(
  file: File,
  resultTypeCode: string
): Promise<{ data: AutoFillResult }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('resultTypeCode', resultTypeCode)
  return request({
    url: '/auto-fill/from-attachment',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60 * 1000,
  })
}

/**
 * 确认字段回填（可选：记录用户选择）
 * POST /auto-fill/confirm
 */
export function confirmAutoFillFields(params: {
  fileId: string
  selectedKeys: string[]
}): Promise<{ data: void }> {
  return request({
    url: '/auto-fill/confirm',
    method: 'post',
    data: params
  })
}
