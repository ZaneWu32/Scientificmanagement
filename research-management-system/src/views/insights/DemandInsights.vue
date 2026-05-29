<template>
  <div class="demand-insights">
    <!-- 顶部 -->
    <header class="page-header">
      <div>
        <div class="eyebrow">智能洞察 · 需求转化</div>
        <h2>需求洞察</h2>
        <p class="subtitle">白名单来源持续采集 → 结构化摘要 → 候选成果匹配 → 人工确认跟进</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDemands()">刷新</el-button>
        <el-tag type="success" effect="plain">ES 实时匹配</el-tag>
      </div>
    </header>

    <!-- 指标卡 -->
    <section class="metrics-row">
      <div v-for="m in metrics" :key="m.label" class="metric-card">
        <div class="metric-icon" :style="{ background: m.iconBg }">
          <el-icon :size="20" :style="{ color: m.iconColor }"><component :is="m.icon" /></el-icon>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
        <div class="metric-badge" :class="m.badgeClass">{{ m.badge }}</div>
      </div>
    </section>

    <!-- 主体：需求卡片列表 + 详情侧栏 -->
    <section class="main-grid">
      <!-- 左：筛选 + 卡片列表 -->
      <div class="demand-list-col" v-loading="loading">
        <div class="list-toolbar">
          <el-input
            v-model="filters.keyword"
            placeholder="关键词、行业、来源…"
            clearable
            :prefix-icon="Search"
            class="search-input"
          />
          <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:140px">
            <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </div>

        <div class="demand-cards">
          <div
            v-for="item in filteredDemands"
            :key="item.id"
            class="demand-card"
            :class="{ 'is-active': selectedDemand?.id === item.id }"
            @click="selectDemand(item)"
          >
            <div class="card-top">
              <span class="priority-dot" :class="`pri-${item.priority}`"></span>
              <span class="card-industry">{{ item.industry }}</span>
              <el-tag :type="statusType(item.status)" effect="plain" size="small" class="card-status">
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
            <div class="card-title">{{ item.title }}</div>
            <div class="card-meta">
              <span>{{ item.sourceSite }}</span>
              <span>{{ item.region }}</span>
              <span>{{ formatDate(item.capturedAt) }}</span>
            </div>
            <div class="card-footer">
              <div class="match-score-bar">
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: toPercent(item.bestMatchScore) + '%', background: scoreColor(item.bestMatchScore) }"></div>
                </div>
                <span class="bar-label">最佳匹配 {{ toPercent(item.bestMatchScore) }}%</span>
              </div>
            </div>
          </div>
          <el-empty v-if="!filteredDemands.length" description="暂无匹配需求" :image-size="60" />
        </div>
      </div>

      <!-- 右：详情 -->
      <aside class="detail-col" v-if="selectedDemand">
        <div class="detail-panel">
          <!-- 详情头 -->
          <div class="detail-head">
            <div class="detail-head-left">
              <el-tag :type="statusType(selectedDemand.status)" effect="plain">{{ statusLabel(selectedDemand.status) }}</el-tag>
              <el-tag effect="plain" size="small" class="priority-tag" :class="`pri-tag-${selectedDemand.priority}`">
                {{ selectedDemand.priority }}优先级
              </el-tag>
            </div>
            <el-button size="small" :icon="Search" :loading="rematching" @click="handleRematch">重新匹配</el-button>
          </div>
          <h3 class="detail-title">{{ selectedDemand.title }}</h3>
          <div class="detail-meta">
            <span>📍 {{ selectedDemand.region }}</span>
            <span>🏭 {{ selectedDemand.industry }}</span>
            <span>🔗 {{ selectedDemand.sourceSite }}</span>
          </div>

          <!-- AI摘要 -->
          <div class="detail-section">
            <div class="section-label">🤖 智能摘要</div>
            <p class="detail-summary">{{ selectedDemand.llmSummary }}</p>
            <div class="keyword-row">
              <el-tag v-for="kw in selectedDemand.keywords" :key="kw" size="small" effect="plain" round>{{ kw }}</el-tag>
            </div>
          </div>

          <!-- 候选成果 -->
          <div class="detail-section">
            <div class="section-label">🎯 候选成果匹配</div>
            <el-empty v-if="!selectedDemand.matches.length" description="暂无高置信候选成果" :image-size="56" />
            <div v-for="m in selectedDemand.matches" :key="m.resultId" class="match-card">
              <div class="match-header">
                <span class="match-title">{{ m.resultTitle }}</span>
                <span class="match-type">{{ m.resultType }}</span>
              </div>
              <div class="match-score-row">
                <span class="match-score-label">匹配度</span>
                <el-progress :percentage="toPercent(m.matchScore)" :stroke-width="8"
                  :color="scoreColor(m.matchScore)" style="flex:1" />
                <span class="match-score-num" :style="{color: scoreColor(m.matchScore)}">{{ toPercent(m.matchScore) }}%</span>
              </div>
              <p class="match-reason">{{ m.reason }}</p>
              <div class="match-snippet">{{ m.sourceSnippet }}</div>
              <div class="fit-tags">
                <el-tag v-for="t in m.fitTags" :key="t" size="small" type="success" effect="plain">{{ t }}</el-tag>
              </div>
            </div>
          </div>

          <!-- 跟进步骤 -->
          <div class="detail-section" v-if="selectedDemand.followUp?.length">
            <div class="section-label">📋 跟进步骤</div>
            <el-timeline>
              <el-timeline-item
                v-for="step in selectedDemand.followUp"
                :key="step.label"
                :type="timelineType(step.status)"
                :hollow="step.status === 'todo'"
                size="large"
              >
                <div class="timeline-content">
                  <span class="tl-label">{{ step.label }}</span>
                  <span class="tl-meta">{{ step.owner }} · {{ formatDate(step.dueAt) }}</span>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <!-- 待确认 -->
          <div class="detail-section" v-if="selectedDemand.pendingConfirmations?.length">
            <div class="section-label">⚠️ 待人工确认</div>
            <ul class="confirm-list">
              <li v-for="item in selectedDemand.pendingConfirmations" :key="item">{{ item }}</li>
            </ul>
          </div>
        </div>
      </aside>
      <div v-else class="detail-empty">
        <el-empty description="点击左侧需求卡片查看详情" :image-size="80" />
      </div>
    </section>

    <!-- 底部：数据源状态 -->
    <section class="panel source-section">
      <div class="section-header">
        <h3>数据源健康状态</h3>
        <p>白名单采集源，仅接入公开、稳定来源，不做全网泛化抓取</p>
      </div>
      <div class="source-grid">
        <div v-for="src in demandSources" :key="src.id" class="source-card">
          <div class="source-card-head">
            <span class="source-name">{{ src.name }}</span>
            <el-tag :type="sourceTagType(src.status)" effect="plain" size="small">{{ sourceLabel(src.status) }}</el-tag>
          </div>
          <div class="source-stats">
            <span>{{ src.type }}</span>
            <span>每 {{ src.frequencyHours }}h 采集</span>
            <span>成功率 {{ src.successRate }}%</span>
            <span>新增 {{ src.newCount }} 条</span>
          </div>
          <div v-if="src.failureReason && src.failureReason !== '-'" class="source-warn">{{ src.failureReason }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Search, TrendCharts, CircleCheck, Promotion, Refresh, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { getDemands, getDemandSources, rematchDemand } from '@/api/demand'

type DemandStatus = 'new' | 'reviewing' | 'matched' | 'in_follow_up' | 'invalid' | 'archived'
type SourceHealth = 'healthy' | 'warning' | 'error' | 'idle'

interface DemandMatchItem {
  resultId: string
  resultTitle: string
  resultType: string
  owner: string
  department: string
  matchScore: number
  reason: string
  sourceSnippet: string
  updatedAt: string
  fitTags: string[]
}

interface DemandFollowUpItem {
  label: string
  owner: string
  status: string
  dueAt: string
}

interface DemandItem {
  id: string
  title: string
  sourceCategory: string
  sourceSite: string
  sourceUrl: string
  capturedAt: string
  industry: string
  region: string
  priority: '高' | '中' | '低'
  confidence: number
  bestMatchScore: number
  status: DemandStatus
  valueLevel: string
  owner: string
  dueAt: string
  summary: string
  llmSummary: string
  keywords: string[]
  tags: string[]
  pendingConfirmations: string[]
  riskNotes: string[]
  matches: DemandMatchItem[]
  followUp: DemandFollowUpItem[]
}

interface DemandSourceItem {
  id: string
  name: string
  type: string
  industry: string
  region: string
  frequencyHours: number
  enabled: boolean
  status: SourceHealth
  lastRunAt: string
  lastSuccessAt: string
  failureReason: string
  successRate: number
  newCount: number
  matchedCount: number
}

const demands = ref<DemandItem[]>([])
const demandSources = ref<DemandSourceItem[]>([])
const selectedDemand = ref<DemandItem | null>(null)
const loading = ref(false)
const rematching = ref(false)
const filters = reactive({ keyword: '', status: '' })

const statusOptions = [
  { label: '新入池', value: 'new' },
  { label: '研判中', value: 'reviewing' },
  { label: '已匹配', value: 'matched' },
  { label: '跟进中', value: 'in_follow_up' }
]

const filteredDemands = computed(() => {
  const kw = filters.keyword.trim().toLowerCase()
  return demands.value.filter(item => {
    const kwHit = !kw || [item.title, item.summary, item.industry, item.region, ...(item.keywords || [])].join(' ').toLowerCase().includes(kw)
    const sHit = !filters.status || item.status === filters.status
    return kwHit && sHit
  })
})

const metrics = computed(() => {
  const total = demands.value.length
  const matched = demands.value.filter(d => d.matches.length > 0).length
  const followUp = demands.value.filter(d => d.status === 'in_follow_up').length
  const srcOk = demandSources.value.filter(s => s.status === 'healthy').length
  return [
    { label: '入池需求', value: total, badge: '去重后', badgeClass: 'badge-blue', icon: Tickets, iconBg: '#eff6ff', iconColor: '#2563eb' },
    { label: '有候选成果', value: matched, badge: '待研判', badgeClass: 'badge-green', icon: CircleCheck, iconBg: '#f0fdf4', iconColor: '#16a34a' },
    { label: '跟进中', value: followUp, badge: '人工确认后', badgeClass: 'badge-amber', icon: Promotion, iconBg: '#fffbeb', iconColor: '#d97706' },
    { label: '正常数据源', value: `${srcOk}/${demandSources.value.length}`, badge: '白名单', badgeClass: 'badge-purple', icon: TrendCharts, iconBg: '#f5f3ff', iconColor: '#7c3aed' }
  ]
})

function selectDemand(row: DemandItem) { selectedDemand.value = row }
function toPercent(v: number) { return Math.round(v * 100) }

async function loadDemands(keepId?: string) {
  loading.value = true
  try {
    const [demandRes, sourceRes] = await Promise.all([
      getDemands({ pageNum: 1, pageSize: 100 }),
      getDemandSources()
    ])
    demands.value = normalizeDemands(demandRes?.data?.records || demandRes?.data || [])
    demandSources.value = normalizeSources(sourceRes?.data || [])
    selectedDemand.value = demands.value.find(item => String(item.id) === keepId) || demands.value[0] || null
  } finally {
    loading.value = false
  }
}

async function handleRematch() {
  if (!selectedDemand.value) return
  rematching.value = true
  const demandId = String(selectedDemand.value.id)
  try {
    await rematchDemand(demandId)
    await loadDemands(demandId)
    ElMessage.success('已重新计算候选成果匹配')
  } finally {
    rematching.value = false
  }
}

function normalizeDemands(rows: any[]): DemandItem[] {
  return rows.map(row => ({
    ...row,
    id: String(row.id),
    priority: row.priority || '中',
    confidence: Number(row.confidence || 0),
    bestMatchScore: Number(row.bestMatchScore || 0),
    keywords: row.keywords || [],
    tags: row.tags || [],
    pendingConfirmations: row.pendingConfirmations || [],
    riskNotes: row.riskNotes || [],
    matches: (row.matches || []).map((m: any) => ({
      ...m,
      matchScore: Number(m.matchScore || 0),
      fitTags: m.fitTags || [],
      updatedAt: m.updatedAt || ''
    })),
    followUp: row.followUp || []
  }))
}

function normalizeSources(rows: any[]): DemandSourceItem[] {
  return rows.map(row => ({
    ...row,
    id: String(row.id),
    successRate: Number(row.successRate || 0),
    newCount: Number(row.newCount || 0),
    matchedCount: Number(row.matchedCount || 0)
  }))
}

function formatDate(value?: string) {
  if (!value) return '-'
  return value.slice(0, 10)
}

function scoreColor(score: number) {
  if (score >= 0.8) return '#16a34a'
  if (score >= 0.65) return '#d97706'
  return '#dc2626'
}

function statusType(status: DemandStatus) {
  const m: Record<DemandStatus, any> = { new: 'info', reviewing: 'primary', matched: 'success', in_follow_up: 'warning', invalid: 'danger', archived: 'info' }
  return m[status]
}
function statusLabel(status: DemandStatus) {
  const m: Record<DemandStatus, string> = { new: '新入池', reviewing: '研判中', matched: '已匹配', in_follow_up: '跟进中', invalid: '无效', archived: '已归档' }
  return m[status]
}
function sourceTagType(s: SourceHealth) {
  if (s === 'healthy') return 'success'; if (s === 'warning') return 'warning'; if (s === 'error') return 'danger'; return 'info'
}
function sourceLabel(s: SourceHealth) {
  const m: Record<SourceHealth, string> = { healthy: '正常', warning: '预警', error: '异常', idle: '停用' }
  return m[s]
}
function timelineType(status: string) {
  if (status === 'done') return 'success'; if (status === 'doing') return 'primary'; return 'info'
}

onMounted(() => loadDemands())
</script>

<style scoped>
.demand-insights { display: flex; flex-direction: column; gap: 16px; }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.eyebrow { font-size: 12px; font-weight: 700; color: #2563eb; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 4px; }
h2 { margin: 0; font-size: 24px; font-weight: 800; color: #0f172a; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: #64748b; }

/* 指标卡 */
.metrics-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.metric-card {
  background: #fff; border: 1px solid #e5e7eb; border-radius: 12px;
  padding: 16px; display: flex; align-items: center; gap: 14px;
  box-shadow: 0 4px 14px rgba(15,23,42,.05); position: relative; overflow: hidden;
}
.metric-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.metric-body { flex: 1; }
.metric-value { font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1; }
.metric-label { font-size: 12px; color: #64748b; margin-top: 3px; }
.metric-badge { font-size: 11px; padding: 2px 8px; border-radius: 999px; white-space: nowrap; align-self: flex-start; }
.badge-blue { background: #dbeafe; color: #1d4ed8; }
.badge-green { background: #dcfce7; color: #15803d; }
.badge-amber { background: #fef9c3; color: #a16207; }
.badge-purple { background: #ede9fe; color: #6d28d9; }

/* 主体双栏 */
.main-grid { display: grid; grid-template-columns: 380px 1fr; gap: 16px; min-height: 600px; }

/* 左栏 */
.demand-list-col { display: flex; flex-direction: column; gap: 12px; }
.list-toolbar { display: flex; gap: 8px; }
.search-input { flex: 1; }
.demand-cards { display: flex; flex-direction: column; gap: 10px; overflow-y: auto; max-height: 560px; padding-right: 4px; }

/* 需求卡片 */
.demand-card {
  background: #fff; border: 1.5px solid #e5e7eb; border-radius: 12px; padding: 14px;
  cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(15,23,42,.04);
}
.demand-card:hover { border-color: #93c5fd; box-shadow: 0 4px 16px rgba(37,99,235,.1); }
.demand-card.is-active { border-color: #2563eb; background: #eff6ff; box-shadow: 0 4px 16px rgba(37,99,235,.15); }

.card-top { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.priority-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.pri-高 { background: #ef4444; }
.pri-中 { background: #f59e0b; }
.pri-低 { background: #6b7280; }
.card-industry { font-size: 12px; color: #6b7280; flex: 1; }
.card-status { flex-shrink: 0; }

.card-title { font-size: 14px; font-weight: 700; color: #111827; line-height: 1.45; margin-bottom: 8px; }
.card-meta { display: flex; gap: 8px; flex-wrap: wrap; font-size: 12px; color: #9ca3af; margin-bottom: 10px; }
.card-meta span::after { content: '·'; margin-left: 8px; }
.card-meta span:last-child::after { content: ''; }

.match-score-bar { display: flex; align-items: center; gap: 8px; }
.bar-track { flex: 1; height: 5px; background: #f1f5f9; border-radius: 99px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 99px; transition: width 0.5s; }
.bar-label { font-size: 12px; color: #64748b; white-space: nowrap; }

/* 右栏详情 */
.detail-col { overflow-y: auto; max-height: 640px; }
.detail-empty { display: flex; align-items: center; justify-content: center; background: #f9fafb; border-radius: 12px; border: 1.5px dashed #e5e7eb; }
.detail-panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; box-shadow: 0 4px 18px rgba(15,23,42,.06); }

.detail-head { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 10px; }
.detail-head-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.detail-title { margin: 0 0 10px; font-size: 17px; font-weight: 800; color: #0f172a; line-height: 1.4; }
.detail-meta { display: flex; gap: 14px; flex-wrap: wrap; font-size: 12px; color: #6b7280; margin-bottom: 14px; }

.priority-tag { border-radius: 999px; }
.pri-tag-高 { background: #fee2e2; color: #dc2626; border-color: #fca5a5; }
.pri-tag-中 { background: #fef9c3; color: #a16207; border-color: #fde68a; }
.pri-tag-低 { background: #f3f4f6; color: #6b7280; border-color: #e5e7eb; }

.detail-section { margin-top: 18px; padding-top: 16px; border-top: 1px solid #f1f5f9; }
.section-label { font-size: 12px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 10px; }
.detail-summary { font-size: 14px; color: #374151; line-height: 1.7; margin: 0 0 10px; }
.keyword-row { display: flex; flex-wrap: wrap; gap: 6px; }

/* 候选成果卡 */
.match-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; margin-bottom: 10px; background: #f9fafb; }
.match-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.match-title { font-size: 14px; font-weight: 700; color: #111827; flex: 1; }
.match-type { font-size: 12px; color: #9ca3af; white-space: nowrap; }
.match-score-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.match-score-label { font-size: 12px; color: #6b7280; white-space: nowrap; }
.match-score-num { font-size: 13px; font-weight: 700; white-space: nowrap; }
.match-reason { font-size: 13px; color: #374151; line-height: 1.6; margin: 0 0 6px; }
.match-snippet { font-size: 12px; color: #9ca3af; font-style: italic; margin-bottom: 8px; line-height: 1.5; }
.fit-tags { display: flex; flex-wrap: wrap; gap: 6px; }

/* 跟进 Timeline */
.timeline-content { display: flex; flex-direction: column; gap: 2px; }
.tl-label { font-size: 14px; color: #111827; font-weight: 600; }
.tl-meta { font-size: 12px; color: #9ca3af; }

/* 待确认 */
.confirm-list { margin: 0; padding-left: 18px; color: #78350f; font-size: 13px; line-height: 2; }

/* 数据源 */
.source-section { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; box-shadow: 0 4px 14px rgba(15,23,42,.05); }
.panel { background: #fff; }
h3 { margin: 0; font-size: 16px; font-weight: 700; color: #111827; }
p { margin: 4px 0 0; font-size: 13px; color: #64748b; }
.section-header { margin-bottom: 14px; }
.source-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.source-card { border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; background: #f9fafb; }
.source-card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 8px; }
.source-name { font-size: 13px; font-weight: 700; color: #111827; line-height: 1.3; }
.source-stats { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: #6b7280; }
.source-warn { margin-top: 6px; font-size: 12px; color: #d97706; background: #fffbeb; padding: 4px 8px; border-radius: 6px; }

@media (max-width: 1200px) {
  .metrics-row { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
  .source-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
