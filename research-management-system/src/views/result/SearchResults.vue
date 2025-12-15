<template>
  <div class="search-results">
    <el-card>
      <el-form :model="searchForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关键词">
              <el-input v-model="searchForm.keyword" placeholder="搜索标题、作者、摘要" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成果类型">
              <el-select v-model="searchForm.type" placeholder="全部类型" clearable>
                <el-option label="论文" value="paper" />
                <el-option label="专利" value="patent" />
                <el-option label="软著" value="software" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年份范围">
              <el-date-picker
                v-model="searchForm.yearRange"
                type="yearrange"
                range-separator="至"
                start-placeholder="开始年份"
                end-placeholder="结束年份"
                value-format="YYYY"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="searchForm.author" placeholder="作者姓名" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目">
              <el-select v-model="searchForm.projectId" placeholder="全部项目" clearable filterable>
                <el-option :label="'全部'" :value="''" />
                <el-option
                  v-for="project in projects"
                  :key="project.id"
                  :label="getProjectLabel(project)"
                  :value="project.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="title" label="成果名称" min-width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">
              {{ row.title }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="authors" label="作者" width="200">
          <template #default="{ row }">
            {{ row.authors?.join(', ') }}
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="100" />
        <el-table-column prop="projectName" label="所属项目" min-width="180">
          <template #default="{ row }">
            <span v-if="row.projectName">{{ row.projectName }} ({{ row.projectCode }})</span>
            <span v-else class="text-muted">无所属/其他</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getResults } from '@/api/result'
import { getProjects } from '@/api/project'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const projects = ref([])

const searchForm = reactive({
  keyword: '',
  type: '',
  yearRange: [],
  author: '',
  projectId: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  initFromQuery()
  loadProjects()
  handleSearch()
})

async function handleSearch() {
  loading.value = true
  try {
    const res = await getResults({
      ...searchForm,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    const { data } = res || {}
    tableData.value = data?.list || []
    pagination.total = data?.total || 0
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

function handleReset() {
  Object.assign(searchForm, {
    keyword: '',
    type: '',
    yearRange: null,
    author: '',
    projectId: ''
  })
  pagination.page = 1
  handleSearch()
}

function viewDetail(row) {
  router.push(`/results/${row.id}`)
}

async function loadProjects() {
  try {
    const res = await getProjects()
    projects.value = res?.data || []
  } catch (error) {
    ElMessage.error('加载项目列表失败')
  }
}

function getProjectLabel(project) {
  if (!project) return ''
  return `${project.name} (${project.code})`
}

function initFromQuery() {
  const { projectId, keyword } = route.query
  if (projectId && typeof projectId === 'string') {
    searchForm.projectId = projectId
  }
  if (keyword && typeof keyword === 'string') {
    searchForm.keyword = keyword
  }
}
</script>

<style scoped>
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
