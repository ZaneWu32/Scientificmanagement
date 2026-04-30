<template>
  <el-dialog
    v-model="visible"
    title="智能化导出报告"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-steps :active="currentStep" finish-status="success" style="margin-bottom: 24px">
      <el-step title="确认成果物" />
      <el-step title="生成报告" />
      <el-step title="预览编辑" />
      <el-step title="导出" />
    </el-steps>

    <!-- Step 0: 确认成果物 -->
    <div v-if="currentStep === 0">
      <p style="margin-bottom: 12px">已选择 <strong>{{ achievements.length }}</strong> 个成果物：</p>
      <el-table :data="achievements" max-height="300" border size="small">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="typeName" label="类型" width="120" />
        <el-table-column prop="year" label="年份" width="80" />
      </el-table>
    </div>

    <!-- Step 1: 生成中 -->
    <div v-if="currentStep === 1" style="text-align: center; padding: 40px 0">
      <el-progress
        type="circle"
        :percentage="taskProgress"
        :status="taskStatus === 'FAILED' ? 'exception' : undefined"
      />
      <p style="margin-top: 16px; color: #666">
        <template v-if="taskStatus === 'GENERATING'">AI 正在分析成果物内容并生成报告，请稍候...</template>
        <template v-else-if="taskStatus === 'PENDING'">正在准备数据...</template>
        <template v-else-if="taskStatus === 'FAILED'">
          <span style="color: var(--el-color-danger)">生成失败：{{ taskErrorMsg }}</span>
        </template>
      </p>
    </div>

    <!-- Step 2: 预览编辑 -->
    <div v-if="currentStep === 2" class="editor-container">
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
    </div>

    <!-- Step 3: 导出 -->
    <div v-if="currentStep === 3" style="text-align: center; padding: 40px 0">
      <el-icon style="font-size: 48px; color: var(--el-color-success); margin-bottom: 16px"><SuccessFilled /></el-icon>
      <p style="margin-bottom: 24px; font-size: 16px">报告已准备就绪，请选择导出格式：</p>
      <el-radio-group v-model="exportFormat" size="large">
        <el-radio-button value="pdf">PDF 文档</el-radio-button>
        <el-radio-button value="word">Word 文档</el-radio-button>
      </el-radio-group>
    </div>

    <template #footer>
      <div style="display: flex; justify-content: space-between">
        <div>
          <el-button v-if="currentStep > 0 && currentStep !== 1" @click="currentStep--">上一步</el-button>
        </div>
        <div>
          <el-button @click="visible = false">取消</el-button>
          <el-button v-if="currentStep === 0" type="primary" @click="startGenerate">开始生成</el-button>
          <el-button v-if="currentStep === 1 && taskStatus === 'FAILED'" type="warning" @click="startGenerate">重试</el-button>
          <el-button v-if="currentStep === 2" type="primary" @click="currentStep = 3">下一步</el-button>
          <el-button v-if="currentStep === 3" type="primary" :loading="exporting" @click="handleExport">导出</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { exportReport, generateReport, getReportContent, getReportTask } from '@/api/report'
import { SuccessFilled } from '@element-plus/icons-vue'
import { Table, TableCell, TableHeader, TableRow } from '@tiptap/extension-table'
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, ref, watch } from 'vue'

interface AchievementItem {
  documentId: string
  title: string
  typeName?: string
  year?: string
}

const props = defineProps<{
  modelValue: boolean
  achievements: AchievementItem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => emit('update:modelValue', v))

const currentStep = ref(0)
const taskId = ref('')
const taskStatus = ref('')
const taskProgress = ref(0)
const taskErrorMsg = ref('')
const exportFormat = ref<'pdf' | 'word'>('pdf')
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

function handleClose() {
  stopPolling()
  currentStep.value = 0
  taskId.value = ''
  taskStatus.value = ''
  taskProgress.value = 0
  taskErrorMsg.value = ''
  editor.value?.commands.setContent('')
}

async function startGenerate() {
  try {
    currentStep.value = 1
    taskStatus.value = 'PENDING'
    taskProgress.value = 0
    taskErrorMsg.value = ''

    const docIds = props.achievements.map(a => a.documentId)
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
  pollTimer = setInterval(async () => {
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
  } catch (e: any) {
    ElMessage.error('加载报告内容失败')
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const html = editor.value?.getHTML() || ''
    const blob = await exportReport(taskId.value, exportFormat.value, html)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `research_report.${exportFormat.value === 'pdf' ? 'pdf' : 'docx'}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
    visible.value = false
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
.editor-container {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.editor-toolbar {
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
}

.editor-content {
  min-height: 400px;
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
}

.editor-content :deep(.tiptap) {
  outline: none;
  min-height: 380px;
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
</style>
