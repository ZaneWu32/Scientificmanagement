<template>
  <div class="report-create">
    <el-card>
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="确认成果物" />
        <el-step title="生成报告" />
        <el-step title="编辑导出" />
      </el-steps>

      <div class="step-content">
        <!-- Step 0: 确认成果物 -->
        <div v-if="currentStep === 0" class="step-panel">
          <h3>已选择 {{ achievements.length }} 个成果物</h3>
          <el-table :data="achievements" border size="small" max-height="400">
            <el-table-column prop="title" label="标题" min-width="240" />
            <el-table-column prop="typeName" label="类型" width="140" />
            <el-table-column prop="year" label="年份" width="100" />
          </el-table>
        </div>

        <!-- Step 1: 生成中 -->
        <div v-if="currentStep === 1" class="step-panel step-generating">
          <el-progress
            type="circle"
            :percentage="taskProgress"
            :status="taskStatus === 'FAILED' ? 'exception' : undefined"
          />
          <p class="status-text">
            <template v-if="taskStatus === 'PENDING'">正在准备数据...</template>
            <template v-else-if="taskStatus === 'GENERATING'">AI 正在分析成果物内容并生成报告，请稍候...</template>
            <template v-else-if="taskStatus === 'COMPLETED'">生成完成</template>
            <template v-else-if="taskStatus === 'FAILED'">
              <span class="error-text">生成失败：{{ taskErrorMsg }}</span>
            </template>
          </p>
        </div>

        <!-- Step 2: 编辑 & 导出 -->
        <div v-if="currentStep === 2" class="step-panel step-editor">
          <div v-if="editor" class="editor-toolbar">
            <el-button-group size="small">
              <el-button @click="editor!.chain().focus().toggleBold().run()" :type="editor!.isActive('bold') ? 'primary' : ''">加粗</el-button>
              <el-button @click="editor!.chain().focus().toggleItalic().run()" :type="editor!.isActive('italic') ? 'primary' : ''">斜体</el-button>
              <el-button @click="editor!.chain().focus().toggleHeading({ level: 1 }).run()" :type="editor!.isActive('heading', { level: 1 }) ? 'primary' : ''">H1</el-button>
              <el-button @click="editor!.chain().focus().toggleHeading({ level: 2 }).run()" :type="editor!.isActive('heading', { level: 2 }) ? 'primary' : ''">H2</el-button>
              <el-button @click="editor!.chain().focus().toggleHeading({ level: 3 }).run()" :type="editor!.isActive('heading', { level: 3 }) ? 'primary' : ''">H3</el-button>
              <el-button @click="editor!.chain().focus().toggleBulletList().run()">列表</el-button>
              <el-button @click="editor!.chain().focus().toggleOrderedList().run()">有序列表</el-button>
            </el-button-group>
          </div>
          <editor-content :editor="editor" class="editor-content" />

          <div class="export-bar">
            <el-text type="info">将导出为 Word 文档格式</el-text>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="step-actions">
        <el-button v-if="currentStep > 0 && currentStep !== 1" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep === 0" type="primary" @click="startGenerate">开始生成</el-button>
        <el-button v-if="currentStep === 1 && taskStatus === 'FAILED'" type="warning" @click="startGenerate">重试</el-button>
        <el-button v-if="currentStep === 2" type="primary" :loading="exporting" @click="handleExport">
          导出
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { exportReport, generateReport, getReportContent, getReportTask } from '@/api/report'
import { Table, TableCell, TableHeader, TableRow } from '@tiptap/extension-table'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

interface AchievementItem {
  documentId: string
  title: string
  typeName?: string
  year?: string
}

const route = useRoute()
const router = useRouter()

const currentStep = ref(0)
const achievements = ref<AchievementItem[]>([])
const taskId = ref('')
const taskStatus = ref('')
const taskProgress = ref(0)
const taskErrorMsg = ref('')
const exporting = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const editor = useEditor({
  extensions: [
    StarterKit,
    Table.configure({ resizable: true }),
    TableRow,
    TableCell,
    TableHeader,
  ],
  content: '',
})

onMounted(() => {
  const data = route.query.data as string
  if (data) {
    try {
      achievements.value = JSON.parse(decodeURIComponent(data))
    } catch {
      ElMessage.error('参数解析失败')
      router.back()
    }
  } else {
    const ids = route.query.ids as string
    if (ids) {
      achievements.value = ids.split(',').map(id => ({ documentId: id, title: id }))
    }
  }

  if (!achievements.value.length) {
    ElMessage.warning('未选择成果物')
    router.back()
  }
})

async function startGenerate() {
  try {
    currentStep.value = 1
    taskStatus.value = 'PENDING'
    taskProgress.value = 0
    taskErrorMsg.value = ''

    const docIds = achievements.value.map(a => a.documentId)
    const res = await generateReport(docIds)
    if (res.code !== 1) {
      taskStatus.value = 'FAILED'
      taskErrorMsg.value = res.msg || '请求失败'
      return
    }
    taskId.value = res.data.taskId
    startPolling()
  } catch (e: any) {
    taskStatus.value = 'FAILED'
    taskErrorMsg.value = e.message || '请求异常'
  }
}

function startPolling() {
  stopPolling()
  const maxAttempts = 60
  let attempts = 0
  pollTimer = setInterval(async () => {
    if (++attempts > maxAttempts) {
      stopPolling()
      taskStatus.value = 'FAILED'
      taskErrorMsg.value = '生成超时，请重试'
      return
    }
    try {
      const res = await getReportTask(taskId.value)
      if (res.code !== 1) return
      const task = res.data
      taskStatus.value = task.status
      taskProgress.value = task.progress
      taskErrorMsg.value = task.errorMsg || ''

      if (task.status === 'COMPLETED') {
        stopPolling()
        await loadContent()
      } else if (task.status === 'FAILED') {
        stopPolling()
      }
    } catch {
      // 忽略单次轮询错误
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function loadContent() {
  try {
    const res = await getReportContent(taskId.value)
    if (res.code === 1 && res.data.html) {
      editor.value?.commands.setContent(res.data.html)
      currentStep.value = 2
    }
  } catch {
    ElMessage.error('加载报告内容失败')
  }
}

function prevStep() {
  currentStep.value--
}

async function handleExport() {
  exporting.value = true
  try {
    const html = editor.value?.getHTML() || ''
    const blob = await exportReport(taskId.value, 'word', html)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const now = new Date()
    const ts = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}`
    a.download = `科研成果报告_${ts}.docx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error('导出失败: ' + (e.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

onBeforeUnmount(() => {
  stopPolling()
  editor.value?.destroy()
})
</script>

<style scoped>
.report-create {
  max-width: 1200px;
  margin: 0 auto;
}

.step-content {
  margin: 40px 0;
  min-height: 400px;
}

.step-panel {
  padding: 20px;
}

.step-panel h3 {
  margin-bottom: 24px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
}

.step-generating {
  text-align: center;
  padding: 60px 20px;
}

.status-text {
  margin-top: 20px;
  font-size: 16px;
  color: #606266;
}

.error-text {
  color: var(--el-color-danger);
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}

/* 编辑器 */
.editor-toolbar {
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  background: var(--el-fill-color-light);
}

.editor-content {
  min-height: 480px;
  max-height: 65vh;
  overflow-y: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 0 0 4px 4px;
  padding: 20px;
}

.editor-content :deep(.tiptap) {
  outline: none;
  min-height: 440px;
}

.editor-content :deep(.tiptap h1) {
  font-size: 24px;
  margin: 16px 0 8px;
}

.editor-content :deep(.tiptap h2) {
  font-size: 20px;
  margin: 14px 0 6px;
}

.editor-content :deep(.tiptap h3) {
  font-size: 16px;
  margin: 12px 0 4px;
}

.editor-content :deep(.tiptap table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.editor-content :deep(.tiptap td),
.editor-content :deep(.tiptap th) {
  border: 1px solid var(--el-border-color);
  padding: 6px 10px;
}

.editor-content :deep(.tiptap th) {
  background: var(--el-fill-color-light);
  font-weight: bold;
}

.export-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
