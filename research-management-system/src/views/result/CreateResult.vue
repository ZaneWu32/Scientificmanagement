<template>
  <div class="create-result">
    <el-card>
      <!-- 步骤条 -->
      <el-steps :active="currentStep" align-center finish-status="success">
        <el-step title="选择类型" />
        <el-step title="填写基础信息" />
        <el-step title="上传附件" />
        <el-step title="智能补全" />
        <el-step title="提交" />
      </el-steps>

      <div class="step-content">
        <!-- 步骤1: 选择类型 -->
        <div v-if="currentStep === 0" class="step-panel">
          <h3>选择成果类型</h3>
          <el-select
            v-model="formData.typeId"
            placeholder="请选择成果类型"
            size="large"
            style="width: 100%"
            @change="handleTypeChange"
          >
            <el-option
              v-for="type in resultTypes"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            >
              <span>{{ type.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                {{ type.description }}
              </span>
            </el-option>
          </el-select>
          <div v-if="selectedType" class="type-info">
            <el-alert :title="selectedType.description" type="info" :closable="false" />
          </div>
        </div>

        <!-- 步骤2: 填写基础信息 -->
        <div v-show="currentStep === 1" class="step-panel" v-loading="loadingFields" element-loading-text="正在加载字段配置...">
          <h3>填写基础信息</h3>
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="120px"
          >
            <el-form-item label="成果标题" prop="title" required>
              <el-input v-model="formData.title" placeholder="请输入成果标题" />
            </el-form-item>
            <el-form-item label="作者" prop="authors" required>
              <el-select
                v-model="formData.authors"
                multiple
                filterable
                allow-create
                placeholder="请选择或输入作者"
                style="width: 100%"
              >
                <el-option :label="currentUserName" :value="currentUserName" />
              </el-select>
            </el-form-item>
            <el-form-item label="年份" prop="year" required>
              <el-date-picker
                v-model="formData.year"
                type="year"
                placeholder="选择年份"
                value-format="YYYY"
              />
            </el-form-item>
            <el-form-item label="摘要" prop="abstract">
              <el-input
                v-model="formData.abstract"
                type="textarea"
                :rows="4"
                placeholder="请输入成果摘要"
              />
            </el-form-item>
            <el-form-item label="关键词" prop="keywords">
              <el-select
                v-model="formData.keywords"
                multiple
                filterable
                allow-create
                placeholder="请输入关键词，按回车添加"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 动态字段 - 使用组件映射模型 -->
            <template v-if="selectedType">
              <el-form-item
                v-for="field in selectedType.fields"
                :key="field.id"
                :label="field.label"
                :prop="`metadata.${field.name}`"
                :required="field.required"
              >
                <DynamicFieldRenderer
                  :field="field"
                  v-model="formData.metadata[field.name]"
                />
              </el-form-item>
            </template>
          </el-form>
        </div>

        <!-- 步骤3: 上传附件 -->
        <div v-show="currentStep === 2" class="step-panel">
          <h3>上传附件与设置可见范围</h3>
          <el-form label-width="120px">
            <el-form-item label="附件上传">
              <el-upload
                v-model:file-list="fileList"
                :auto-upload="false"
                :on-change="handleFileChange"
                drag
                multiple
              >
                <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                <div class="el-upload__text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
              </el-upload>
              <div class="upload-hint">
                当前演示版会使用公开 PDF 样本展示“上传后预填”的效果。上传任意演示附件后，可在下一步选择公开样本完成识别。
              </div>
            </el-form-item>
            <el-form-item label="可见范围" required>
              <el-radio-group v-model="formData.visibility">
                <el-radio label="private">私有</el-radio>
                <el-radio label="public">公开</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤4: 智能补全 -->
        <div v-show="currentStep === 3" class="step-panel">
          <h3>智能补全</h3>
          <el-alert
            title="当前演示使用公开开放 PDF 样本模拟识别与预填流程，重点说明‘预填字段 + 原文依据 + 待确认项’的交互方式。"
            type="info"
            :closable="false"
          />

          <el-alert
            v-if="selectedType?.code !== 'paper'"
            title="当前公开样本更适配论文类成果，其他成果类型仍可演示流程，但字段命中会更偏标题、作者、摘要等通用项。"
            type="warning"
            :closable="false"
            class="step-alert"
          />

          <el-card shadow="never" class="auto-fill-card">
            <template #header>
              <div class="card-head">
                <div>
                  <div class="card-title">公开样本选择</div>
                  <div class="card-desc">用于模拟上传附件后的智能识别与字段预填。</div>
                </div>
                <el-button type="primary" :loading="autoFilling" @click="handleAutoFill">
                  开始识别
                </el-button>
              </div>
            </template>

            <el-form label-width="120px">
              <el-form-item label="演示样本">
                <el-select v-model="autoFillSampleId" placeholder="请选择公开 PDF 样本" style="width: 100%">
                  <el-option
                    v-for="sample in autoFillSamples"
                    :key="sample.id"
                    :label="sample.sampleFileMeta.title"
                    :value="sample.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="已上传附件">
                <div class="uploaded-files">
                  <span v-if="fileList.length">{{ fileList.length }} 个演示附件</span>
                  <span v-else>未上传演示附件，仍可继续查看公开样本识别效果。</span>
                </div>
              </el-form-item>
            </el-form>

            <div v-if="selectedAutoFillSample" class="sample-meta">
              <div class="meta-grid">
                <div class="meta-item">
                  <span class="meta-label">样本文件</span>
                  <span class="meta-value">{{ selectedAutoFillSample.sampleFileMeta.fileName }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">来源</span>
                  <span class="meta-value">{{ selectedAutoFillSample.sourceName }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">发布日期</span>
                  <span class="meta-value">{{ selectedAutoFillSample.publishDate }}</span>
                </div>
                <div class="meta-item">
                  <span class="meta-label">采集日期</span>
                  <span class="meta-value">{{ selectedAutoFillSample.capturedDate }}</span>
                </div>
              </div>
              <p class="sample-note">{{ selectedAutoFillSample.curationNote }}</p>
              <el-link :href="selectedAutoFillSample.sourceUrl" target="_blank" type="primary">
                查看公开 PDF 样本
              </el-link>
            </div>
          </el-card>

          <div v-if="autoFillResult" class="auto-fill-result">
            <el-row :gutter="16" class="summary-row">
              <el-col :xs="24" :md="8">
                <el-card shadow="never" class="mini-summary">
                  <div class="summary-label">识别字段</div>
                  <div class="summary-value">{{ autoFillResult.recognizedFields.length }}</div>
                </el-card>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-card shadow="never" class="mini-summary">
                  <div class="summary-label">待确认项</div>
                  <div class="summary-value">{{ autoFillResult.pendingConfirmations.length }}</div>
                </el-card>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-card shadow="never" class="mini-summary">
                  <div class="summary-label">建议来源</div>
                  <div class="summary-value">{{ autoFillResult.sourceName }}</div>
                </el-card>
              </el-col>
            </el-row>

            <el-card shadow="never" class="auto-fill-card">
              <template #header>
                <div class="card-head">
                  <div>
                    <div class="card-title">预填字段</div>
                    <div class="card-desc">勾选要回填到当前表单的字段，提交仍由人工控制。</div>
                  </div>
                  <el-button type="primary" plain @click="applyAutoFillSelection">
                    回填到表单
                  </el-button>
                </div>
              </template>

              <el-table :data="autoFillResult.recognizedFields" border>
                <el-table-column width="56">
                  <template #default="{ row }">
                    <el-checkbox v-model="selectedAutoFillKeys" :label="row.key" />
                  </template>
                </el-table-column>
                <el-table-column prop="label" label="字段" width="160" />
                <el-table-column label="建议值" min-width="260">
                  <template #default="{ row }">
                    {{ renderRecognizedValue(row.value) }}
                  </template>
                </el-table-column>
                <el-table-column label="置信度" width="120">
                  <template #default="{ row }">
                    {{ Math.round(row.confidence * 100) }}%
                  </template>
                </el-table-column>
              </el-table>
            </el-card>

            <el-row :gutter="16">
              <el-col :xs="24" :lg="12">
                <el-card shadow="never" class="auto-fill-card">
                  <template #header>
                    <div class="card-title">原文依据</div>
                  </template>
                  <div class="evidence-list">
                    <div v-for="item in autoFillResult.fieldEvidence" :key="`${item.fieldKey}-${item.evidenceText}`" class="evidence-item">
                      <div class="evidence-key">{{ item.fieldKey }}</div>
                      <div class="evidence-text">{{ item.evidenceText }}</div>
                      <div class="evidence-hint">{{ item.sourceHint || '来源片段' }}</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
              <el-col :xs="24" :lg="12">
                <el-card shadow="never" class="auto-fill-card">
                  <template #header>
                    <div class="card-title">待确认项</div>
                  </template>
                  <div class="pending-list">
                    <div
                      v-for="item in autoFillResult.pendingConfirmations"
                      :key="`${item.fieldKey}-${item.label}`"
                      class="pending-item"
                    >
                      <div class="pending-title">{{ item.label }}</div>
                      <div class="pending-reason">{{ item.reason }}</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- 步骤5: 确认提交 -->
        <div v-show="currentStep === 4" class="step-panel">
          <h3>确认信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="成果标题">{{ formData.title }}</el-descriptions-item>
            <el-descriptions-item label="成果类型">{{ selectedType?.name }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ formData.authors.join(', ') }}</el-descriptions-item>
            <el-descriptions-item label="年份">{{ formData.year }}</el-descriptions-item>
            <el-descriptions-item label="摘要">{{ formData.abstract }}</el-descriptions-item>
            <el-descriptions-item label="关键词">{{ formData.keywords.join(', ') }}</el-descriptions-item>
            <el-descriptions-item label="可见范围" :span="2">
              {{ getVisibilityText(formData.visibility) }}
            </el-descriptions-item>
          </el-descriptions>
            <!-- 扩展信息（动态字段） -->
          <div v-if="confirmExtraFields.length" style="margin-top: 16px">
            <h4 style="margin: 0 0 8px 0">扩展信息</h4>
            <el-descriptions :column="2" border>
            <el-descriptions-item
              v-for="item in confirmExtraFields"
              :key="item.key"
              :label="item.label"
              :span="item.span || 1"
            >
              {{ item.text }}
            </el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 附件列表 -->
          <div style="margin-top: 16px">
            <h4 style="margin: 0 0 8px 0">附件列表</h4>
            <el-empty v-if="!fileList.length" description="未选择附件" />
            <el-table v-else :data="confirmFiles" size="small" border max-height="320">
              <el-table-column prop="name" label="文件名" min-width="220" />
              <el-table-column prop="sizeText" label="大小" width="120" />
              <el-table-column prop="type" label="类型" width="160" />
            </el-table>
          </div>
        </div>
      </div>
      <!-- 操作按钮 -->
      <div class="step-actions">
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep < 4" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === 4" type="success" :loading="submitting" @click="handleSubmit">
          提交审核
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getResultTypes, getFieldDefsByType, createResult, createResultWithFiles, autoFillMetadata, getAutoFillSamples } from '@/api/result'
import type { AutoFillDemoSample } from '@/types'
import { ResultVisibility } from '@/types'
import DynamicFieldRenderer from '@/components/DynamicFieldRenderer.vue'
import { mapFieldType } from '@/config/dynamicFields'

const router = useRouter()
const userStore = useUserStore()

const currentStep = ref(0)
const resultTypes = ref([])
const loadingFields = ref(false)
const selectedType = computed(() => {
  const type = resultTypes.value.find(t => t.id === formData.typeId)
  return type || null
})
const currentUserName = computed(() => userStore.userInfo?.name || '')

const formRef = ref()
const formData = reactive({
  typeId: '',
  title: '',
  authors: [currentUserName.value || ''],
  year: new Date().getFullYear().toString(),
  abstract: '',
  keywords: [],
  visibility: ResultVisibility.PRIVATE,
  metadata: {} as Record<string, any>,
  attachments: []
})

const formRules = {
  title: [{ required: true, message: '请输入成果标题', trigger: 'blur' }],
  authors: [{ required: true, message: '请选择作者', trigger: 'change' }],
  year: [{ required: true, message: '请选择年份', trigger: 'change' }]
}

const fileList = ref([])
const autoFillSamples = ref<AutoFillDemoSample[]>([])
const autoFillSampleId = ref('')
const autoFilling = ref(false)
const autoFillResult = ref<AutoFillDemoSample | null>(null)
const selectedAutoFillKeys = ref<string[]>([])
const submitting = ref(false)
const MAX_FILE_SIZE = 20 * 1024 * 1024

onMounted(async () => {
  await Promise.all([loadResultTypes(), loadAutoFillSamples()])
})

const selectedAutoFillSample = computed(() => {
  return autoFillSamples.value.find((item) => item.id === autoFillSampleId.value) || null
})

const confirmExtraFields = computed(() => {
  const type = selectedType.value
  const fields = type?.fields || []
  if (!fields.length) return []

  return fields.map((f: any) => {
    const raw = formData.metadata?.[f.name]
    const text = formatFieldValue(f, raw)
    return {
      key: f.name,
      label: f.label,
      text: text || '—'
    }
  })
})

const confirmFiles = computed(() => {
  return (fileList.value || []).map((f: any) => {
    const raw = f.raw || f
    return {
      name: raw?.name || f.name || 'unknown',
      sizeText: formatFileSize(raw?.size || f.size || 0),
      type: raw?.type || f.raw?.type || ''
    }
  })
})

function formatFieldValue(field: any, value: any) {
  if (value === undefined || value === null || value === '') return ''
  // 你 DynamicFieldRenderer 里可能有数组/对象，这里统一转成可读文本
  if (Array.isArray(value)) return value.join('，')
  if (typeof value === 'object') return JSON.stringify(value)
  // switch/checkbox 这种 boolean
  if (field.type === 'switch' || field.type === 'checkbox') return value ? '是' : '否'
  return String(value)
}

function formatFileSize(bytes: number) {
  const b = Number(bytes || 0)
  if (b < 1024) return `${b} B`
  const kb = b / 1024
  if (kb < 1024) return `${kb.toFixed(1)} KB`
  const mb = kb / 1024
  if (mb < 1024) return `${mb.toFixed(1)} MB`
  const gb = mb / 1024
  return `${gb.toFixed(1)} GB`
}

async function loadResultTypes() {
  try {
    const res = await getResultTypes('normal')
    const { data } = res || {}

    resultTypes.value = (data || [])
      .map((t: any) => {
        const isDelete = Number(t.is_delete ?? t.isDelete ?? 0)
        // 后端有 enabled 就用 enabled；没有 enabled 才从 is_delete 推导
        const enabled =
          t.enabled != null ? Number(t.enabled) : (isDelete === 1 ? 0 : 1)

        return {
          ...t,
          id: t.documentId || t.id,
          name: t.type_name || t.typeName || t.name,
          code: t.type_code || t.typeCode || t.code,
          enabled // ✅ 0/1
        }
      })
      .filter((t: any) => t.enabled === 1) //  只显示启用(1)的类型
  } catch (error) {
    ElMessage.error('加载成果类型失败')
  }
}

async function loadAutoFillSamples() {
  try {
    const res = await getAutoFillSamples()
    autoFillSamples.value = res?.data || []
    if (!autoFillSampleId.value && autoFillSamples.value.length > 0) {
      autoFillSampleId.value = autoFillSamples.value[0].id
    }
  } catch (error) {
    console.error('加载演示样本失败:', error)
    ElMessage.error('加载公开样本失败')
  }
}


// 将后端字段定义转换为前端表单字段格式
function transformFieldDef(field: any) {
  return {
    id: field.field_code,
    name: field.field_code,
    label: field.field_name,
    type: mapFieldType(field.field_type), // 使用配置文件中的映射函数
    documentId: field.documentId,
    backendType: field.field_type,
    required: field.is_required === 1,
    helpText: field.description || `请输入${field.field_name}`,
    options: field.options || [], // 支持选项字段
    order: field.order || 0
  }
}

// 当用户选择成果类型后，动态加载该类型的字段定义
async function handleTypeChange() {
  // 清空旧的元数据
  formData.metadata = {}

  if (!formData.typeId) return

  // 查找当前选择的类型
  const currentType = resultTypes.value.find(t => t.id === formData.typeId)
  if (!currentType) return

  // 如果该类型已经加载过字段，不重复加载
  if (currentType.fields && currentType.fields.length > 0) return

  loadingFields.value = true
  try {
    const res = await getFieldDefsByType(formData.typeId)
    const rawFields = res?.data || []

    // 转换字段格式并排序
    const transformedFields = rawFields
      .map(transformFieldDef)
      .sort((a, b) => (a.order || 0) - (b.order || 0))

    // 将字段定义注入到类型对象中
    currentType.fields = transformedFields

    if (transformedFields.length === 0) {
      ElMessage.info('该类型暂未配置动态字段')
    }
  } catch (error) {
    console.error('加载字段定义失败:', error)
    ElMessage.error('加载字段定义失败，请稍后重试')
    // 失败时设置空数组，避免重复请求
    if (currentType) currentType.fields = []
  } finally {
    loadingFields.value = false
  }
}

function validateDynamicFields() {
  if (!selectedType.value) return true
  for (const field of selectedType.value.fields || []) {
    if (!field.required) continue
    const value = formData.metadata[field.name]
    if (value === undefined || value === null || value === '') {
      ElMessage.warning(`请填写${field.label}`)
      return false
    }
  }
  return true
}

async function nextStep() {
  if (currentStep.value === 0 && !formData.typeId) {
    ElMessage.warning('请先选择成果类型')
    return
  }

  if (currentStep.value === 1) {
    try {
      await formRef.value?.validate()
    } catch {
      return
    }

    if (!validateDynamicFields()) return
  }

  currentStep.value++
}

function prevStep() {
  currentStep.value--
}

async function handleAutoFill() {
  if (!autoFillSampleId.value) {
    ElMessage.warning('请先选择公开 PDF 样本')
    return
  }

  autoFilling.value = true
  try {
    if (!fileList.value.length) {
      ElMessage.info('未上传演示附件，当前将直接使用公开样本展示识别效果')
    }

    const res = await autoFillMetadata({
      type: 'sample_pdf',
      value: autoFillSampleId.value
    })

    if (res?.data) {
      autoFillResult.value = res.data
      selectedAutoFillKeys.value = (res.data.recognizedFields || []).map((item: any) => item.key)
      ElMessage.success('识别完成，请确认后回填')
    }
  } catch (error) {
    ElMessage.error('补全失败，请稍后重试')
  } finally {
    autoFilling.value = false
  }
}

function handleFileChange(file, files) {
  if (file?.raw && file.raw.size > MAX_FILE_SIZE) {
    ElMessage.warning('单个附件不能超过 20MB')
    fileList.value = files.filter((item) => item.uid !== file.uid)
    return
  }
  fileList.value = files
}

function renderRecognizedValue(value: any) {
  if (Array.isArray(value)) return value.join('，')
  if (value && typeof value === 'object') return JSON.stringify(value)
  return value ?? '—'
}

function applyAutoFillSelection() {
  if (!autoFillResult.value) {
    ElMessage.warning('请先完成识别')
    return
  }

  if (selectedAutoFillKeys.value.length === 0) {
    ElMessage.warning('请至少选择一个字段进行回填')
    return
  }

  autoFillResult.value.recognizedFields.forEach((field) => {
    if (!selectedAutoFillKeys.value.includes(field.key)) return

    const value = field.value
    switch (field.key) {
      case 'title':
        formData.title = String(value || '')
        break
      case 'authors':
        formData.authors = Array.isArray(value) ? value : [String(value || '')]
        break
      case 'abstract':
        formData.abstract = String(value || '')
        break
      case 'keywords':
        formData.keywords = Array.isArray(value) ? value : [String(value || '')]
        break
      case 'year':
        formData.year = String(value || '')
        break
      default:
        if (field.key.startsWith('metadata.')) {
          const metadataKey = field.key.replace('metadata.', '')
          formData.metadata[metadataKey] = value
        }
    }
  })

  ElMessage.success('已将选中字段回填到表单')
}

const VISIBILITY_TEXT = {
  [ResultVisibility.PRIVATE]: '私有',
  [ResultVisibility.PUBLIC]: '公开'
}

function getVisibilityText(visibility) {
  return VISIBILITY_TEXT[visibility] || visibility
}

async function handleSubmit() {
  submitting.value = true
  try {
    const payload = buildPayload()
    const rawFiles = fileList.value.filter(f => f.raw).map(f => f.raw) as File[]

    if (rawFiles.length > 0) {
      await createResultWithFiles(payload, rawFiles)
    } else {
      await createResult(payload)
    }

    ElMessage.success('提交成功')
    router.push('/results/my')
  } catch (error) {
    const err = error as any
    const message =
      err?.message ||
      err?.response?.data?.message ||
      err?.response?.data?.msg ||
      '提交失败'
    console.error('提交失败:', err)
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

function buildFieldValues() {
  const fields = selectedType.value?.fields || []
  return fields
    .map((field: any) => {
      if (!field.documentId) return null
      const value = formData.metadata[field.name]
      if (value === undefined || value === null || value === '') return null

      const payload: Record<string, any> = {
        achievement_field_def_id: field.documentId
      }

      if (field.type === 'number') {
        payload.numberValue = Number(value)
      } else if (field.type === 'checkbox' || field.type === 'switch') {
        payload.booleanValue = Boolean(value)
      } else if (field.type === 'date') {
        payload.dateValue = value
      } else {
        payload.textValue = value
      }

      return payload
    })
    .filter(Boolean)
}

function buildPayload() {
  return {
    data: {
      title: formData.title,
      year: formData.year,
      authors: formData.authors,
      keywords: formData.keywords,
      summary: formData.abstract,
      achievementStatus: 'PENDING',
      typeDocId: formData.typeId,
      visibilityRange: formData.visibility,
      fields: buildFieldValues()
    }
  }
}
</script>

<style scoped>
.create-result {
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
  color: #303133;
}

.type-info {
  margin-top: 20px;
}

.upload-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
}

.step-alert {
  margin-top: 14px;
}

.auto-fill-card {
  margin-top: 16px;
  border: 1px solid #e5eef7;
  border-radius: 18px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-title {
  font-weight: 700;
  color: #111827;
}

.card-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #6b7280;
}

.uploaded-files,
.sample-note {
  color: #6b7280;
  line-height: 1.7;
}

.sample-meta {
  margin-top: 12px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.meta-label,
.summary-label,
.evidence-hint {
  font-size: 12px;
  color: #64748b;
}

.meta-value {
  font-weight: 600;
  color: #111827;
}

.auto-fill-result {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-row {
  margin: 0;
}

.mini-summary {
  border: 1px solid #e5eef7;
  border-radius: 18px;
}

.summary-value {
  margin-top: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.evidence-list,
.pending-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-item,
.pending-item {
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
}

.evidence-key,
.pending-title {
  font-weight: 700;
  color: #111827;
}

.evidence-text,
.pending-reason {
  margin-top: 8px;
  color: #475569;
  line-height: 1.7;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid #e4e7ed;
}
/* =========================
   Step 5 确认提交页美化
   ========================= */
.step-panel {
  padding: 24px;
}

/* 标题更稳重 */
.step-panel h3 {
  margin-bottom: 18px;
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
  letter-spacing: 0.2px;
}

/* 给确认页加“分区卡片感” */
.step-panel :deep(.el-descriptions) {
  border-radius: 12px;
  overflow: hidden;
}

/* descriptions 的 header/边框更柔和 */
.step-panel :deep(.el-descriptions__header) {
  margin-bottom: 10px;
}

/* 单元格样式（像你截图那种浅底分割） */
.step-panel :deep(.el-descriptions__cell) {
  padding: 12px 14px;
  font-size: 14px;
}

.step-panel :deep(.el-descriptions__label) {
  width: 120px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.step-panel :deep(.el-descriptions__content) {
  color: #111827;
}

/* 关键 tag（关键词那种）更像胶囊 */
.step-panel :deep(.el-tag) {
  border-radius: 999px;
  padding: 0 10px;
}

/* 分区标题（扩展信息 / 附件列表）统一风格 */
.step-panel h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 18px 0 10px 0;
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.step-panel h4::before {
  content: "";
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: #22c55e; /* 绿色强调 */
  display: inline-block;
}

/* 扩展信息块间距 */
.step-panel > div[style*="margin-top: 16px"] {
  margin-top: 18px !important;
}

/* 附件列表 table 更清爽 */
.step-panel :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

/* 表头 */
.step-panel :deep(.el-table__header-wrapper th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

/* 表格行 hover 更轻 */
.step-panel :deep(.el-table__body tr:hover > td) {
  background: #f1f5f9 !important;
}

/* 表格单元格 padding 更舒服 */
.step-panel :deep(.el-table__cell) {
  padding: 12px 12px;
  font-size: 13px;
  color: #111827;
}

/* 附件为空时更居中更紧凑 */
.step-panel :deep(.el-empty) {
  padding: 18px 0;
}

/* 确认页整体“留白更像卡片” */
.step-panel {
  background: #ffffff;
}

/* 让确认页的三个区块更像独立小卡片（不改结构，靠 wrapper 选择器实现） */
.step-panel :deep(.el-descriptions),
.step-panel :deep(.el-table),
.step-panel :deep(.el-empty) {
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

@media (max-width: 900px) {
  .card-head {
    flex-direction: column;
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }
}

</style>
