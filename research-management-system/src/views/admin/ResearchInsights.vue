<template>
  <div class="research-insights">
    <!-- 顶部 -->
    <header class="page-header">
      <div>
        <div class="eyebrow">智能洞察 · 管理增值</div>
        <h2>研究洞察</h2>
        <p class="subtitle">基于内部成果与外部需求信号，形成热点主题、趋势变化和方向摘要，辅助管理层研判。</p>
      </div>
      <el-tag type="warning" effect="plain">Mock 演示数据</el-tag>
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
      </div>
    </section>

    <!-- 管理层辅助摘要 -->
    <section class="panel">
      <div class="section-header">
        <h3>管理层辅助摘要</h3>
        <p>以下内容用于辅助方向研判，不替代立项或资源配置决策。</p>
      </div>
      <div class="exec-grid">
        <article v-for="item in executiveInsights" :key="item.title" class="exec-card" :class="`exec-${insightKey(item.level)}`">
          <div class="exec-level-bar"></div>
          <div class="exec-body">
            <el-tag :type="insightType(item.level)" effect="plain" size="small" class="exec-tag">{{ item.level }}</el-tag>
            <h4>{{ item.title }}</h4>
            <p class="exec-desc">{{ item.description }}</p>
            <div class="exec-evidence">{{ item.evidence }}</div>
            <div class="exec-action">→ {{ item.action }}</div>
          </div>
        </article>
      </div>
    </section>

    <!-- 主体：主题池 + 详情 -->
    <section class="main-grid">
      <!-- 左：主题池列表 -->
      <div class="topic-list-col panel">
        <div class="section-header list-header">
          <div>
            <h3>主题池</h3>
            <p>点击主题查看数据依据和建议动作。</p>
          </div>
          <el-select v-model="filters.stage" placeholder="全部类型" clearable size="small" style="width:130px">
            <el-option label="新兴主题" value="emerging" />
            <el-option label="强势方向" value="strong" />
            <el-option label="交叉方向" value="cross" />
            <el-option label="高潜缺口" value="gap" />
          </el-select>
        </div>

        <div class="topic-cards">
          <div
            v-for="topic in filteredTopics"
            :key="topic.id"
            class="topic-card"
            :class="{ 'is-active': selectedTopic?.id === topic.id }"
            @click="selectTopic(topic)"
          >
            <div class="topic-card-top">
              <span class="topic-name">{{ topic.name }}</span>
              <el-tag :type="stageType(topic.stage)" effect="plain" size="small">{{ topic.opportunityType }}</el-tag>
            </div>
            <div class="topic-kws">{{ topic.keywords.slice(0,3).join(' · ') }}</div>
            <!-- 迷你趋势线 -->
            <svg class="mini-trend" viewBox="0 0 120 32" preserveAspectRatio="none">
              <polyline
                :points="miniTrendPoints(topic.trend)"
                fill="none"
                :stroke="stageColor(topic.stage)"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <div class="topic-scores">
              <span class="score-item heat">外部热度 <strong>{{ topic.externalHeat }}</strong></span>
              <span class="score-item strength">内部基础 <strong>{{ topic.internalStrength }}</strong></span>
              <span class="score-item growth">增速 <strong>+{{ topic.growthRate12m }}%</strong></span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右：主题详情 -->
      <aside v-if="selectedTopic" class="topic-detail panel">
        <div class="detail-head">
          <div>
            <h3>{{ selectedTopic.name }}</h3>
            <p>{{ selectedTopic.description }}</p>
          </div>
          <el-tag :type="stageType(selectedTopic.stage)" effect="plain">{{ selectedTopic.opportunityType }}</el-tag>
        </div>

        <!-- 供需对照 -->
        <div class="compare-block">
          <div class="compare-item">
            <div class="compare-label">外部热度</div>
            <el-progress :percentage="selectedTopic.externalHeat" status="" :stroke-width="10"
              color="#ef4444" :show-text="false" />
            <div class="compare-num heat-num">{{ selectedTopic.externalHeat }}</div>
          </div>
          <div class="compare-item">
            <div class="compare-label">内部基础</div>
            <el-progress :percentage="selectedTopic.internalStrength" :stroke-width="10"
              color="#2563eb" :show-text="false" />
            <div class="compare-num strength-num">{{ selectedTopic.internalStrength }}</div>
          </div>
        </div>
        <div class="growth-badge">近 12 个月增速 <strong>+{{ selectedTopic.growthRate12m }}%</strong></div>

        <!-- 趋势折线图（SVG） -->
        <div class="trend-chart-container">
          <div class="trend-label-row">
            <span class="trend-title">近 12 个月趋势</span>
            <span class="trend-months">1月 → 12月</span>
          </div>
          <svg class="trend-chart" viewBox="0 0 360 80" preserveAspectRatio="none">
            <!-- 网格线 -->
            <line x1="0" y1="20" x2="360" y2="20" stroke="#f1f5f9" stroke-width="1" />
            <line x1="0" y1="40" x2="360" y2="40" stroke="#f1f5f9" stroke-width="1" />
            <line x1="0" y1="60" x2="360" y2="60" stroke="#f1f5f9" stroke-width="1" />
            <!-- 填充区 -->
            <path :d="trendFillPath(selectedTopic.trend)" :fill="stageColor(selectedTopic.stage) + '22'" />
            <!-- 折线 -->
            <polyline
              :points="trendPoints(selectedTopic.trend)"
              fill="none"
              :stroke="stageColor(selectedTopic.stage)"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <!-- 数据点 -->
            <circle
              v-for="(pt, i) in trendPointList(selectedTopic.trend)"
              :key="i"
              :cx="pt.x"
              :cy="pt.y"
              r="3"
              :fill="stageColor(selectedTopic.stage)"
            />
          </svg>
        </div>

        <!-- 建议动作 -->
        <div class="detail-section">
          <div class="section-label">💡 建议动作</div>
          <p class="recommendation">{{ selectedTopic.recommendation }}</p>
        </div>
        <div class="detail-section">
          <div class="section-label">📊 解释依据</div>
          <p class="explanation">{{ selectedTopic.explanation }}</p>
        </div>

        <!-- 内部成果 vs 外部信号 -->
        <div class="evidence-grid">
          <div class="evidence-col">
            <div class="section-label">🔬 内部代表成果</div>
            <ul class="evidence-list">
              <li v-for="r in selectedTopic.internalResults" :key="r">{{ r }}</li>
            </ul>
          </div>
          <div class="evidence-col">
            <div class="section-label">📡 外部需求信号</div>
            <ul class="evidence-list external">
              <li v-for="s in selectedTopic.externalSignals" :key="s">{{ s }}</li>
            </ul>
          </div>
        </div>

        <!-- 参与部门 -->
        <div class="detail-section">
          <div class="section-label">🏛 参与部门</div>
          <div class="dept-tags">
            <el-tag v-for="d in selectedTopic.departments" :key="d" effect="plain" size="small">{{ d }}</el-tag>
          </div>
        </div>
      </aside>
      <div v-else class="detail-empty panel">
        <el-empty description="点击左侧主题查看详情" :image-size="80" />
      </div>
    </section>

    <!-- 底部：能力边界说明 -->
    <section class="panel boundary-section">
      <div class="section-header">
        <h3>交付说明与能力边界</h3>
        <p>与甲方对齐口径，避免将辅助能力理解为自动决策。</p>
      </div>
      <div class="boundary-grid">
        <div v-for="item in aiAlignmentCards" :key="item.name" class="boundary-card">
          <div class="boundary-name">{{ item.name }}</div>
          <div class="boundary-role">{{ item.role }}</div>
          <div class="boundary-output">输出：{{ item.output }}</div>
          <div class="boundary-limit">边界：{{ item.boundary }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { CircleCheck, DataAnalysis, TrendCharts, Warning } from '@element-plus/icons-vue'
import { computed, reactive, ref } from 'vue'
import {
  aiAlignmentCards,
  executiveInsights,
  researchTopics,
  type ResearchTopicMock,
  type TopicStage
} from '@/mocks/insightShowcase'

const selectedTopic = ref<ResearchTopicMock | null>(researchTopics[0] || null)
const filters = reactive({ stage: '' })

const filteredTopics = computed(() =>
  researchTopics.filter(t => !filters.stage || t.stage === filters.stage)
)

const metrics = computed(() => {
  const emerging = researchTopics.filter(t => t.stage === 'emerging').length
  const strong = researchTopics.filter(t => t.stage === 'strong').length
  const gap = researchTopics.filter(t => t.stage === 'gap').length
  const depts = new Set(researchTopics.flatMap(t => t.departments)).size
  return [
    { label: '新兴主题', value: emerging, icon: 'Warning', iconBg: '#fef2f2', iconColor: '#ef4444' },
    { label: '强势方向', value: strong, icon: 'TrendCharts', iconBg: '#f0fdf4', iconColor: '#16a34a' },
    { label: '机会缺口', value: gap, icon: 'DataAnalysis', iconBg: '#fffbeb', iconColor: '#d97706' },
    { label: '参与部门', value: depts, icon: 'CircleCheck', iconBg: '#eff6ff', iconColor: '#2563eb' }
  ]
})

function selectTopic(t: ResearchTopicMock) { selectedTopic.value = t }

// SVG 迷你趋势（120×32）
function miniTrendPoints(trend: number[]): string {
  if (!trend?.length) return ''
  const min = Math.min(...trend), max = Math.max(...trend)
  const range = max - min || 1
  const W = 120, H = 28, PAD = 2
  return trend.map((v, i) => {
    const x = PAD + (i / (trend.length - 1)) * (W - PAD * 2)
    const y = H - PAD - ((v - min) / range) * (H - PAD * 2)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}

// SVG 大趋势（360×80）
function trendPointList(trend: number[]) {
  if (!trend?.length) return []
  const min = Math.min(...trend), max = Math.max(...trend)
  const range = max - min || 1
  const W = 360, H = 72, PAD = 4
  return trend.map((v, i) => ({
    x: PAD + (i / (trend.length - 1)) * (W - PAD * 2),
    y: H - PAD - ((v - min) / range) * (H - PAD * 2)
  }))
}

function trendPoints(trend: number[]): string {
  return trendPointList(trend).map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
}

function trendFillPath(trend: number[]): string {
  const pts = trendPointList(trend)
  if (!pts.length) return ''
  const W = 360, H = 76
  const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
  return `${line} L${pts[pts.length - 1].x.toFixed(1)},${H} L${pts[0].x.toFixed(1)},${H} Z`
}

function stageColor(stage: TopicStage) {
  const m: Record<TopicStage, string> = { strong: '#16a34a', emerging: '#ef4444', cross: '#2563eb', gap: '#d97706' }
  return m[stage]
}

function stageType(stage: TopicStage) {
  const m: Record<TopicStage, any> = { emerging: 'danger', strong: 'success', cross: 'primary', gap: 'warning' }
  return m[stage]
}

function insightType(level: string) {
  if (level === '重点关注') return 'danger'
  if (level === '持续投入') return 'success'
  if (level === '组织协同') return 'primary'
  return 'warning'
}

function insightKey(level: string) {
  const m: Record<string, string> = { '重点关注': 'danger', '持续投入': 'success', '组织协同': 'primary', '补齐短板': 'warning' }
  return m[level] || 'info'
}
</script>

<style scoped>
.research-insights { display: flex; flex-direction: column; gap: 16px; }

.page-header { display: flex; align-items: flex-start; justify-content: space-between; }
.eyebrow { font-size: 12px; font-weight: 700; color: #7c3aed; letter-spacing: 0.8px; margin-bottom: 4px; }
h2 { margin: 0; font-size: 24px; font-weight: 800; color: #0f172a; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: #64748b; }
h3 { margin: 0; font-size: 15px; font-weight: 700; color: #111827; }
h4 { margin: 8px 0 6px; font-size: 14px; font-weight: 700; color: #111827; line-height: 1.4; }
p { margin: 0; }

.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 20px; box-shadow: 0 4px 14px rgba(15,23,42,.05); }

/* 指标卡 */
.metrics-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.metric-card { background: #fff; border: 1px solid #e5e7eb; border-radius: 12px; padding: 16px; display: flex; align-items: center; gap: 14px; box-shadow: 0 4px 14px rgba(15,23,42,.05); }
.metric-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.metric-body { flex: 1; }
.metric-value { font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1; }
.metric-label { font-size: 12px; color: #64748b; margin-top: 3px; }

/* 执行摘要 */
.section-header { margin-bottom: 14px; }
.section-header p { font-size: 13px; color: #64748b; margin-top: 3px; }
.exec-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.exec-card { display: flex; border-radius: 10px; overflow: hidden; border: 1px solid #e5e7eb; background: #f9fafb; }
.exec-level-bar { width: 5px; flex-shrink: 0; }
.exec-danger .exec-level-bar { background: #ef4444; }
.exec-success .exec-level-bar { background: #22c55e; }
.exec-primary .exec-level-bar { background: #3b82f6; }
.exec-warning .exec-level-bar { background: #f59e0b; }
.exec-body { padding: 14px; flex: 1; }
.exec-tag { margin-bottom: 6px; }
.exec-desc { font-size: 13px; color: #374151; line-height: 1.6; margin: 0 0 8px; }
.exec-evidence { font-size: 12px; color: #6b7280; margin-bottom: 6px; font-style: italic; }
.exec-action { font-size: 13px; color: #1d4ed8; font-weight: 600; }

/* 主体双栏 */
.main-grid { display: grid; grid-template-columns: 360px 1fr; gap: 16px; }
.topic-list-col { display: flex; flex-direction: column; }
.list-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.topic-cards { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; overflow-y: auto; max-height: 560px; }

/* 主题卡 */
.topic-card { border: 1.5px solid #e5e7eb; border-radius: 10px; padding: 14px; cursor: pointer; transition: all 0.2s; background: #fff; }
.topic-card:hover { border-color: #a5b4fc; box-shadow: 0 4px 14px rgba(124,58,237,.1); }
.topic-card.is-active { border-color: #7c3aed; background: #f5f3ff; box-shadow: 0 4px 14px rgba(124,58,237,.15); }
.topic-card-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 6px; }
.topic-name { font-size: 14px; font-weight: 700; color: #111827; flex: 1; }
.topic-kws { font-size: 12px; color: #9ca3af; margin-bottom: 8px; }
.mini-trend { width: 100%; height: 32px; display: block; margin-bottom: 8px; }
.topic-scores { display: flex; gap: 12px; flex-wrap: wrap; }
.score-item { font-size: 12px; color: #6b7280; }
.score-item strong { font-weight: 700; }
.heat strong { color: #ef4444; }
.strength strong { color: #2563eb; }
.growth strong { color: #16a34a; }

/* 详情面板 */
.topic-detail { overflow-y: auto; max-height: 680px; }
.detail-empty { display: flex; align-items: center; justify-content: center; min-height: 300px; }
.detail-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.detail-head p { font-size: 13px; color: #6b7280; margin-top: 4px; }

/* 供需对照 */
.compare-block { display: flex; flex-direction: column; gap: 10px; margin-bottom: 10px; }
.compare-item { display: flex; align-items: center; gap: 10px; }
.compare-label { font-size: 12px; color: #6b7280; width: 60px; flex-shrink: 0; }
.compare-num { font-size: 14px; font-weight: 700; width: 30px; text-align: right; }
.heat-num { color: #ef4444; }
.strength-num { color: #2563eb; }
.growth-badge { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; color: #6b7280; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 99px; padding: 3px 12px; margin-bottom: 14px; }
.growth-badge strong { color: #16a34a; }

/* 趋势图 */
.trend-chart-container { background: #f8fafc; border-radius: 10px; padding: 12px; margin-bottom: 2px; }
.trend-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.trend-title { font-size: 12px; font-weight: 700; color: #374151; }
.trend-months { font-size: 11px; color: #9ca3af; }
.trend-chart { width: 100%; height: 80px; display: block; }

/* 详情区块 */
.detail-section { margin-top: 16px; padding-top: 14px; border-top: 1px solid #f1f5f9; }
.section-label { font-size: 11px; font-weight: 700; color: #6b7280; text-transform: uppercase; letter-spacing: 0.6px; margin-bottom: 8px; }
.recommendation { font-size: 14px; color: #1d4ed8; font-weight: 600; line-height: 1.6; margin: 0; }
.explanation { font-size: 13px; color: #374151; line-height: 1.7; margin: 0; }

/* 证据双栏 */
.evidence-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 16px; padding-top: 14px; border-top: 1px solid #f1f5f9; }
.evidence-col { }
.evidence-list { margin: 0; padding-left: 16px; font-size: 13px; color: #374151; line-height: 1.9; }
.evidence-list.external { color: #6d28d9; }

.dept-tags { display: flex; flex-wrap: wrap; gap: 6px; }

/* 能力边界 */
.boundary-section { }
.boundary-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.boundary-card { padding: 14px; border: 1px solid #e5e7eb; border-radius: 10px; background: #f9fafb; display: flex; flex-direction: column; gap: 5px; }
.boundary-name { font-size: 14px; font-weight: 800; color: #0f172a; }
.boundary-role { font-size: 12px; color: #7c3aed; font-weight: 600; }
.boundary-output { font-size: 12px; color: #374151; line-height: 1.5; }
.boundary-limit { font-size: 12px; color: #9ca3af; font-style: italic; line-height: 1.5; }

@media (max-width: 1200px) {
  .metrics-row, .exec-grid, .boundary-grid { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
}
</style>
