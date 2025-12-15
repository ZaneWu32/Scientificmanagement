import request from '@/utils/request'

// 获取统计数据
export function getStatistics() {
  return request({
    url: '/results/statistics',
    method: 'get'
  })
}

// 获取个人统计
export function getMyStatistics() {
  return request({
    url: '/results/my-statistics',
    method: 'get'
  })
}

// 获取成果类型列表
export function getResultTypes() {
  return request({
    url: '/result-types',
    method: 'get'
  })
}

// 获取成果类型详情
export function getResultType(id) {
  return request({
    url: `/result-types/${id}`,
    method: 'get'
  })
}

// 获取成果列表
export function getResults(params) {
  return request({
    url: '/results',
    method: 'get',
    params
  })
}

// 获取我的成果列表
export function getMyResults(params) {
  return request({
    url: '/results/my',
    method: 'get',
    params
  })
}

// 获取成果详情
export function getResult(id) {
  return request({
    url: `/results/${id}`,
    method: 'get'
  })
}

// 申请查看成果全文
export function requestResultAccess(id, data) {
  return request({
    url: `/results/${id}/access-requests`,
    method: 'post',
    data
  })
}

// 创建成果
export function createResult(data) {
  return request({
    url: '/results',
    method: 'post',
    data
  })
}

// 更新成果
export function updateResult(id, data) {
  return request({
    url: `/results/${id}`,
    method: 'put',
    data
  })
}

// 删除成果
export function deleteResult(id) {
  return request({
    url: `/results/${id}`,
    method: 'delete'
  })
}

// 保存草稿
export function saveDraft(data) {
  return request({
    url: '/results/draft',
    method: 'post',
    data
  })
}

// 提交审核
export function submitReview(id) {
  return request({
    url: `/results/${id}/submit`,
    method: 'post'
  })
}

// 审核成果
export function reviewResult(id, data) {
  return request({
    url: `/results/${id}/review`,
    method: 'post',
    data
  })
}

// 智能补全（通过 DOI 等）
export function autoFillMetadata(params) {
  return request({
    url: '/results/auto-fill',
    method: 'get',
    params
  })
}

// 上传附件
export function uploadAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
