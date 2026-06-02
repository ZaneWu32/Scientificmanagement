<template>
  <div class="demand-insights">
    <!-- 顶部 -->
    <header class="page-header">
      <div>
        <div class="eyebrow">智能洞察 · 需求转化</div>
        <h2>需求洞察</h2>
        <p class="subtitle">采集入池 → 需求摘要 → 候选成果匹配 → 人工研判</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadDemands()">刷新</el-button>
        <el-tag type="success" effect="plain">成果匹配</el-tag>
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

    <!-- 智能匹配工作流流程图 -->
    <section class="workflow-collapse-panel">
      <div class="workflow-toggle-header" @click="showWorkflow = !showWorkflow">
        <span class="toggle-title">
          <el-icon style="vertical-align: middle; margin-right: 6px;"><Tickets /></el-icon>
          智能洞察与成果匹配流程图
        </span>
        <el-button size="small" link>
          {{ showWorkflow ? '折叠流程图' : '展开流程图' }}
          <el-icon class="toggle-arrow" :class="{ 'is-active': showWorkflow }"><ArrowDown /></el-icon>
        </el-button>
      </div>

      <el-collapse-transition>
        <div v-show="showWorkflow" class="workflow-container">
          <!-- 步骤连接 SVG (底层虚线流光) -->
          <svg class="workflow-svg-connectors" viewBox="0 0 1000 100" preserveAspectRatio="none">
            <path class="flow-line" d="M 100 50 L 300 50" />
            <path class="flow-line" d="M 500 50 L 700 50" />
            <path class="flow-line" d="M 700 50 L 900 50" />
            <path class="flow-line-fork" d="M 300 50 Q 500 15 700 50" />
          </svg>

          <!-- 步骤节点卡片 -->
          <div class="workflow-steps">
            <!-- 步骤 1 -->
            <div class="workflow-step-card step-1">
              <div class="step-badge">01</div>
              <div class="step-node-title">成果物多模态解析</div>
              <div class="step-desc">
                <div class="tech-tags">
                  <span class="tech-tag">Apache Tika</span>
                  <span class="tech-tag">Tesseract OCR</span>
                  <span class="tech-tag">LLM Extraction</span>
                </div>
                <div class="desc-text">提取非结构化附件（PDF/DOCX）文本，经由 <strong>大语言模型</strong> 智能提取基础字段与自定义属性，降低人工录入成本。</div>
              </div>
            </div>
            
            <!-- 步骤 2 -->
            <div class="workflow-step-card step-2">
              <div class="step-badge">02</div>
              <div class="step-node-title">检索文档构建与索引</div>
              <div class="step-desc">
                <div class="tech-tags">
                  <span class="tech-tag">MyBatis-Plus</span>
                  <span class="tech-tag">IK Analyzer</span>
                  <span class="tech-tag">Elasticsearch</span>
                </div>
                <div class="desc-text">审核通过后，重构为本地检索文档 <code>search_doc</code> 并异步同步分词文本至 <strong>Elasticsearch</strong>，以支撑多维文本关联检索。</div>
              </div>
            </div>

            <!-- 步骤 3 -->
            <div class="workflow-step-card step-3">
              <div class="step-badge">03</div>
              <div class="step-node-title">需求采集与治理入池</div>
              <div class="step-desc">
                <div class="tech-tags">
                  <span class="tech-tag">Scheduled Cron</span>
                  <span class="tech-tag">MD5 Deduplicate</span>
                  <span class="tech-tag">Data Cleaning</span>
                </div>
                <div class="desc-text">爬虫轮询拉取政策、项目指南与招标公告，完成内容 MD5 去重与格式清洗后归一化录入线索表 <code>demand_items</code>。</div>
              </div>
            </div>

            <!-- 步骤 4 -->
            <div class="workflow-step-card step-4">
              <div class="step-badge">04</div>
              <div class="step-node-title">混合检索与 LLM 重排</div>
              <div class="step-desc">
                <div class="tech-tags">
                  <span class="tech-tag">ES BM25</span>
                  <span class="tech-tag">Rule Scoring</span>
                  <span class="tech-tag">DeepSeek Rerank</span>
                </div>
                <div class="desc-text">拼装需求 Query 并通过 <strong>ES</strong> 召回，计算业务规则重叠分，级联调用 <strong>DeepSeek</strong> 生成匹配置信度与匹配缘由。</div>
              </div>
            </div>

            <!-- 步骤 5 -->
            <div class="workflow-step-card step-5">
              <div class="step-badge">05</div>
              <div class="step-node-title">人工研判与闭环轨迹</div>
              <div class="step-desc">
                <div class="tech-tags">
                  <span class="tech-tag">Keycloak Audit</span>
                  <span class="tech-tag">Local DOM Patch</span>
                  <span class="tech-tag">Action Timeline</span>
                </div>
                <div class="desc-text">研判员一键确认匹配或排除拦截，操作过程作为日志轨迹持久化留痕，用以反哺匹配模型做自适应优化。</div>
              </div>
            </div>
          </div>
        </div>
      </el-collapse-transition>
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

        <!-- 分页组件 -->
        <div class="list-pagination" style="margin-top: 10px; display: flex; justify-content: center;">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="prev, pager, next"
            small
            background
            @current-change="handlePageChange"
          />
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
            <span>地域：{{ selectedDemand.region }}</span>
            <span>行业：{{ selectedDemand.industry }}</span>
            <span>来源：{{ selectedDemand.sourceSite }}</span>
          </div>

          <div class="detail-section status-section">
            <div class="section-label">研判状态</div>
            <div class="status-editor">
              <el-select v-model="statusForm.status" size="small" style="width: 132px">
                <el-option v-for="o in statusActionOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-input
                v-model="statusForm.note"
                type="textarea"
                :rows="2"
                maxlength="120"
                show-word-limit
                placeholder="记录本次研判或跟进说明"
              />
              <el-button type="primary" size="small" :loading="statusSaving" @click="handleStatusUpdate">
                保存
              </el-button>
            </div>
          </div>

          <!-- 摘要 -->
          <div class="detail-section">
            <div class="section-label">需求摘要</div>
            <p class="detail-summary">{{ selectedDemand.llmSummary }}</p>
            <div class="keyword-row">
              <el-tag v-for="kw in selectedDemand.keywords" :key="kw" size="small" effect="plain" round>{{ kw }}</el-tag>
            </div>
          </div>

          <!-- 候选成果 -->
          <div class="detail-section">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
              <div class="section-label" style="margin-bottom: 0;">候选成果匹配</div>
              <div v-if="selectedDemand.matches.some(m => m.matchScore < 0.5)" class="score-filter-toggle">
                <el-checkbox v-model="filterLowScore" size="small">仅看高匹配 (≥50%)</el-checkbox>
              </div>
            </div>

            <el-empty v-if="!visibleMatches.length" description="暂无符合筛选的候选成果" :image-size="56" />
            <div
              v-for="m in visibleMatches"
              :key="m.resultId"
              class="match-card"
              :class="{ 'is-rejected': m.confirmStatus === 'rejected' }"
            >
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
              <div class="match-actions">
                <el-tag v-if="m.confirmStatus === 'confirmed'" type="success" size="small" effect="plain">
                  已确认
                </el-tag>
                <el-tag v-else-if="m.confirmStatus === 'rejected'" type="danger" size="small" effect="plain">
                  已排除
                </el-tag>
                <template v-else>
                  <el-button
                    type="primary"
                    size="small"
                    plain
                    :icon="CircleCheck"
                    :loading="confirmingMatchKey === matchKey(m)"
                    @click.stop="handleConfirmMatch(m)"
                  >
                    确认匹配
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    plain
                    :icon="CircleClose"
                    :loading="rejectingMatchKey === matchKey(m)"
                    @click.stop="handleRejectMatch(m)"
                  >
                    排除
                  </el-button>
                </template>
                <el-button size="small" link @click.stop="openResult(m.resultId)">查看成果</el-button>
              </div>
            </div>

            <!-- 折叠提示 -->
            <div v-if="filterLowScore && hiddenMatchesCount > 0" class="collapsed-tip" style="text-align: center; padding: 10px; background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; margin-top: 10px;">
              <span style="font-size: 12px; color: #64748b; margin-right: 8px;">已折叠 {{ hiddenMatchesCount }} 个低匹配度成果 (低于 50%)</span>
              <el-button type="primary" link size="small" @click="filterLowScore = false">展开全部</el-button>
            </div>
          </div>

          <!-- 跟进步骤 -->
          <div class="detail-section" v-if="selectedDemand.followUp?.length">
            <div class="section-label">跟进记录</div>
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
        </div>
      </aside>
      <div v-else class="detail-empty">
        <el-empty description="点击左侧需求卡片查看详情" :image-size="80" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Search, CircleCheck, CircleClose, Promotion, Refresh, Tickets, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  confirmMatch as confirmDemandMatch,
  getDemands,
  rematchDemand,
  updateDemandStatus,
  getDemandDetail,
  rejectMatch as rejectDemandMatch,
  getDemandStats
} from '@/api/demand'
import { useRouter } from 'vue-router'

type DemandStatus = 'new' | 'reviewing' | 'matched' | 'in_follow_up' | 'invalid' | 'archived'

interface DemandMatchItem {
  matchId?: string
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
  confirmStatus?: 'pending' | 'confirmed' | 'rejected'
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
  matches: DemandMatchItem[]
  followUp: DemandFollowUpItem[]
}

const demands = ref<DemandItem[]>([])
const selectedDemand = ref<DemandItem | null>(null)
const loading = ref(false)
const rematching = ref(false)
const statusSaving = ref(false)
const confirmingMatchKey = ref('')
const rejectingMatchKey = ref('')
const filters = reactive({ keyword: '', status: '' })
const router = useRouter()
const statusForm = reactive<{ status: DemandStatus; note: string }>({ status: 'new', note: '' })

// 服务端分页状态
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 全局指标统计数据
const stats = ref({
  totalDemands: 0,
  matchedDemands: 0,
  followUpDemands: 0
})

// 折叠低评分成果控制
const filterLowScore = ref(true)

// 控制工作流流程图展开/折叠
const showWorkflow = ref(true)

const statusOptions = [
  { label: '新入池', value: 'new' },
  { label: '研判中', value: 'reviewing' },
  { label: '已匹配', value: 'matched' },
  { label: '跟进中', value: 'in_follow_up' }
]
const statusActionOptions = [
  ...statusOptions,
  { label: '无效', value: 'invalid' },
  { label: '已归档', value: 'archived' }
] as Array<{ label: string; value: DemandStatus }>

const filteredDemands = computed(() => {
  return demands.value
})

const visibleMatches = computed(() => {
  if (!selectedDemand.value) return []
  const matches = selectedDemand.value.matches || []
  if (filterLowScore.value) {
    return matches.filter(m => m.matchScore >= 0.5)
  }
  return matches
})

const hiddenMatchesCount = computed(() => {
  if (!selectedDemand.value) return 0
  const matches = selectedDemand.value.matches || []
  return matches.filter(m => m.matchScore < 0.5).length
})

const metrics = computed(() => {
  return [
    { label: '入池需求', value: stats.value.totalDemands, badge: '去重后', badgeClass: 'badge-blue', icon: Tickets, iconBg: '#eff6ff', iconColor: '#2563eb' },
    { label: '有候选成果', value: stats.value.matchedDemands, badge: '待研判', badgeClass: 'badge-green', icon: CircleCheck, iconBg: '#f0fdf4', iconColor: '#16a34a' },
    { label: '跟进中', value: stats.value.followUpDemands, badge: '人工确认后', badgeClass: 'badge-amber', icon: Promotion, iconBg: '#fffbeb', iconColor: '#d97706' }
  ]
})

function selectDemand(row: DemandItem) {
  selectedDemand.value = row
  syncStatusForm(row)
}
function toPercent(v: number) { return Math.round(v * 100) }

// 防抖关键字检索与状态检索监听
let debounceTimer: any = null
watch(() => filters.keyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    pagination.pageNum = 1
    loadDemands()
  }, 300)
})

watch(() => filters.status, () => {
  pagination.pageNum = 1
  loadDemands()
})

function handlePageChange(val: number) {
  pagination.pageNum = val
  loadDemands()
}

// 加载全局指标统计
async function loadStats() {
  try {
    const statsRes = await getDemandStats()
    if (statsRes?.data) {
      stats.value = statsRes.data
    }
  } catch (err) {
    console.error('加载统计数据失败', err)
  }
}

// 局部微更新数据行，防止左侧滚动条重置闪烁
async function refreshSingleDemand(demandId: string) {
  try {
    const res = await getDemandDetail(demandId)
    if (res?.data) {
      const updatedDemand = normalizeDemands([res.data])[0]
      const idx = demands.value.findIndex(d => String(d.id) === demandId)
      if (idx !== -1) {
        demands.value[idx] = updatedDemand
      }
      if (selectedDemand.value && String(selectedDemand.value.id) === demandId) {
        selectedDemand.value = updatedDemand
      }
    }
  } catch (err) {
    console.error('局部刷新需求数据失败', err)
  }
}

async function loadDemands(keepId?: string) {
  loading.value = true
  try {
    const demandRes = await getDemands({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined
    })
    const records = demandRes?.data?.records || []
    pagination.total = demandRes?.data?.total || 0
    demands.value = normalizeDemands(records)
    selectedDemand.value = demands.value.find(item => String(item.id) === keepId) || demands.value[0] || null
    syncStatusForm(selectedDemand.value)
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
    await refreshSingleDemand(demandId)
    await loadStats()
    ElMessage.success('已重新计算候选成果匹配')
  } finally {
    rematching.value = false
  }
}

async function handleStatusUpdate() {
  if (!selectedDemand.value) return
  statusSaving.value = true
  const demandId = String(selectedDemand.value.id)
  try {
    await updateDemandStatus(demandId, statusForm.status, statusForm.note.trim() || undefined)
    await refreshSingleDemand(demandId)
    await loadStats()
    ElMessage.success('状态已更新')
  } finally {
    statusSaving.value = false
  }
}

async function handleConfirmMatch(match: DemandMatchItem) {
  if (!selectedDemand.value) return
  const demandId = String(selectedDemand.value.id)
  const resultId = match.matchId || match.resultId
  const key = matchKey(match)
  confirmingMatchKey.value = key
  try {
    await confirmDemandMatch(demandId, resultId)
    await refreshSingleDemand(demandId)
    await loadStats()
    ElMessage.success('已确认候选成果')
  } finally {
    confirmingMatchKey.value = ''
  }
}

async function handleRejectMatch(match: DemandMatchItem) {
  if (!selectedDemand.value) return
  const demandId = String(selectedDemand.value.id)
  const resultId = match.matchId || match.resultId
  const key = matchKey(match)
  rejectingMatchKey.value = key
  try {
    await rejectDemandMatch(demandId, resultId)
    await refreshSingleDemand(demandId)
    await loadStats()
    ElMessage.success('已排除该候选成果')
  } finally {
    rejectingMatchKey.value = ''
  }
}

function openResult(resultId: string) {
  if (!resultId) return
  const routeData = router.resolve(`/results/${resultId}`)
  window.open(routeData.href, '_blank')
}

function matchKey(match: DemandMatchItem) {
  return match.matchId || match.resultId || match.resultTitle
}

function syncStatusForm(row: DemandItem | null) {
  statusForm.status = row?.status || 'new'
  statusForm.note = ''
}

function normalizeDemands(rows: any[]): DemandItem[] {
  return rows.map(row => ({
    ...row,
    id: String(row.id),
    priority: row.priority || '中',
    confidence: Number(row.confidence || 0),
    bestMatchScore: Number(row.bestMatchScore || 0),
    llmSummary: row.llmSummary || row.summary || '暂无摘要',
    keywords: row.keywords || [],
    tags: row.tags || [],
    matches: (row.matches || []).map((m: any) => ({
      ...m,
      matchScore: Number(m.matchScore || 0),
      fitTags: m.fitTags || [],
      confirmStatus: m.confirmStatus || 'pending',
      updatedAt: m.updatedAt || ''
    })),
    followUp: row.followUp || []
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
function timelineType(status: string) {
  if (status === 'matched' || status === 'in_follow_up') return 'success'
  if (status === 'reviewing') return 'primary'
  if (status === 'invalid') return 'danger'
  return 'info'
}

onMounted(() => {
  loadDemands()
  loadStats()
})
</script>

<style scoped>
.demand-insights { display: flex; flex-direction: column; gap: 16px; }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.eyebrow { font-size: 12px; font-weight: 700; color: #2563eb; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 4px; }
h2 { margin: 0; font-size: 24px; font-weight: 800; color: #0f172a; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: #64748b; }

/* 指标卡 */
.metrics-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
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
.status-section { margin-top: 12px; }
.section-label { font-size: 12px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 10px; }
.detail-summary { font-size: 14px; color: #374151; line-height: 1.7; margin: 0 0 10px; }
.keyword-row { display: flex; flex-wrap: wrap; gap: 6px; }
.status-editor { display: grid; grid-template-columns: 132px minmax(0, 1fr) auto; gap: 8px; align-items: flex-start; }

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
.match-actions { display: flex; align-items: center; gap: 8px; margin-top: 10px; }

/* 跟进 Timeline */
.timeline-content { display: flex; flex-direction: column; gap: 2px; }
.tl-label { font-size: 14px; color: #111827; font-weight: 600; }
.tl-meta { font-size: 12px; color: #9ca3af; }

/* 排除的候选成果样式 */
.match-card.is-rejected {
  opacity: 0.6;
  filter: grayscale(40%);
  border-style: dashed;
  background: #f1f5f9;
}

/* 流程图折叠面板 */
.workflow-collapse-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
  overflow: hidden;
  margin-top: 10px;
}

.workflow-toggle-header {
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  user-select: none;
  transition: background 0.2s;
}
.workflow-toggle-header:hover {
  background: #f1f5f9;
}
.toggle-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
}
.toggle-arrow {
  transition: transform 0.3s;
  margin-left: 4px;
}
.toggle-arrow.is-active {
  transform: rotate(180deg);
}

.workflow-container {
  padding: 24px 20px;
  position: relative;
  background: linear-gradient(135deg, #f8fafc 0%, #eff6ff 100%);
  overflow: hidden;
}

/* SVG 连线样式与粒子流光效果 */
.workflow-svg-connectors {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
.flow-line, .flow-line-fork {
  fill: none;
  stroke: #cbd5e1;
  stroke-width: 2.5;
  stroke-dasharray: 6 6;
  animation: flowParticle 25s linear infinite;
}
.flow-line {
  stroke: #93c5fd;
}
.flow-line-fork {
  stroke: #c084fc;
}

@keyframes flowParticle {
  from {
    stroke-dashoffset: 500;
  }
  to {
    stroke-dashoffset: 0;
  }
}

/* 步骤节点布局 */
.workflow-steps {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  position: relative;
  z-index: 2;
}

/* 步骤卡片设计：磨砂玻璃 (Glassmorphism) */
.workflow-step-card {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(226, 232, 240, 0.8);
  border-radius: 12px;
  padding: 16px;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 10px rgba(15, 23, 242, 0.01);
}

/* 卡片 Hover 状态 */
.workflow-step-card:hover {
  transform: translateY(-5px);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 10px 25px rgba(37, 99, 235, 0.1);
  border-color: #2563eb;
}

/* 步骤数字角标 */
.step-badge {
  position: absolute;
  top: -10px;
  left: 16px;
  background: #2563eb;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 999px;
  box-shadow: 0 4px 8px rgba(37, 99, 235, 0.2);
}

.step-2 .step-badge { background: #3b82f6; box-shadow: 0 4px 8px rgba(59, 130, 246, 0.2); }
.step-3 .step-badge { background: #10b981; box-shadow: 0 4px 8px rgba(16, 185, 129, 0.2); }
.step-4 .step-badge { background: #8b5cf6; box-shadow: 0 4px 8px rgba(139, 92, 246, 0.2); }
.step-5 .step-badge { background: #f59e0b; box-shadow: 0 4px 8px rgba(245, 158, 11, 0.2); }

/* 技术栈标签组件 */
.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}
.tech-tag {
  background: rgba(37, 99, 235, 0.06);
  color: #2563eb;
  font-size: 9px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
  border: 0.5px solid rgba(37, 99, 235, 0.15);
  white-space: nowrap;
}

.step-2 .tech-tag {
  background: rgba(59, 130, 246, 0.06);
  color: #3b82f6;
  border-color: rgba(59, 130, 246, 0.15);
}
.step-3 .tech-tag {
  background: rgba(16, 185, 129, 0.06);
  color: #10b981;
  border-color: rgba(16, 185, 129, 0.15);
}
.step-4 .tech-tag {
  background: rgba(139, 92, 246, 0.06);
  color: #8b5cf6;
  border-color: rgba(139, 92, 246, 0.15);
}
.step-5 .tech-tag {
  background: rgba(245, 158, 11, 0.06);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.15);
}

.step-node-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 4px;
  margin-bottom: 8px;
  border-bottom: 1.5px solid #f1f5f9;
  padding-bottom: 6px;
}
.workflow-step-card:hover .step-node-title {
  color: #2563eb;
}

.step-desc {
  font-size: 11px;
  color: #64748b;
  line-height: 1.6;
}
.desc-text {
  font-size: 10px;
  color: #64748b;
  line-height: 1.5;
}
.desc-text strong {
  color: #334155;
}
.step-desc code {
  background: #f1f5f9;
  padding: 1px 4px;
  border-radius: 4px;
  color: #0f172a;
}

@media (max-width: 1200px) {
  .metrics-row { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
  .status-editor { grid-template-columns: 1fr; }
}
</style>
