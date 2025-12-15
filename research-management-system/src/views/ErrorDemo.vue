<template>
  <div class="error-demo">
    <el-card header="错误处理系统演示">
      <el-space direction="vertical" :size="20" style="width: 100%">
        <!-- 1. 网络错误演示 -->
        <el-card shadow="never">
          <template #header>
            <span>网络错误测试</span>
          </template>
          <el-space>
            <el-button @click="triggerNetworkError">
              触发网络错误
            </el-button>
            <el-button @click="triggerTimeout">
              触发超时错误
            </el-button>
          </el-space>
        </el-card>

        <!-- 2. 业务错误演示 -->
        <el-card shadow="never">
          <template #header>
            <span>业务错误测试</span>
          </template>
          <el-space>
            <el-button @click="trigger404">
              触发 404 错误
            </el-button>
            <el-button @click="trigger500">
              触发 500 错误
            </el-button>
            <el-button @click="triggerBusinessError">
              触发业务错误
            </el-button>
          </el-space>
        </el-card>

        <!-- 3. 权限错误演示 -->
        <el-card shadow="never">
          <template #header>
            <span>权限错误测试</span>
          </template>
          <el-space>
            <el-button @click="trigger401">
              触发 401 错误
            </el-button>
            <el-button @click="trigger403">
              触发 403 错误
            </el-button>
          </el-space>
        </el-card>

        <!-- 4. 表单验证演示 -->
        <el-card shadow="never">
          <template #header>
            <span>表单提交测试 (使用 useFormSubmit)</span>
          </template>
          <el-form ref="demoFormRef" :model="demoForm" :rules="demoRules">
            <el-form-item label="标题" prop="title">
              <el-input v-model="demoForm.title" placeholder="请输入标题" />
            </el-form-item>
            <el-form-item label="内容" prop="content">
              <el-input 
                v-model="demoForm.content" 
                type="textarea"
                placeholder="请输入内容"
              />
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button 
                  type="primary" 
                  :loading="submitting"
                  @click="handleSubmit"
                >
                  提交表单
                </el-button>
                <el-button @click="handleSubmitWithError">
                  提交（模拟失败）
                </el-button>
              </el-space>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 5. 列表加载演示 -->
        <el-card shadow="never">
          <template #header>
            <span>列表加载测试 (使用 useList)</span>
          </template>
          <el-space direction="vertical" style="width: 100%">
            <el-space>
              <el-button @click="refresh" :loading="loading">
                刷新列表
              </el-button>
              <el-button @click="reset">
                重置列表
              </el-button>
              <el-button @click="loadWithError">
                加载失败
              </el-button>
            </el-space>
            
            <el-table :data="list" v-loading="loading" border>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button 
                    type="danger" 
                    size="small"
                    :loading="deleting"
                    @click="handleDelete(row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <el-pagination
              :current-page="page"
              :page-size="pageSize"
              :total="total"
              layout="total, sizes, prev, pager, next"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </el-space>
        </el-card>

        <!-- 6. 异步操作演示 -->
        <el-card shadow="never">
          <template #header>
            <span>异步操作测试 (使用 useAsyncAction)</span>
          </template>
          <el-space>
            <el-button 
              type="success"
              :loading="executing"
              @click="handleAsyncAction"
            >
              执行成功操作
            </el-button>
            <el-button 
              type="warning"
              :loading="executing"
              @click="handleAsyncActionWithError"
            >
              执行失败操作
            </el-button>
          </el-space>
        </el-card>

        <!-- 7. 组件错误边界演示 -->
        <el-card shadow="never">
          <template #header>
            <span>组件错误边界测试</span>
          </template>
          <el-space direction="vertical" style="width: 100%">
            <el-button @click="showErrorComponent = !showErrorComponent">
              {{ showErrorComponent ? '隐藏' : '显示' }}错误组件
            </el-button>
            
            <ErrorBoundary v-if="showErrorComponent" :show-details="true">
              <ErrorComponent :trigger-error="triggerComponentError" />
            </ErrorBoundary>
          </el-space>
        </el-card>

        <!-- 8. 错误日志查看 -->
        <el-card shadow="never">
          <template #header>
            <el-space>
              <span>错误日志 ({{ errorLogs.length }})</span>
              <el-button size="small" @click="refreshLogs">
                刷新日志
              </el-button>
              <el-button size="small" @click="clearLogs">
                清空日志
              </el-button>
            </el-space>
          </template>
          
          <el-timeline v-if="errorLogs.length > 0">
            <el-timeline-item 
              v-for="(log, index) in errorLogs" 
              :key="index"
              :timestamp="log.timestamp"
              placement="top"
            >
              <el-card>
                <p><strong>类型:</strong> {{ log.type }}</p>
                <p><strong>消息:</strong> {{ log.message }}</p>
                <p><strong>代码:</strong> {{ log.code }}</p>
                <el-collapse v-if="log.stack">
                  <el-collapse-item title="查看堆栈">
                    <pre style="font-size: 12px; max-height: 200px; overflow-y: auto">{{ log.stack }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无错误日志" />
        </el-card>
      </el-space>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import { 
  useFormSubmit, 
  useList, 
  useDelete, 
  useAsyncAction 
} from '@/composables/useErrorHandler'
import { 
  errorLogger, 
  AppError, 
  ErrorType, 
  ErrorHandler 
} from '@/utils/errorHandler'

// 错误组件
const ErrorComponent = {
  props: ['triggerError'],
  setup(props: any) {
    if (props.triggerError) {
      throw new Error('这是一个组件内部错误!')
    }
    return () => '正常组件'
  }
}

const showErrorComponent = ref(false)
const triggerComponentError = ref(false)

// 网络错误
const triggerNetworkError = () => {
  const error = ErrorHandler.handleNetworkError({ message: 'Network Error' })
  ErrorHandler.showError(error)
}

const triggerTimeout = () => {
  const error = ErrorHandler.handleNetworkError({ code: 'ECONNABORTED' })
  ErrorHandler.showError(error)
}

// HTTP 错误
const trigger404 = () => {
  const error = ErrorHandler.handleHttpError(404)
  ErrorHandler.showError(error)
}

const trigger500 = () => {
  const error = ErrorHandler.handleHttpError(500, { message: '服务器内部错误' })
  ErrorHandler.showError(error)
}

const trigger401 = () => {
  const error = ErrorHandler.handleHttpError(401)
  ErrorHandler.handlePermissionError(error)
}

const trigger403 = () => {
  const error = ErrorHandler.handleHttpError(403)
  ErrorHandler.handlePermissionError(error)
}

const triggerBusinessError = () => {
  const error = ErrorHandler.handleBusinessError(
    'BIZ_ERROR_001',
    '业务处理失败: 数据不符合要求',
    { field: 'title', constraint: 'max_length' }
  )
  ErrorHandler.showError(error)
}

// 表单演示
const demoFormRef = ref()
const demoForm = reactive({
  title: '',
  content: ''
})

const demoRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, message: '标题至少5个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
}

const { submitting, submit } = useFormSubmit(
  async (formData) => {
    // 模拟 API 调用
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ id: Date.now(), ...formData })
      }, 1000)
    })
  },
  {
    validate: async () => {
      if (!demoFormRef.value) return false
      return await demoFormRef.value.validate()
    },
    successMessage: '提交成功!',
    onSuccess: (data) => {
      console.log('表单提交成功:', data)
      demoForm.title = ''
      demoForm.content = ''
    }
  }
)

const handleSubmit = () => submit(demoForm)

const handleSubmitWithError = async () => {
  const { submitting: errorSubmitting, submit: errorSubmit } = useFormSubmit(
    async () => {
      throw new AppError(
        '提交失败: 服务器拒绝了请求',
        ErrorType.BUSINESS,
        'SUBMIT_REJECTED'
      )
    },
    {
      validate: () => demoFormRef.value.validate()
    }
  )
  
  await errorSubmit(demoForm)
}

// 列表演示
interface ListItem {
  id: number
  title: string
}

const mockListData: ListItem[] = Array.from({ length: 50 }, (_, i) => ({
  id: i + 1,
  title: `测试数据 ${i + 1}`
}))

const {
  loading,
  list,
  total,
  page,
  pageSize,
  refresh,
  reset,
  handlePageChange,
  handleSizeChange
} = useList<ListItem>(
  async (params) => {
    // 模拟 API 调用
    return new Promise((resolve) => {
      setTimeout(() => {
        const start = (params.page - 1) * params.pageSize
        const end = start + params.pageSize
        resolve({
          list: mockListData.slice(start, end),
          total: mockListData.length
        })
      }, 500)
    })
  },
  { immediate: true }
)

const loadWithError = async () => {
  const error = new AppError(
    '加载列表失败: 网络异常',
    ErrorType.NETWORK,
    'LIST_LOAD_ERROR'
  )
  ErrorHandler.showError(error)
}

// 删除演示
const { deleting, confirmDelete } = useDelete(
  async (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const index = mockListData.findIndex(item => item.id === id)
        if (index > -1) {
          mockListData.splice(index, 1)
        }
        resolve(true)
      }, 500)
    })
  },
  {
    confirmMessage: '确定要删除这条数据吗?',
    successMessage: '删除成功!',
    onSuccess: refresh
  }
)

const handleDelete = (id: number) => confirmDelete(id)

// 异步操作演示
const { executing, execute } = useAsyncAction(
  async (success: boolean) => {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        if (success) {
          resolve({ status: 'success', message: '操作完成' })
        } else {
          reject(new AppError(
            '异步操作失败',
            ErrorType.BUSINESS,
            'ASYNC_FAILED'
          ))
        }
      }, 1000)
    })
  },
  {
    successMessage: '异步操作成功!'
  }
)

const handleAsyncAction = () => execute(true)
const handleAsyncActionWithError = () => execute(false)

// 错误日志
const errorLogs = ref(errorLogger.getLogs())

const refreshLogs = () => {
  errorLogs.value = errorLogger.getLogs()
}

const clearLogs = () => {
  errorLogger.clearLogs()
  errorLogs.value = []
  ElMessage.success('日志已清空')
}
</script>

<style scoped>
.error-demo {
  padding: 20px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
