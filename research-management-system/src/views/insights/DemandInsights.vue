<template>
  <div class="demand-insights">
    <div class="page-header">
      <div>
        <h2>需求洞察与智能匹配</h2>
        <p class="subtitle">
          基于公开官方/平台真实来源策展需求线索，用“需求入池 -> 结构化 -> 匹配 -> 跟进”解释下一版本主线能力。
        </p>
      </div>
      <div class="header-actions">
        <el-tag type="success" effect="plain">真实来源优先</el-tag>
        <el-button :loading="listLoading || statsLoading" @click="refreshAll">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :md="6" v-for="card in summaryCards" :key="card.key">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-note">{{ card.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="source-card">
      <template #header>
        <div class="section-head">
          <div class="section-title">来源白名单快照</div>
          <div class="section-desc">当前演示数据以公开官方/平台来源为主，不做现场实时抓取。</div>
        </div>
      </template>
      <div class="source-tags">
        <el-link
          v-for="source in stats?.sources || []"
          :key="source.name"
          class="source-link"
          :href="source.url"
          target="_blank"
          type="primary"
          :underline="false"
        >
          <span>{{ source.name }}</span>
          <el-tag size="small" effect="plain">{{ source.count }} 条</el-tag>
        </el-link>
      </div>
    </el-card>

    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            placeholder="需求标题、来源、摘要"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="filters.industry" placeholder="全部" clearable>
            <el-option v-for="opt in industryOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="地域">
          <el-select v-model="filters.region" placeholder="全部" clearable>
            <el-option v-for="opt in regionOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="filters.sourceCategory" placeholder="全部" clearable>
            <el-option v-for="opt in sourceCategoryOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable>
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="listLoading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        :data="demandList"
        :loading="listLoading"
        height="560"
        border
        highlight-current-row
        @row-click="handleRowClick"
      >
        <el-table-column prop="title" label="需求线索" min-width="320">
          <template #default="{ row }">
            <div class="title-cell">
              <div class="title-text">{{ row.title }}</div>
              <div class="title-tags">
                <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
              </div>
              <div class="meta-line">
                <span>{{ row.sourceName || row.sourceSite }}</span>
                <span class="dot">·</span>
                <span>{{ row.sourceCategory || '来源未分类' }}</span>
                <span class="dot">·</span>
                <span>{{ row.publishDate || '发布日期未知' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="行业 / 地域" min-width="160">
          <template #default="{ row }">
            <div class="stack-cell">
              <span>{{ row.industry || '未分类行业' }}</span>
              <span class="muted">{{ row.region || '未知地域' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="capturedAt" label="采集日期" width="120" />
        <el-table-column prop="confidence" label="可信度" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round((row.confidence || 0) * 100)"
              :stroke-width="10"
              :color="progressColor(row.confidence || 0)"
              :format="(p) => `${p}%`"
            />
          </template>
        </el-table-column>
        <el-table-column label="匹配度" width="110">
          <template #default="{ row }">
            <div class="score-pill" :class="scoreClass(row.bestMatchScore)">
              {{ formatScore(row.bestMatchScore) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="180">
          <template #default="{ row }">
            <div class="stack-cell">
              <el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
              <span class="muted">
                {{ followUpLabel(row.followUpStatus) }} / {{ row.followUpOwner || '待分派' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="handleRowClick(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          background
          layout="prev, pager, next, jumper, total"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.page"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" size="720px" title="需求详情与匹配" destroy-on-close>
      <el-skeleton :loading="detailLoading" animated :rows="8">
        <template #default>
          <div v-if="selectedDemand" class="drawer-body">
            <div class="drawer-header">
              <div>
                <div class="drawer-title">{{ selectedDemand.title }}</div>
                <div class="drawer-sub">
                  <el-tag size="small" effect="plain">{{ selectedDemand.sourceCategory }}</el-tag>
                  <span>{{ selectedDemand.sourceName || selectedDemand.sourceSite }}</span>
                  <span class="dot">·</span>
                  <span>发布 {{ selectedDemand.publishDate || '未知' }}</span>
                  <span class="dot">·</span>
                  <span>采集 {{ selectedDemand.capturedDate || selectedDemand.capturedAt || '未知' }}</span>
                </div>
              </div>
              <div class="drawer-actions">
                <el-tag :type="statusType(selectedDemand.status)">{{ statusLabel(selectedDemand.status) }}</el-tag>
                <el-button size="small" :loading="rematching" @click="handleRematch">
                  <el-icon><Refresh /></el-icon>
                  重新匹配
                </el-button>
              </div>
            </div>

            <div class="drawer-grid">
              <section class="info-section">
                <div class="section-head">
                  <div class="section-title">来源说明</div>
                  <div class="section-desc">展示真实来源、策展口径和关键证据。</div>
                </div>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">来源名称</span>
                    <span class="info-value">{{ selectedDemand.sourceName || selectedDemand.sourceSite }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">发布日期</span>
                    <span class="info-value">{{ selectedDemand.publishDate || '未知' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">采集日期</span>
                    <span class="info-value">{{ selectedDemand.capturedDate || selectedDemand.capturedAt || '未知' }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">当前责任人</span>
                    <span class="info-value">{{ selectedDemand.followUpOwner || '待分派' }}</span>
                  </div>
                </div>
                <div class="evidence-box">
                  <div class="evidence-title">策展说明</div>
                  <p>{{ selectedDemand.curationNote || '暂无策展说明。' }}</p>
                </div>
                <div class="evidence-box">
                  <div class="evidence-title">来源依据</div>
                  <p>{{ selectedDemand.evidenceText || '暂无来源依据。' }}</p>
                </div>
                <el-link v-if="selectedDemand.sourceUrl" :href="selectedDemand.sourceUrl" target="_blank" type="primary">
                  查看原始来源
                </el-link>
              </section>

              <section class="info-section">
                <div class="section-head">
                  <div class="section-title">需求摘要</div>
                  <div class="section-desc">用结构化摘要解释需求对象，而不是直接展示原网页。</div>
                </div>
                <p class="body-copy">{{ selectedDemand.summary }}</p>
                <div class="llm-summary">智能摘要：{{ selectedDemand.llmSummary || '暂无智能摘要' }}</div>
                <div class="chip-group">
                  <el-tag v-for="kw in selectedDemand.keywords || []" :key="kw" size="small" effect="plain">{{ kw }}</el-tag>
                </div>
              </section>
            </div>

            <section class="info-section">
              <div class="section-head">
                <div class="section-title">跟进时间线</div>
                <div class="section-desc">演示人工确认与运营闭环，不包装成自动推进。</div>
              </div>
              <el-empty v-if="!selectedDemand.timeline?.length" description="暂无跟进记录" />
              <el-timeline v-else>
                <el-timeline-item
                  v-for="item in selectedDemand.timeline"
                  :key="`${item.time}-${item.title}`"
                  :timestamp="item.time"
                  :type="timelineType(item.status)"
                >
                  <div class="timeline-title">{{ item.title }}</div>
                  <div class="timeline-meta">{{ item.actor || '系统' }}</div>
                  <p class="timeline-note">{{ item.note || '—' }}</p>
                </el-timeline-item>
              </el-timeline>
            </section>

            <section class="info-section">
              <div class="section-head">
                <div class="section-title">智能匹配结果</div>
                <div class="section-desc">候选结果按匹配度排序，并展示证据片段与匹配理由。</div>
              </div>
              <el-empty v-if="matches.length === 0" description="当前暂无候选匹配结果" />
              <div v-else class="match-list">
                <div v-for="item in matches" :key="item.resultId" class="match-card">
                  <div class="match-header">
                    <div>
                      <div class="match-title">{{ item.resultTitle }}</div>
                      <div class="match-sub">
                        <span>{{ item.resultType }}</span>
                        <span class="dot">·</span>
                        <span>{{ item.owner || '负责人待定' }}</span>
                      </div>
                    </div>
                    <div class="score-pill" :class="scoreClass(item.matchScore)">
                      {{ formatScore(item.matchScore) }}
                    </div>
                  </div>
                  <p class="body-copy">{{ item.reason || '暂无匹配理由' }}</p>
                  <div class="evidence-box">
                    <div class="evidence-title">来源片段</div>
                    <p>{{ item.sourceSnippet || '暂无片段' }}</p>
                  </div>
                  <div v-if="item.matchEvidence?.length" class="chip-group">
                    <el-tag v-for="evidence in item.matchEvidence" :key="evidence" size="small" effect="plain">
                      {{ evidence }}
                    </el-tag>
                  </div>
                  <el-button type="primary" link @click="goResultDetail(item.resultId)">查看成果详情</el-button>
                </div>
              </div>
            </section>
          </div>
        </template>
      </el-skeleton>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { getDemandDetail, getDemands, getDemandStats, rematchDemand } from '@/api/demand'
import type { DemandItem, DemandStats, DemandMatch } from '@/types'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const filters = reactive({
  keyword: '',
  industry: '',
  region: '',
  sourceCategory: '',
  status: ''
})

const statusOptions = [
  { label: '未匹配', value: 'unmatched' },
  { label: '已匹配', value: 'matched' },
  { label: '跟进中', value: 'in_follow_up' }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = ref<DemandStats | null>(null)
const demandList = ref<DemandItem[]>([])
const selectedDemand = ref<DemandItem | null>(null)
const matches = ref<DemandMatch[]>([])
const drawerVisible = ref(false)
const listLoading = ref(false)
const statsLoading = ref(false)
const detailLoading = ref(false)
const rematching = ref(false)

const industryOptions = computed(() => stats.value?.industries || [])
const regionOptions = computed(() => stats.value?.regions || [])
const sourceCategoryOptions = computed(() => stats.value?.sourceCategories || [])

const summaryCards = computed(() => {
  const total = stats.value?.total || 0
  const matched = stats.value?.matched || 0
  const inFollowUp = stats.value?.inFollowUp || 0
  const unmatched = stats.value?.unmatched || 0
  const averageConfidence = Math.round((stats.value?.averageConfidence || 0) * 100)

  return [
    { key: 'total', label: '真实线索总数', value: `${total}`, note: '来自公开官方/平台来源的策展样本。' },
    { key: 'matched', label: '已形成候选匹配', value: `${matched}`, note: '已输出候选成果和匹配理由。' },
    { key: 'follow', label: '跟进中', value: `${inFollowUp}`, note: '用于展示人工确认与推进闭环。' },
    { key: 'confidence', label: '平均可信度', value: `${averageConfidence}%`, note: `未匹配 ${unmatched} 条，适合继续补充样本。` }
  ]
})

function progressColor(value: number) {
  if (value >= 0.9) return '#16a34a'
  if (value >= 0.8) return '#0ea5e9'
  if (value >= 0.6) return '#f59e0b'
  return '#94a3b8'
}

function scoreClass(score?: number) {
  if (score === undefined || score === null) return 'score-neutral'
  if (score >= 0.85) return 'score-high'
  if (score >= 0.7) return 'score-mid'
  return 'score-low'
}

function formatScore(score?: number) {
  if (score === undefined || score === null) return '--'
  return `${Math.round(score * 100)}%`
}

function statusType(status?: string) {
  if (status === 'matched') return 'success'
  if (status === 'in_follow_up') return 'warning'
  return 'info'
}

function statusLabel(status?: string) {
  if (status === 'matched') return '已匹配'
  if (status === 'in_follow_up') return '跟进中'
  if (status === 'unmatched') return '未匹配'
  return status || '未知'
}

function followUpLabel(status?: string) {
  if (status === 'confirmed') return '已确认'
  if (status === 'in_progress') return '推进中'
  if (status === 'completed') return '已完成'
  return '待确认'
}

function timelineType(status?: string) {
  if (status === 'done') return 'success'
  if (status === 'active') return 'warning'
  return 'info'
}

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getDemandStats()
    stats.value = res?.data || null
  } finally {
    statsLoading.value = false
  }
}

async function loadDemands() {
  listLoading.value = true
  try {
    const res = await getDemands({
      ...filters,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    demandList.value = res?.data?.list || []
    pagination.total = res?.data?.total || 0
  } finally {
    listLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadDemands()
}

function handleReset() {
  filters.keyword = ''
  filters.industry = ''
  filters.region = ''
  filters.sourceCategory = ''
  filters.status = ''
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadDemands()
}

async function handleRowClick(row: DemandItem) {
  await loadDetail(row.id)
}

async function loadDetail(id: string) {
  detailLoading.value = true
  try {
    const res = await getDemandDetail(id)
    selectedDemand.value = res?.data || null
    matches.value = res?.data?.matches || []
    drawerVisible.value = true
  } finally {
    detailLoading.value = false
  }
}

async function handleRematch() {
  const id = selectedDemand.value?.id
  if (!id) {
    ElMessage.warning('缺少需求 ID')
    return
  }

  rematching.value = true
  try {
    await rematchDemand(id)
    await loadDetail(id)
    await loadDemands()
    ElMessage.success('已重新匹配，结果已更新')
  } finally {
    rematching.value = false
  }
}

function goResultDetail(resultId: string) {
  router.push(`/results/${resultId}`)
}

async function refreshAll() {
  await Promise.all([loadStats(), loadDemands()])
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.demand-insights {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.section-head,
.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 8px;
}

.subtitle,
.section-desc,
.body-copy,
.timeline-note {
  color: #64748b;
  line-height: 1.7;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-row {
  margin: 0;
}

.summary-card,
.filter-card,
.table-card,
.source-card,
.info-section {
  border: 1px solid #e5eef7;
  border-radius: 18px;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
}

.summary-value {
  margin-top: 14px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.summary-note {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.source-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.title-cell,
.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title-text,
.drawer-title,
.match-title,
.timeline-title {
  font-weight: 700;
  color: #0f172a;
}

.title-tags,
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.meta-line,
.drawer-sub,
.match-sub,
.timeline-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.muted,
.dot {
  color: #94a3b8;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 16px;
}

.info-section {
  padding: 16px;
  background: #ffffff;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 14px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.info-label {
  font-size: 12px;
  color: #64748b;
}

.info-value {
  color: #0f172a;
  font-weight: 600;
}

.evidence-box {
  margin: 14px 0;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.evidence-title {
  margin-bottom: 8px;
  font-weight: 700;
  color: #0f172a;
}

.llm-summary {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
  color: #1e3a8a;
}

.match-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.match-card {
  padding: 16px;
  border-radius: 16px;
  border: 1px solid #e5eef7;
  background: #fafcff;
}

.match-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.score-pill {
  padding: 5px 10px;
  border-radius: 999px;
  min-width: 72px;
  text-align: center;
  font-weight: 700;
}

.score-high {
  background: #dcfce7;
  color: #15803d;
}

.score-mid {
  background: #dbeafe;
  color: #1d4ed8;
}

.score-low {
  background: #fff7ed;
  color: #c2410c;
}

.score-neutral {
  background: #f1f5f9;
  color: #475569;
}

@media (max-width: 960px) {
  .page-header,
  .drawer-header {
    flex-direction: column;
  }

  .drawer-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
