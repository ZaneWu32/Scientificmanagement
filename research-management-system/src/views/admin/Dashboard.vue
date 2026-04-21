<template>
  <div class="admin-dashboard">
    <section class="hero-card">
      <div>
        <p class="eyebrow">下一版本能力总览</p>
        <h2>科研成果管理系统演示版</h2>
        <p class="hero-copy">
          以公开官方真实数据优先，围绕需求洞察主线组织演示，同时串联研究洞察与成果物自动补全，方便与甲方现场对齐交付边界。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="router.push('/insights/demands')">进入需求洞察</el-button>
        <el-button @click="router.push('/admin/system-settings')">查看数据源</el-button>
      </div>
    </section>

    <el-row :gutter="16" class="summary-grid">
      <el-col :xs="24" :md="8" v-for="card in capabilityCards" :key="card.key">
        <el-card shadow="never" class="summary-card">
          <div class="summary-top">
            <span class="summary-label">{{ card.label }}</span>
            <el-tag :type="card.tagType" effect="plain">{{ card.tag }}</el-tag>
          </div>
          <div class="summary-value">{{ card.value }}</div>
          <p class="summary-note">{{ card.note }}</p>
          <el-button type="primary" link @click="router.push(card.to)">
            {{ card.action }}
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <div class="section-header">
              <div>
                <div class="section-title">推荐演示顺序</div>
                <div class="section-desc">按主线、增值、提效三层组织讲解，避免能力边界混淆。</div>
              </div>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="item in demoSteps"
              :key="item.title"
              :timestamp="item.step"
              placement="top"
            >
              <div class="timeline-title">{{ item.title }}</div>
              <p class="timeline-copy">{{ item.copy }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <div class="section-header">
              <div>
                <div class="section-title">真实来源快照</div>
                <div class="section-desc">演示数据以公开官方/平台来源为主，不使用敏感内部材料。</div>
              </div>
            </div>
          </template>
          <el-skeleton :loading="loading" animated :rows="4">
            <template #default>
              <div class="source-list">
                <div v-for="source in topSources" :key="source.name" class="source-item">
                  <div>
                    <div class="source-name">{{ source.name }}</div>
                    <div class="source-meta">样本 {{ source.count }} 条</div>
                  </div>
                  <el-link v-if="source.url" :href="source.url" target="_blank" type="primary">
                    查看来源
                  </el-link>
                </div>
              </div>
            </template>
          </el-skeleton>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <div class="section-header">
              <div>
                <div class="section-title">演示口径</div>
                <div class="section-desc">对外统一强调辅助识别、辅助判断与人工确认。</div>
              </div>
            </div>
          </template>
          <div class="tag-cloud">
            <el-tag v-for="item in messagingTags" :key="item" effect="plain" round>{{ item }}</el-tag>
          </div>
          <p class="section-copy">
            需求洞察是主线能力，研究洞察体现管理增值，成果物自动补全体现录入提效。当前演示版重点是把真实来源、页面承载与交互路径讲清楚，而不是包装为已成熟自动化系统。
          </p>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <div class="section-header">
              <div>
                <div class="section-title">管理层摘要</div>
                <div class="section-desc">来自研究洞察聚合结果，用于首页快速预告。</div>
              </div>
            </div>
          </template>
          <el-skeleton :loading="loading" animated :rows="3">
            <template #default>
              <div class="brief-list">
                <div v-for="brief in insightBrief" :key="brief" class="brief-item">
                  {{ brief }}
                </div>
              </div>
            </template>
          </el-skeleton>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { getDemandStats } from '@/api/demand'
import { getResearchInsightsOverview } from '@/api/result'
import { getCrawlerSources } from '@/api/system'
import type { DemandSourceStat, ResearchInsightOverview } from '@/types'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const demandStats = ref<{ total: number; matched: number; inFollowUp: number } | null>(null)
const sources = ref<DemandSourceStat[]>([])
const researchInsights = ref<ResearchInsightOverview | null>(null)

const capabilityCards = computed(() => [
  {
    key: 'demand',
    label: '需求洞察',
    value: `${demandStats.value?.total || 0} 条真实线索`,
    note: `已结构化 ${demandStats.value?.matched || 0} 条可匹配线索，${demandStats.value?.inFollowUp || 0} 条处于跟进中。`,
    tag: '主线能力',
    tagType: 'success',
    action: '查看需求池',
    to: '/insights/demands'
  },
  {
    key: 'research',
    label: '研究洞察',
    value: `${researchInsights.value?.hotTopics.length || 0} 个热点主题`,
    note: '基于真实线索与开放论文样本形成热点、趋势和供需对照摘要。',
    tag: '管理增值',
    tagType: 'warning',
    action: '查看管理视图',
    to: '/admin/research-insights'
  },
  {
    key: 'autofill',
    label: '成果物自动补全',
    value: '3 个公开 PDF 样本',
    note: '演示上传附件后的字段预填、原文依据与待确认项。',
    tag: '录入提效',
    tagType: 'info',
    action: '进入创建成果',
    to: '/results/create'
  }
])

const topSources = computed(() => sources.value.slice(0, 4))
const insightBrief = computed(() => researchInsights.value?.insightBrief || [])

const demoSteps = [
  {
    step: 'Step 1',
    title: '从首页说明演示边界',
    copy: '先向甲方明确三项能力都进入下一版本，但都坚持“辅助识别、辅助判断、人工确认”的统一口径。'
  },
  {
    step: 'Step 2',
    title: '进入需求洞察主线',
    copy: '用真实来源的需求池、详情、匹配和跟进状态解释“需求采集 -> 需求入池 -> 匹配 -> 跟进”的业务闭环。'
  },
  {
    step: 'Step 3',
    title: '切到研究洞察管理视图',
    copy: '强调热点主题、趋势变化和供需对照是管理视图，不把图谱包装成完整结论。'
  },
  {
    step: 'Step 4',
    title: '展示成果物自动补全',
    copy: '用公开 PDF 样本走一遍上传、识别、原文依据和人工确认回填，突出录入提效价值。'
  }
]

const messagingTags = [
  '公开官方真实数据优先',
  '需求洞察主线推进',
  '研究洞察服务管理层',
  '自动补全不替代人工校对'
]

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, sourceRes, researchRes] = await Promise.all([
      getDemandStats(),
      getCrawlerSources(),
      getResearchInsightsOverview()
    ])

    demandStats.value = {
      total: statsRes?.data?.total || 0,
      matched: statsRes?.data?.matched || 0,
      inFollowUp: statsRes?.data?.inFollowUp || 0
    }
    const rawSourceData = sourceRes?.data as any
    sources.value = statsRes?.data?.sources || rawSourceData?.list || rawSourceData || []
    researchInsights.value = researchRes?.data || null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
  border-radius: 20px;
  color: #0f172a;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.18), transparent 32%),
    radial-gradient(circle at left bottom, rgba(16, 185, 129, 0.16), transparent 36%),
    linear-gradient(135deg, #f8fafc 0%, #eef6ff 48%, #f6fffb 100%);
  border: 1px solid #dbeafe;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f766e;
}

.hero-card h2 {
  margin: 0 0 12px;
  font-size: 28px;
}

.hero-copy {
  max-width: 780px;
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.summary-grid {
  margin: 0;
}

.summary-card,
.detail-card {
  border: 1px solid #e5eef7;
  border-radius: 18px;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
}

.summary-value {
  margin-top: 18px;
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.summary-note,
.section-copy {
  margin: 12px 0 0;
  line-height: 1.7;
  color: #64748b;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.section-desc {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.timeline-title {
  font-weight: 700;
  color: #0f172a;
}

.timeline-copy {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.source-list,
.brief-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.source-item,
.brief-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.source-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.source-name {
  font-weight: 700;
  color: #0f172a;
}

.source-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
  }

  .hero-actions {
    align-items: stretch;
    flex-wrap: wrap;
  }
}
</style>
