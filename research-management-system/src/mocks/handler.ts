import { users, results, resultTypes, projects, uploads, nextId, demands } from './data'

const now = () => new Date().toISOString().slice(0, 10)

export function handleMockRequest(config = {}) {
  const method = (config.method || 'get').toLowerCase()
  const url = (config.url || '').split('?')[0]
  const params = config.params || {}
  const body = parseBody(config.data)
  const token = extractToken(config)
  const currentUser = getUserByToken(token)

  // 认证相关
  if (method === 'post' && url === '/auth/login') {
    const user = users.find(
      (u) => u.username === body.username && u.password === body.password
    )
    if (!user) return fail(401, '账号或密码错误')
    return success({
      token: user.token,
      user: stripUser(user)
    })
  }

  if (method === 'post' && url === '/auth/logout') {
    return success(true, '已退出')
  }

  if (method === 'get' && url === '/auth/current') {
    if (!currentUser) return fail(401, '未登录')
    return success(stripUser(currentUser))
  }

  // 项目
  if (method === 'get' && url === '/projects') {
    const keyword = params.keyword || params.keywords || ''
    let list = [...projects]
    if (keyword) {
      list = list.filter(
        (item) =>
          item.name.includes(keyword) ||
          item.code.includes(keyword) ||
          item.description?.includes(keyword)
      )
    }
    return success(list)
  }

  if (method === 'get' && /^\/projects\/[^/]+$/.test(url)) {
    const id = url.split('/')[2]
    const project = projects.find((item) => item.id === id || item.code === id)
    if (!project) return fail(404, '未找到项目')
    return success(project)
  }

  if (method === 'post' && url === '/projects') {
    if (!currentUser) return fail(401, '未登录')
    const newProject = {
      ...body,
      id: nextId('p')
    }
    projects.unshift(newProject)
    return success(newProject, '项目创建成功')
  }

  // 统计
  if (method === 'get' && url === '/results/statistics') {
    return success(buildStatistics(results))
  }

  if (method === 'get' && url === '/results/my-statistics') {
    if (!currentUser) return fail(401, '未登录')
    const mine = results.filter((item) => item.createdBy === currentUser.name)
    return success(buildStatistics(mine))
  }

  // 成果类型
  if (method === 'get' && url === '/result-types') {
    return success(resultTypes)
  }

  if (method === 'get' && url.startsWith('/result-types/')) {
    const id = url.split('/')[2]
    const type = resultTypes.find((item) => item.id === id || item.code === id)
    return success(type || null)
  }

  // 成果列表
  if (method === 'get' && url === '/results') {
    return success(pageResults(results, params))
  }

  if (method === 'get' && url === '/results/my') {
    if (!currentUser) return fail(401, '未登录')
    const mine = results.filter((item) => item.createdBy === currentUser.name)
    return success(pageResults(mine, params))
  }

  // 成果详情（放在列表后以避免与统计接口冲突）
  if (method === 'get' && /^\/results\/[^/]+$/.test(url)) {
    const id = url.split('/')[2]
    const detail = results.find((item) => item.id === id)
    if (!detail) return fail(404, '未找到成果')
    return success(detail)
  }

  // 创建/草稿
  if (method === 'post' && url === '/results') {
    if (!currentUser) return fail(401, '未登录')
    const typeId = body.typeId || body.type
    const projectFields = mergeProjectInfo(body)
    const newResult = {
      ...body,
      type: typeId,
      typeId,
      id: nextId(),
      status: body.status || 'pending',
      createdBy: currentUser.name,
      createdAt: now(),
      updatedAt: now(),
      reviewHistory: [],
      ...projectFields
    }
    results.unshift(newResult)
    return success(newResult, '创建成功')
  }

  if (method === 'post' && url === '/results/draft') {
    if (!currentUser) return fail(401, '未登录')
    const typeId = body.typeId || body.type
    const projectFields = mergeProjectInfo(body)
    const draft = {
      ...body,
      type: typeId,
      typeId,
      id: nextId(),
      status: 'draft',
      createdBy: currentUser.name,
      createdAt: now(),
      updatedAt: now(),
      reviewHistory: [],
      ...projectFields
    }
    results.unshift(draft)
    return success(draft, '草稿已保存')
  }

  // 更新
  if (method === 'put' && /^\/results\/[^/]+$/.test(url)) {
    const id = url.split('/')[2]
    const index = results.findIndex((item) => item.id === id)
    if (index === -1) return fail(404, '未找到成果')
    const typeId = body.typeId || body.type || results[index].typeId
    const projectFields = mergeProjectInfo(body, results[index])
    results[index] = { ...results[index], ...body, ...projectFields, typeId, type: typeId, updatedAt: now() }
    return success(results[index], '更新成功')
  }

  // 删除
  if (method === 'delete' && /^\/results\/[^/]+$/.test(url)) {
    const id = url.split('/')[2]
    const index = results.findIndex((item) => item.id === id)
    if (index === -1) return fail(404, '未找到成果')
    results.splice(index, 1)
    return success(true, '删除成功')
  }

  // 提交审核
  if (method === 'post' && /^\/results\/[^/]+\/submit$/.test(url)) {
    const id = url.split('/')[2]
    const item = results.find((r) => r.id === id)
    if (!item) return fail(404, '未找到成果')
    item.status = 'reviewing'
    item.updatedAt = now()
    return success(true, '已提交审核')
  }

  // 审核
  if (method === 'post' && /^\/results\/[^/]+\/review$/.test(url)) {
    const id = url.split('/')[2]
    const item = results.find((r) => r.id === id)
    if (!item) return fail(404, '未找到成果')
    const reviewerName = currentUser?.name || '审核专家'
    const action = body.action === 'reject' ? 'reject' : 'approve'
    item.status = action === 'approve' ? 'published' : 'rejected'
    item.updatedAt = now()
    item.reviewHistory = item.reviewHistory || []
    item.reviewHistory.push({
      id: nextId('rev'),
      reviewerId: currentUser?.id || '2',
      reviewerName,
      action,
      comment: body.comment || '',
      createdAt: now()
    })
    return success(true, '审核完成')
  }

  // 智能补全
  if (method === 'get' && url === '/results/auto-fill') {
    const value = params.value || body.value
    const type = params.type || body.type
    const sample = {
      title: `智能推荐的成果标题${type ? `（${type}）` : ''}`,
      authors: value ? [value, '合作者A'] : ['作者A', '作者B'],
      abstract: '这是根据标识符自动补全的摘要示例，可根据需要修改。',
      keywords: ['自动补全', '示例'],
      year: now().slice(0, 4)
    }
    return success(sample, '已补全')
  }

  // 企业需求列表
  if (method === 'get' && url === '/demand') {
    return success(pageDemands(demands, params))
  }

  // 企业需求详情
  if (method === 'get' && /^\/demand\/[^/]+$/.test(url)) {
    const id = url.split('/')[2]
    const item = demands.find((d) => d.id === id)
    if (!item) return fail(404, '未找到需求')
    return success(item)
  }

  // 重新匹配
  if (method === 'post' && /^\/demand\/[^/]+\/rematch$/.test(url)) {
    const id = url.split('/')[2]
    const item = demands.find((d) => d.id === id)
    if (!item) return fail(404, '未找到需求')
    item.matches = (item.matches || []).map((m) => {
      const noise = (Math.random() - 0.5) * 0.1 // -0.05 ~ 0.05
      const score = Math.min(0.98, Math.max(0.1, (m.matchScore || 0) + noise))
      return {
        ...m,
        matchScore: Number(score.toFixed(2)),
        updatedAt: now()
      }
    }).sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0))
    item.bestMatchScore = item.matches[0]?.matchScore || 0
    item.status = item.matches.length ? 'matched' : 'unmatched'
    return success(item, '已重新匹配')
  }

  // 上传
  if (method === 'post' && url === '/upload') {
    const file = body?.get ? body.get('file') : null
    const uploadResult = {
      url: '/mock/upload/' + nextId('file'),
      name: file?.name || 'mock-file',
      size: file?.size || 1024 * 100
    }
    uploads.push(uploadResult)
    return success(uploadResult, '上传成功')
  }

  return null
}

function parseBody(raw) {
  if (!raw) return {}
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch (e) {
      return {}
    }
  }
  return raw
}

function extractToken(config) {
  const auth = config.headers?.Authorization || ''
  if (auth.startsWith('Bearer ')) return auth.replace('Bearer ', '')
  if (config.params?.token) return config.params.token
  return ''
}

function getUserByToken(token) {
  if (!token) return null
  return users.find((u) => u.token === token) || null
}

function stripUser(user) {
  const { password, ...rest } = user || {}
  return rest
}

function success(data, message = 'success') {
  return { code: 200, message, data }
}

function fail(code = 400, message = '请求失败') {
  return { code, message, data: null }
}

function pageResults(source, params) {
  const keyword = params.keyword || params.keywords || ''
  const status = params.status || ''
  const type = params.type || params.typeId || ''
  const author = params.author || ''
  const projectId = params.projectId || params.project || ''
  const yearRange = params.yearRange || params.years || []
  const page = Number(params.page) || 1
  const pageSize = Number(params.pageSize) || 10

  let list = [...source]

  if (keyword) {
    list = list.filter(
      (item) =>
        item.title.includes(keyword) ||
        item.abstract?.includes(keyword) ||
        item.authors?.some((a) => a.includes(keyword))
    )
  }

  if (status) {
    const statusList = Array.isArray(status) ? status : [status]
    list = list.filter((item) => statusList.includes(item.status))
  }

  if (type) {
    list = list.filter((item) => item.type === type || item.typeId === type)
  }

  if (author) {
    list = list.filter((item) => item.authors?.some((a) => a.includes(author)))
  }

  if (projectId) {
    list = list.filter((item) => item.projectId === projectId)
  }

  if (Array.isArray(yearRange) && yearRange.length === 2) {
    const [start, end] = yearRange
    list = list.filter((item) => {
      const year = Number(item.year)
      return (!start || year >= Number(start)) && (!end || year <= Number(end))
    })
  }

  const total = list.length
  const start = (page - 1) * pageSize
  const end = start + pageSize

  return {
    list: list.slice(start, end),
    total,
    page,
    pageSize
  }
}

function buildStatistics(source) {
  const totalResults = source.length
  const paperCount = source.filter((item) => item.type === 'paper').length
  const patentCount = source.filter((item) => item.type === 'patent').length
  const monthlyNew = source.filter((item) => (item.createdAt || '').startsWith(now().slice(0, 7))).length

  const typeMap = {}
  source.forEach((item) => {
    const key = item.type || item.typeId
    typeMap[key] = (typeMap[key] || 0) + 1
  })
  const typeDistribution = Object.entries(typeMap).map(([name, value]) => ({
    name: name === 'paper' ? '论文' : name === 'patent' ? '专利' : name,
    value
  }))

  const currentYear = Number(now().slice(0, 4))
  const yearlyTrend = []
  for (let i = 4; i >= 0; i--) {
    const year = (currentYear - i).toString()
    const count = source.filter((item) => String(item.year) === year).length
    yearlyTrend.push({ year, count })
  }

  return {
    totalResults,
    paperCount,
    patentCount,
    monthlyNew,
    typeDistribution,
    yearlyTrend
  }
}

function mergeProjectInfo(body = {}, fallback = {}) {
  const projectId = body.projectId || fallback.projectId || ''
  const project = findProject(projectId)
  return {
    projectId,
    projectName: body.projectName || project?.name || fallback.projectName || '',
    projectCode: body.projectCode || project?.code || fallback.projectCode || ''
  }
}

function findProject(id) {
  if (!id) return null
  return projects.find((item) => item.id === id || item.code === id) || null
}

function pageDemands(source, params) {
  const keyword = params.keyword || ''
  const industry = params.industry || ''
  const region = params.region || ''
  const sourceCategory = params.sourceCategory || ''
  const status = params.status || ''
  const page = Number(params.page) || 1
  const pageSize = Number(params.pageSize) || 10

  let list = [...source]

  if (keyword) {
    list = list.filter(
      (item) =>
        item.title.includes(keyword) ||
        item.summary?.includes(keyword) ||
        item.industry?.includes(keyword) ||
        item.region?.includes(keyword) ||
        item.tags?.some((t) => t.includes(keyword))
    )
  }

  if (industry) {
    list = list.filter((item) => item.industry === industry)
  }

  if (region) {
    list = list.filter((item) => item.region === region)
  }

  if (sourceCategory) {
    list = list.filter((item) => item.sourceCategory === sourceCategory)
  }

  if (status) {
    list = list.filter((item) => item.status === status)
  }

  const total = list.length
  const start = (page - 1) * pageSize
  const end = start + pageSize

  return {
    list: list.slice(start, end),
    total,
    page,
    pageSize
  }
}
