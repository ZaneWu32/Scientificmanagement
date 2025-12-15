import request from '@/utils/request'

export function getDemands(params?: any) {
  return request({
    url: '/demand',
    method: 'get',
    params
  })
}

export function getDemandDetail(id: string) {
  return request({
    url: `/demand/${id}`,
    method: 'get'
  })
}

export function rematchDemand(id: string) {
  return request({
    url: `/demand/${id}/rematch`,
    method: 'post'
  })
}
