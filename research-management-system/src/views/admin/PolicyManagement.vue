<template>
  <div class="policy-management">
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>爬虫数据源</span>
          <div class="header-actions">
            <el-button type="primary" :loading="syncAllLoading" @click="handleSyncAll">
              全部同步
            </el-button>
            <el-button :loading="matchLoading" @click="handleMatch">
              重新匹配
            </el-button>
          </div>
        </div>
      </template>
      <div class="crawler-cards" v-loading="crawlersLoading">
        <div v-for="crawler in crawlers" :key="crawler.id" class="crawler-card">
          <div class="crawler-card-header">
            <span class="crawler-name">{{ crawler.name }}</span>
            <el-button
              size="small"
              :loading="crawler.syncStatus === 'syncing'"
              :disabled="crawler.syncStatus === 'syncing'"
              @click="handleSyncOne(crawler.id)"
            >
              同步
            </el-button>
          </div>
          <div class="crawler-card-body">
            <span class="crawler-id">{{ crawler.id }}</span>
            <div class="crawler-tags">
              <el-tag
                :type="statusTagType(crawler.syncStatus)"
                size="small"
                effect="plain"
              >
                {{ syncStatusText(crawler.syncStatus) }}
              </el-tag>
              <el-tag
                v-if="crawler.crawlerStatus !== 'idle'"
                :type="crawler.crawlerStatus === 'unavailable' ? 'danger' : 'info'"
                size="small"
                effect="plain"
              >
                {{ crawler.crawlerStatus === 'unavailable' ? '不可用' : crawler.crawlerStatus }}
              </el-tag>
            </div>
            <span class="crawler-count">{{ crawler.policyCount }} 条数据</span>
          </div>
        </div>
        <el-empty v-if="!crawlersLoading && crawlers.length === 0" description="暂无可用爬虫" />
      </div>
    </el-card>

    <el-card class="section-card">
      <template #header>
        <span>政策数据</span>
      </template>
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
import {
  getCrawlerStatus,
  triggerCrawlerSync,
  triggerSyncAll,
  triggerMatch,
  getPolicyList,
  type CrawlerStatus,
  type PolicyItem
} from '@/api/policy'

const crawlersLoading = ref(false)
const policiesLoading = ref(false)
const syncAllLoading = ref(false)
const matchLoading = ref(false)
const crawlers = ref<CrawlerStatus[]>([])
const policies = ref<PolicyItem[]>([])
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })
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
      existing.syncStatus = item.syncStatus
      existing.crawlerStatus = item.crawlerStatus
      existing.policyCount = item.policyCount
    } else {
      crawlers.value.push(item)
    }
  }
  // 移除不再存在的爬虫
  const incomingIds = new Set(incoming.map(c => c.id))
  crawlers.value = crawlers.value.filter(c => incomingIds.has(c.id))
}

async function loadPolicies(page?: number) {
  if (page) pagination.page = page
  policiesLoading.value = true
  try {
    const res = await getPolicyList(pagination.page, pagination.pageSize)
    const data = res?.data
    policies.value = data?.records || []
    pagination.total = data?.total || 0
  } catch {
    ElMessage.error('加载政策数据失败')
  } finally {
    policiesLoading.value = false
  }
}

async function handleSyncAll() {
  syncAllLoading.value = true
  try {
    await triggerSyncAll()
    ElMessage.success('已触发全部同步')
    startPolling()
  } catch {
    ElMessage.error('触发同步失败')
  } finally {
    syncAllLoading.value = false
  }
}

async function handleSyncOne(crawlerId: string) {
  const crawler = crawlers.value.find(c => c.id === crawlerId)
  if (crawler) crawler.syncStatus = 'syncing'
  try {
    await triggerCrawlerSync(crawlerId)
    ElMessage.success(`已触发 ${crawlerId} 同步`)
    startPolling()
  } catch {
    ElMessage.error(`触发 ${crawlerId} 同步失败`)
    if (crawler) crawler.syncStatus = 'failed'
  }
}

async function handleMatch() {
  matchLoading.value = true
  try {
    await triggerMatch()
    ElMessage.success('匹配完成')
  } catch {
    ElMessage.error('匹配失败')
  } finally {
    matchLoading.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    const res = await getCrawlerStatus().catch(() => null)
    if (res?.data) {
      mergeCrawlers(res.data)
    }
    const hasSyncing = crawlers.value.some(c => c.syncStatus === 'syncing')
    if (!hasSyncing) {
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
    syncing: '',
    completed: 'success',
    failed: 'danger'
  }
  return (map[status] || 'info') as any
}

function syncStatusText(status: string) {
  const map: Record<string, string> = {
    idle: '空闲',
    syncing: '同步中',
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
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.crawler-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.crawler-name {
  font-weight: 600;
  font-size: 14px;
  color: #334155;
}

.crawler-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.crawler-id {
  font-family: monospace;
}

.crawler-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.crawler-count {
  color: #64748b;
  font-size: 13px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
