<template>
  <div class="policy-management">
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>爬虫数据源</span>
          <div class="header-actions">
            <el-button type="primary" :loading="syncAllLoading" @click="handleSyncAll">
              全部爬取
            </el-button>
            <el-button circle :disabled="crawlersLoading" @click="loadCrawlers()">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div class="crawler-cards" v-loading="crawlersLoading">
        <div v-for="crawler in crawlers" :key="crawler.id" class="crawler-card">
          <div class="crawler-card-top">
            <span class="crawler-name" :title="crawler.name">{{ crawler.name }}</span>
            <el-button
              size="small"
              :loading="crawler.status === 'running'"
              :disabled="crawler.status === 'running'"
              @click="handleSyncOne(crawler.id)"
            >
              爬取
            </el-button>
          </div>
          <div class="crawler-card-info">
            <span class="crawler-id">{{ crawler.id }}</span>
            <span class="crawler-sep">&middot;</span>
            <span v-if="crawler.status === 'running' && crawler.stats" class="crawler-progress">
              {{ crawler.stats.done }}/{{ crawler.stats.total }} 条已处理
            </span>
            <span v-else class="crawler-count">{{ crawler.policyCount }} 条数据</span>
            <el-tag
              :type="statusTagType(crawler.status)"
              size="small"
              effect="plain"
            >
              {{ statusText(crawler.status) }}
            </el-tag>
          </div>
        </div>
        <el-empty v-if="!crawlersLoading && crawlers.length === 0" description="暂无可用爬虫" />
      </div>
    </el-card>

    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>政策数据</span>
        </div>
      </template>
      <div class="search-form">
        <el-form :inline="true" :model="searchForm" class="search-fields">
          <el-form-item label="标题">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索标题"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="searchForm.crawlerId" placeholder="全部来源" clearable style="width: 200px">
              <el-option
                v-for="c in crawlers"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
          </el-form-item>
        </el-form>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <el-table :data="policies" v-loading="policiesLoading" stripe>
        <el-table-column prop="title" label="标题" min-width="300" show-overflow-tooltip />
        <el-table-column label="来源" width="220">
          <template #default="{ row }">
            {{ crawlerNameMap[row.crawlerId] || row.crawlerId }}
          </template>
        </el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="150" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openSource(row.sourceUrl)">
              查看原文
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadPolicies"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getCrawlerStatus,
  triggerCrawlerSync,
  triggerSyncAll,
  getPolicyList,
  type CrawlerStatus,
  type PolicyItem
} from '@/api/policy'

const crawlersLoading = ref(false)
const policiesLoading = ref(false)
const syncAllLoading = ref(false)
const crawlers = ref<CrawlerStatus[]>([])
const policies = ref<PolicyItem[]>([])
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  keyword: '',
  crawlerId: '',
  dateRange: null as [string, string] | null
})
let pollTimer: ReturnType<typeof setInterval> | null = null

const crawlerNameMap = computed(() => {
  const map: Record<string, string> = {}
  for (const c of crawlers.value) {
    map[c.id] = c.name
  }
  return map
})

onMounted(async () => {
  await Promise.all([loadCrawlers(), loadPolicies()])
})

onUnmounted(() => {
  stopPolling()
})

async function loadCrawlers() {
  crawlersLoading.value = true
  try {
    const res = await getCrawlerStatus()
    const incoming = res?.data || []
    mergeCrawlers(incoming)
    if (crawlers.value.some(c => c.status === 'running')) {
      startPolling()
    }
  } catch {
    ElMessage.error('加载爬虫列表失败')
  } finally {
    crawlersLoading.value = false
  }
}

function mergeCrawlers(incoming: CrawlerStatus[]) {
  const existingMap = new Map(crawlers.value.map(c => [c.id, c]))
  for (const item of incoming) {
    const existing = existingMap.get(item.id)
    if (existing) {
      existing.status = item.status
      existing.policyCount = item.policyCount
      existing.stats = item.stats
    } else {
      crawlers.value.push(item)
    }
  }
  const incomingIds = new Set(incoming.map(c => c.id))
  crawlers.value = crawlers.value.filter(c => incomingIds.has(c.id))
}

async function loadPolicies(page?: number) {
  if (page) pagination.page = page
  policiesLoading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.crawlerId) params.crawlerId = searchForm.crawlerId
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await getPolicyList(params)
    const data = res?.data
    policies.value = data?.records || []
    pagination.total = data?.total || 0
  } catch {
    ElMessage.error('加载政策数据失败')
  } finally {
    policiesLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadPolicies()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.crawlerId = ''
  searchForm.dateRange = null
  pagination.page = 1
  loadPolicies()
}

async function handleSyncAll() {
  syncAllLoading.value = true
  try {
    await triggerSyncAll()
    ElMessage.success('已触发全部爬取')
    startPolling()
  } catch {
    ElMessage.error('触发爬取失败')
  } finally {
    syncAllLoading.value = false
  }
}

async function handleSyncOne(crawlerId: string) {
  const crawler = crawlers.value.find(c => c.id === crawlerId)
  if (crawler) crawler.status = 'running'
  try {
    await triggerCrawlerSync(crawlerId)
    ElMessage.success(`已触发 ${crawlerId} 爬取`)
    startPolling()
  } catch {
    ElMessage.error(`触发 ${crawlerId} 爬取失败`)
    if (crawler) crawler.status = 'failed'
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    const res = await getCrawlerStatus().catch(() => null)
    if (res?.data) {
      mergeCrawlers(res.data)
    }
    const hasRunning = crawlers.value.some(c => c.status === 'running')
    if (!hasRunning) {
      stopPolling()
      await loadPolicies()
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function openSource(url: string) {
  if (url) window.open(url, '_blank')
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    idle: 'info',
    running: '',
    completed: 'success',
    failed: 'danger'
  }
  return (map[status] || 'info') as any
}

function statusText(status: string) {
  const map: Record<string, string> = {
    idle: '空闲',
    running: '爬取中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status
}
</script>

<style scoped>
.policy-management {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.crawler-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  min-height: 80px;
}

.crawler-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.crawler-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.crawler-name {
  font-weight: 600;
  font-size: 14px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.crawler-card-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #94a3b8;
}

.crawler-id {
  font-family: monospace;
}

.crawler-sep {
  color: #cbd5e1;
}

.crawler-count {
  color: #64748b;
}

.crawler-progress {
  color: #3b82f6;
  font-weight: 500;
}

.search-form {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.search-fields {
  flex: 1;
  min-width: 0;
}

.search-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  padding-top: 2px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
