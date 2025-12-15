import request from '@/utils/request'

// 获取项目列表
export function getProjects(params?: any) {
  return request({
    url: '/projects',
    method: 'get',
    params
  })
}

// 获取项目详情
export function getProject(id: string) {
  return request({
    url: `/projects/${id}`,
    method: 'get'
  })
}

// 创建项目
export function createProject(data: { name: string; code: string }) {
  return request({
    url: '/projects',
    method: 'post',
    data
  })
}
