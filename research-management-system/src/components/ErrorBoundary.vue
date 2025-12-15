<template>
  <div v-if="error" class="error-boundary">
    <el-result
      icon="error"
      :title="errorTitle"
      :sub-title="errorMessage"
    >
      <template #extra>
        <el-space>
          <el-button type="primary" @click="handleReset">
            重新加载
          </el-button>
          <el-button @click="handleGoHome">
            返回首页
          </el-button>
          <el-button v-if="showDetails" text @click="toggleDetails">
            {{ detailsVisible ? '隐藏' : '查看' }}详细信息
          </el-button>
        </el-space>
      </template>
    </el-result>

    <el-collapse-transition>
      <el-card v-if="detailsVisible && error" class="error-details" shadow="never">
        <template #header>
          <span>错误详情</span>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="错误消息">
            {{ error.message }}
          </el-descriptions-item>
          <el-descriptions-item label="错误类型">
            {{ error.name }}
          </el-descriptions-item>
          <el-descriptions-item v-if="error.stack" label="堆栈信息">
            <pre class="error-stack">{{ error.stack }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="时间">
            {{ errorTime }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </el-collapse-transition>
  </div>
  <slot v-else />
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, computed } from 'vue'
import { useRouter } from 'vue-router'
import { errorLogger } from '@/utils/errorHandler'

const props = defineProps({
  showDetails: {
    type: Boolean,
    default: import.meta.env.DEV
  },
  onError: {
    type: Function,
    default: null
  }
})

const router = useRouter()
const error = ref<Error | null>(null)
const errorTime = ref('')
const detailsVisible = ref(false)

const errorTitle = computed(() => {
  return '组件加载失败'
})

const errorMessage = computed(() => {
  return error.value?.message || '发生了一个意外错误，请稍后重试'
})

// 捕获子组件错误
onErrorCaptured((err: Error, instance, info) => {
  console.error('ErrorBoundary caught:', err, info)
  
  error.value = err
  errorTime.value = new Date().toLocaleString()
  
  // 记录错误日志
  errorLogger.log(err, {
    componentName: instance?.$options?.name,
    info
  })

  // 调用自定义错误处理
  if (props.onError) {
    props.onError(err, instance, info)
  }

  // 阻止错误继续向上传播
  return false
})

function handleReset() {
  error.value = null
  detailsVisible.value = false
}

function handleGoHome() {
  error.value = null
  router.push('/')
}

function toggleDetails() {
  detailsVisible.value = !detailsVisible.value
}
</script>

<style scoped>
.error-boundary {
  min-height: 400px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.error-details {
  max-width: 800px;
  margin: 20px auto;
}

.error-stack {
  max-height: 300px;
  overflow-y: auto;
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #666;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
