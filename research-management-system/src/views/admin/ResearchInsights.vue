<template>
  <div class="research-insights">
    <div class="page-header">
      <div>
        <h2>研究洞察</h2>
        <p class="subtitle">
          基于真实需求线索与开放论文样本，先输出管理层可读的方向结论，再用图谱辅助解释主题关系。
        </p>
      </div>
      <div class="header-actions">
        <el-tag effect="plain" type="warning">管理增值能力</el-tag>
        <el-button :loading="loading" @click="loadInsights">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :md="6" v-for="card in insights?.summaryCards || []" :key="card.key">
        <el-card shadow="never" class="summary-card">
          <div class="summary-label">{{ card.label }}</div>
          <div class="summary-value">{{ card.value }}</div>
          <div class="summary-note">{{ card.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="section-head">
          <div>
            <div class="section-title">方向摘要</div>
            <div class="section-desc">先给结论，不把词云或图谱当作最终价值。</div>
          </div>
          <div class="update-text">更新时间：{{ insights?.updatedAt || '—' }}</div>
        </div>
      </template>
      <el-skeleton :loading="loading" animated :rows="3">
        <template #default>
          <div class="brief-list">
            <div v-for="brief in insights?.insightBrief || []" :key="brief" class="brief-item">
              {{ brief }}
            </div>
          </div>
        </template>
      </el-skeleton>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="section-head">
              <div>
                <div class="section-title">热点主题</div>
                <div class="section-desc">结合外部真实线索与开放论文样本形成管理层阅读视图。</div>
              </div>
            </div>
          </template>
          <div class="topic-list">
            <div v-for="topic in insights?.hotTopics || []" :key="topic.topicName" class="topic-card">
              <div class="topic-header">
                <div>
                  <div class="topic-title">{{ topic.topicName }}</div>
                  <div class="topic-meta">
                    <span>外部线索 {{ topic.demandCount }}</span>
                    <span class="dot">·</span>
                    <span>论文样本 {{ topic.paperCount }}</span>
                  </div>
                </div>
                <div class="topic-score">
                  <div class="score-number">{{ topic.hotScore }}</div>
                  <div class="score-trend">+{{ topic.trendDelta }}</div>
                </div>
              </div>
              <p class="topic-copy">{{ topic.insightSummary }}</p>
              <div class="link-group">
                <el-link
                  v-for="link in topic.evidenceLinks"
                  :key="`${topic.topicName}-${link.url}`"
                  :href="link.url"
                  target="_blank"
                  type="primary"
                >
                  {{ link.label }}
                </el-link>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="section-head">
              <div>
                <div class="section-title">趋势变化</div>
                <div class="section-desc">适合现场解释“哪些方向正在升温”。</div>
              </div>
            </div>
          </template>
          <div class="trend-list">
            <div v-for="series in insights?.trendSeries || []" :key="series.topicName" class="trend-card">
              <div class="trend-title">{{ series.topicName }}</div>
              <div class="trend-points">
                <div v-for="(period, index) in series.periods" :key="`${series.topicName}-${period}`" class="trend-point">
                  <div class="point-label">{{ period }}</div>
                  <div class="point-bar">
                    <div class="point-fill" :style="{ width: `${series.values[index]}%` }"></div>
                  </div>
                  <div class="point-value">{{ series.values[index] }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="section-head">
          <div>
            <div class="section-title">供需对照</div>
            <div class="section-desc">把外部热度和当前成果样本基础放在一张表里，便于讨论后续投入方向。</div>
          </div>
        </div>
      </template>
      <el-table :data="insights?.supplyDemandComparison || []" border>
        <el-table-column prop="topicName" label="主题方向" min-width="240" />
        <el-table-column prop="externalHeat" label="外部热度" width="120" />
        <el-table-column prop="internalSupply" label="当前基础" width="120" />
        <el-table-column prop="gap" label="判断" min-width="180" />
        <el-table-column prop="action" label="建议动作" min-width="260" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="panel-card">
      <template #header>
        <div class="section-head">
          <div>
            <div class="section-title">主题关系图谱</div>
            <div class="section-desc">图谱是辅助展示形式，用于解释主题联动，不替代方向结论。</div>
          </div>
        </div>
      </template>
      <div ref="keywordChartRef" class="chart-container"></div>
      <el-empty v-if="!loading && !(insights?.keywordGraph?.nodes || []).length" description="暂无图谱数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { getResearchInsightsOverview } from '@/api/result'
import type { ResearchInsightOverview } from '@/types'
import { Refresh } from '@element-plus/icons-vue'
import type { EChartsType } from 'echarts/core'
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const loading = ref(false)
const insights = ref<ResearchInsightOverview | null>(null)
const keywordChartRef = ref<HTMLElement | null>(null)
const keywordChartInstance = ref<EChartsType | null>(null)
let echartsModule: typeof import('echarts/core') | null = null
let echartsReadyPromise: Promise<typeof import('echarts/core')> | null = null

const colorPalette = ['#0f766e', '#0284c7', '#1d4ed8', '#f97316', '#7c3aed', '#e11d48', '#16a34a']

onMounted(async () => {
  await loadInsights()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeChart()
})

async function ensureECharts() {
  if (echartsModule) return echartsModule
  if (!echartsReadyPromise) {
    echartsReadyPromise = (async () => {
      const echarts = await import('echarts/core')
      const [{ GraphChart }, { TooltipComponent, LegendComponent }, { CanvasRenderer }] = await Promise.all([
        import('echarts/charts'),
        import('echarts/components'),
        import('echarts/renderers')
      ])
      echarts.use([GraphChart, TooltipComponent, LegendComponent, CanvasRenderer])
      echartsModule = echarts
      return echarts
    })()
  }
  return echartsReadyPromise
}

async function loadInsights() {
  loading.value = true
  try {
    const res = await getResearchInsightsOverview()
    insights.value = res?.data || null
    await nextTick()
    await renderKeywordChart()
  } finally {
    loading.value = false
  }
}

async function renderKeywordChart() {
  if (!keywordChartRef.value || !(insights.value?.keywordGraph?.nodes || []).length) return
  if (!keywordChartInstance.value) {
    const echarts = await ensureECharts()
    keywordChartInstance.value = echarts.init(keywordChartRef.value)
  }

  const nodes = (insights.value?.keywordGraph.nodes || []).map((item, index) => ({
    ...item,
    symbolSize: Math.max(16, Math.min(42, item.value)),
    itemStyle: { color: colorPalette[index % colorPalette.length] }
  }))

  const categories = Array.from(new Set(nodes.map((item) => item.category).filter(Boolean))).map((name) => ({ name }))

  keywordChartInstance.value.setOption(
    {
      tooltip: {
        formatter: (params: any) => `${params.data.name}<br/>热度指数：${params.data.value}`
      },
      legend: {
        data: categories.map((item) => item.name),
        top: 8
      },
      series: [
        {
          type: 'graph',
          layout: 'force',
          roam: true,
          force: { repulsion: 120, edgeLength: [50, 140] },
          data: nodes,
          links: insights.value?.keywordGraph.links || [],
          categories,
          label: { show: true, formatter: '{b}', color: '#0f172a', fontSize: 12 },
          lineStyle: { color: '#cbd5e1', opacity: 0.7 },
          emphasis: { focus: 'adjacency', lineStyle: { width: 3 } }
        }
      ]
    },
    true
  )
}

function handleResize() {
  keywordChartInstance.value?.resize()
}

function disposeChart() {
  keywordChartInstance.value?.dispose()
  keywordChartInstance.value = null
}
</script>

<style scoped>
.research-insights {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.section-head,
.topic-header {
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
.topic-copy {
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
.panel-card {
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

.summary-note,
.update-text {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.brief-list,
.topic-list,
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brief-item,
.topic-card,
.trend-card {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.topic-title,
.trend-title {
  font-weight: 700;
  color: #0f172a;
}

.topic-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.topic-score {
  text-align: right;
}

.score-number {
  font-size: 28px;
  font-weight: 700;
  color: #0f766e;
}

.score-trend {
  color: #f97316;
  font-size: 13px;
  font-weight: 700;
}

.link-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trend-points {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
}

.trend-point {
  display: grid;
  grid-template-columns: 70px 1fr 42px;
  gap: 10px;
  align-items: center;
}

.point-label,
.point-value {
  font-size: 12px;
  color: #64748b;
}

.point-bar {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.point-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #0ea5e9, #0f766e);
}

.chart-container {
  width: 100%;
  height: 420px;
}

.dot {
  color: #94a3b8;
}

@media (max-width: 960px) {
  .page-header,
  .topic-header {
    flex-direction: column;
  }
}
</style>
