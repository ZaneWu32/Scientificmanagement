<template>
  <div class="admin-dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6" v-for="stat in stats" :key="stat.key">
        <div class="stat-card" :style="{ background: stat.color }">
          <div class="stat-icon">
            <component :is="stat.icon" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-section">
      <el-col :span="12">
        <el-card>
          <template #header>成果类型分布</template>
          <div ref="typeChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>近5年产出趋势</template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="recent-results">
      <template #header>最新入库成果</template>
      <el-table :data="recentResults" v-loading="loading">
        <el-table-column prop="title" label="成果名称" min-width="200" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="authors" label="作者" width="200">
          <template #default="{ row }">
            {{ row.authors?.join(', ') }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="入库时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Document, Tickets, TrophyBase, TrendCharts } from '@element-plus/icons-vue'
import { getStatistics, getResults } from '@/api/result'
import * as echarts from 'echarts'

const loading = ref(false)
const statistics = ref(null)
const recentResults = ref([])

const typeChartRef = ref(null)
const trendChartRef = ref(null)
const typeChartInstance = ref(null)
const trendChartInstance = ref(null)

const stats = ref([
  {
    key: 'total',
    label: '总成果数',
    value: 0,
    icon: Document,
    color: 'var(--primary-gradient)'
  },
  {
    key: 'paper',
    label: '论文数',
    value: 0,
    icon: Tickets,
    color: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
  },
  {
    key: 'patent',
    label: '专利数',
    value: 0,
    icon: TrophyBase,
    color: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)'
  },
  {
    key: 'monthly',
    label: '本月新增',
    value: 0,
    icon: TrendCharts,
    color: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)'
  }
])

onMounted(async () => {
  await loadData()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeCharts()
})

async function loadData() {
  loading.value = true
  try {
    const [statsRes, resultsRes] = await Promise.all([
      getStatistics(),
      getResults({ page: 1, pageSize: 10 })
    ])
    const statsData = statsRes?.data || {}
    const resultsData = resultsRes?.data || {}
    
    statistics.value = statsData
    recentResults.value = resultsData.list || []

    if (stats.value[0]) stats.value[0].value = statsData.totalResults
    if (stats.value[1]) stats.value[1].value = statsData.paperCount
    if (stats.value[2]) stats.value[2].value = statsData.patentCount
    if (stats.value[3]) stats.value[3].value = statsData.monthlyNew
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (!statistics.value) return

  disposeCharts()

  if (typeChartRef.value) {
    typeChartInstance.value = echarts.init(typeChartRef.value)
    typeChartInstance.value.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 10 },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          data: statistics.value?.typeDistribution || []
        }
      ]
    })
  }

  if (trendChartRef.value) {
    const trendData = statistics.value?.yearlyTrend || []
    trendChartInstance.value = echarts.init(trendChartRef.value)
    trendChartInstance.value.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: trendData.map((item) => item.year)
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: trendData.map((item) => item.count),
          itemStyle: { color: '#1d5bff' }
        }
      ]
    })
  }
}

function handleResize() {
  typeChartInstance.value?.resize()
  trendChartInstance.value?.resize()
}

function disposeCharts() {
  typeChartInstance.value?.dispose()
  trendChartInstance.value?.dispose()
  typeChartInstance.value = null
  trendChartInstance.value = null
}
</script>

<style scoped>
.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  padding: 24px;
  border-radius: 8px;
  color: white;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: transform 0.3s;
  cursor: pointer;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  font-size: 48px;
  opacity: 0.8;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.chart-section {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}
</style>
