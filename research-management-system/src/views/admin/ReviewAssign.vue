<template>
  <div class="review-assign">
    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span class="title">分配审核人</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 待分配 -->
        <el-tab-pane label="待分配" name="pending">
          <div class="pending-toolbar">
            <el-button
              type="primary"
              :disabled="!selectedResultIds.length"
              @click="assignSelectedResults"
            >
              批量分配审核人（已选 {{ selectedResultIds.length }} 项）
            </el-button>
          </div>
          <el-table
            :data="tableData"
            v-loading="loading"
            stripe
            border
            class="table"
            :header-cell-style="headerStyle"
            row-key="id"
            @selection-change="handlePendingSelectionChange"
          >
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column prop="title" label="成果名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="120" align="center" />
            <el-table-column prop="createdBy" label="提交人" width="140" align="center" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="提交时间" width="180" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="time-text">{{ formatDateTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="assignReviewer(row)">
                  单条分配
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 已分配 -->
        <el-tab-pane label="已分配审核" name="assigned">
          <el-table
            :data="assignedTableData"
            v-loading="loading"
            stripe
            border
            class="table"
            :header-cell-style="headerStyle"
          >
            <el-table-column prop="title" label="成果名称" min-width="240" show-overflow-tooltip />
            <el-table-column prop="type" label="成果类型" width="120" align="center" />
            <el-table-column prop="createdBy" label="成果物提交人" width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="reviewerName" label="已分配审核人名称" min-width="180" align="center" show-overflow-tooltip />
            <el-table-column label="分配日期" width="190" align="center">
              <template #default="{ row }">
                <span class="time-text">{{ formatDateTime(row.assignedAt) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 已分配分页 -->
          <div class="pagination">
            <el-pagination
              v-model:current-page="assignedPagination.page"
              v-model:page-size="assignedPagination.pageSize"
              :total="assignedPagination.total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleAssignedSizeChange"
              @current-change="handleAssignedCurrentChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 分配弹窗 -->
    <el-dialog v-model="assignDialog" title="分配审核人" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="已选成果">
          <span>{{ targetAchievementIds.length }} 条</span>
        </el-form-item>
        <el-form-item label="选择审核人">
          <el-select
            v-model="selectedReviewerId"
            placeholder="请选择审核人"
            filterable
            clearable
            v-loading="reviewersLoading"
            style="width: 100%"
          >
            <el-option
              v-for="reviewer in reviewerOptions"
              :key="reviewer.id"
              :label="reviewer.label"
              :value="reviewer.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign" :loading="assignLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingAssignResults, assignReviewers, getAssignReviewersList } from '@/api/result'
import { getAdminUsers, getExpertUsers } from '@/api/user'
import { formatDateTime } from '@/utils/date'
import { buildReviewerOptions, type ReviewerOption } from '@/utils/reviewer'

// 如果你项目已有 dayjs：
// import dayjs from 'dayjs'

const loading = ref(false)
const reviewersLoading = ref(false)
const assignLoading = ref(false)

const activeTab = ref<'pending' | 'assigned'>('pending')

// 待分配
const tableData = ref<any[]>([])

// 已分配
const assignedTableData = ref<any[]>([])
const assignedPagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

// 分配弹窗
const assignDialog = ref(false)
const selectedReviewerId = ref<number | null>(null)
const selectedResultIds = ref<string[]>([])
const targetAchievementIds = ref<string[]>([])
const reviewerOptions = ref<ReviewerOption[]>([])

const headerStyle = computed(() => ({
  background: '#fafafa',
  color: '#303133',
  fontWeight: 600
}))

onMounted(() => {
  loadReviewers()
  // 首次进入根据 tab 加载
  if (activeTab.value === 'pending') loadPending()
  else loadAssigned()
})


async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingAssignResults({ page: 1, pageSize: 50 })
    tableData.value = res?.data?.list || []
    selectedResultIds.value = []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载待分配数据失败')
  } finally {
    loading.value = false
  }
}

async function loadAssigned() {
  loading.value = true
  try {
    const res = await getAssignReviewersList({
      page: assignedPagination.value.page,
      pageSize: assignedPagination.value.pageSize
    })
    assignedTableData.value = res?.data?.list || []
    assignedPagination.value.total = res?.data?.total || 0
  } catch (e) {
    console.error(e)
    ElMessage.error('加载已分配数据失败')
  } finally {
    loading.value = false
  }
}

async function loadReviewers() {
  reviewersLoading.value = true
  try {
    const [adminRes, expertRes] = await Promise.all([getAdminUsers(), getExpertUsers()])
    const admins = Array.isArray(adminRes?.data) ? adminRes.data : []
    const experts = Array.isArray(expertRes?.data) ? expertRes.data : []
    reviewerOptions.value = buildReviewerOptions(admins, experts)
  } catch (error) {
    console.error('加载审核人列表失败:', error)
    ElMessage.error('加载审核人列表失败')
  } finally {
    reviewersLoading.value = false
  }
}

// tab 切换
function handleTabChange() {
  if (activeTab.value === 'pending') {
    loadPending()
  } else {
    selectedResultIds.value = []
    assignedPagination.value.page = 1
    loadAssigned()
  }
}

// ✅ 分页事件（已分配）
function handleAssignedSizeChange(size: number) {
  assignedPagination.value.pageSize = size
  assignedPagination.value.page = 1
  loadAssigned()
}
function handleAssignedCurrentChange(page: number) {
  assignedPagination.value.page = page
  loadAssigned()
}

function toAchievementId(id: any) {
  if (id === undefined || id === null) return ''
  return String(id)
}

function handlePendingSelectionChange(rows: any[]) {
  selectedResultIds.value = rows
    .map((row) => toAchievementId(row?.id))
    .filter((id) => !!id)
}

function openAssignDialog(ids: string[]) {
  if (!ids.length) {
    ElMessage.warning('请先选择成果')
    return
  }
  targetAchievementIds.value = ids
  selectedReviewerId.value = null
  assignDialog.value = true
}

function assignReviewer(row: any) {
  const id = toAchievementId(row?.id)
  openAssignDialog(id ? [id] : [])
}

function assignSelectedResults() {
  openAssignDialog([...selectedResultIds.value])
}

async function confirmAssign() {
  if (!targetAchievementIds.value.length) {
    ElMessage.warning('未选择成果')
    return
  }
  if (selectedReviewerId.value === null) {
    ElMessage.warning('请选择审核人')
    return
  }
  const selectedReviewer = reviewerOptions.value.find((reviewer) => reviewer.id === selectedReviewerId.value)
  if (!selectedReviewer) {
    ElMessage.warning('审核人信息异常，请刷新后重试')
    return
  }

  assignLoading.value = true
  try {
    const payload = {
      reviewerIds: [selectedReviewer.id],
      reviewerNames: [selectedReviewer.reviewerName]
    }
    const assignResults = await Promise.allSettled(
      targetAchievementIds.value.map((id) => assignReviewers(id, payload))
    )
    const successCount = assignResults.filter((item) => item.status === 'fulfilled').length
    const failureCount = assignResults.length - successCount

    if (failureCount === 0) {
      ElMessage.success(`分配成功，共 ${successCount} 条`)
    } else {
      ElMessage.warning(`部分分配成功：成功 ${successCount} 条，失败 ${failureCount} 条`)
      assignResults
        .filter((item) => item.status === 'rejected')
        .forEach((item) => console.error('分配失败明细:', item.reason))
    }
    assignDialog.value = false

    if (activeTab.value === 'pending') {
      await loadPending()
    } else {
      await loadAssigned()
    }
  } catch (error) {
    console.error('分配失败:', error)
    ElMessage.error('分配失败')
  } finally {
    assignLoading.value = false
  }
}
</script>

<style scoped>
.review-assign {
  padding: 8px;
}

.card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.pending-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.table :deep(.el-table__cell) {
  padding: 12px 0;
}
.table :deep(.el-table__header .el-table__cell) {
  padding: 14px 0;
}
.table :deep(.el-table__body tr:hover > td) {
  background-color: #f7faff !important;
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: #606266;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
