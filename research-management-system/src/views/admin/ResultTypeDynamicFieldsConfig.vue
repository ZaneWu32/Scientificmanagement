<template>
  <div class="dynamic-fields-config-container" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>类型基础信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="类型名称">{{ initialData.name }}</el-descriptions-item>
            <el-descriptions-item label="类型代码">{{ initialData.code }}</el-descriptions-item>
            <el-descriptions-item label="ID标识">
              <el-tag size="small">{{ initialData.documentId }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>动态字段配置</span>
              <el-button type="primary" link @click="addField">+ 新增字段</el-button>
            </div>
          </template>

          <el-table :data="fields" row-key="tempKey" border>
            <el-table-column label="字段编码 (field_code)" prop="field_code" width="180">
              <template #header>
                <span style="color: #f56c6c; margin-right: 4px;">*</span>
                <span>字段编码</span>
              </template>
              <template #default="{ row }">
                <el-input 
                  v-model.trim="row.field_code" 
                  size="small" 
                  placeholder="如：paper_title" 
                  :disabled="!!row.documentId" 
                />
              </template>
            </el-table-column>
            
            <el-table-column label="字段名称" prop="field_name" width="180">
              <template #header>
                <span style="color: #f56c6c; margin-right: 4px;">*</span>
                <span>字段名称</span>
              </template>
              <template #default="{ row }">
                <el-input v-model.trim="row.field_name" size="small" placeholder="如：论文标题" />
              </template>
            </el-table-column>
            
            <el-table-column label="字段类型" prop="field_type" width="120">
              <template #default="{ row }">
                <el-select v-model="row.field_type" size="small">
                  <el-option v-for="t in FIELD_TYPES" :key="t" :label="t" :value="t" />
                </el-select>
              </template>
            </el-table-column>
            
            <el-table-column label="说明" prop="description">
              <template #default="{ row }">
                <el-input v-model="row.description" size="small" />
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row, $index }">
                <el-popconfirm title="确定删除该字段吗？" @confirm="handleDeleteField(row, $index)">
                  <template #reference>
                    <el-button link type="danger" size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    
    <div class="dialog-footer">
      <el-button @click="$emit('close')">关闭</el-button>
      <el-button type="primary" :loading="saving" @click="saveAllFields">保存配置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getFieldDefsByType, 
  createFieldDef, 
  updateFieldDef, 
  deleteFieldDef,
  updateResultTypeFieldOrder,
  type AchievementFieldDef 
} from '@/api/result'

const props = defineProps({
  initialData: { type: Object, required: true },
})

const emit = defineEmits(['close'])

const FIELD_TYPES = ["TEXT", "NUMBER", "BOOLEAN", "DATE", "MEDIA", "JSON", "EMAIL", "RICHTEXT","DATETIME"]
const loading = ref(false)
const saving = ref(false)

const fields = ref<any[]>([])
let tempIdCounter = 0

onMounted(() => {
  fetchFields()
})

async function fetchFields() {
 if (!props.initialData.documentId) return
 loading.value = true
 try {
  const res = await getFieldDefsByType(props.initialData.documentId)
  const rawList = res.data || [] 
  
  fields.value = rawList.map((item: any) => ({
   documentId: item.documentId,
   field_code: item.field_code,
   field_name: item.field_name,
   field_type: item.field_type,
   description: item.description,
   // 强制设定 is_required 为 1 (因为前端不再提供勾选，默认即为必填或按逻辑处理)
   // 如果业务上不需要后端校验必填，这里可保持默认
   is_required: 1, 
   tempKey: `server-${item.documentId}`
  }))
 } catch (error) {
  ElMessage.error('字段加载失败')
 } finally {
  loading.value = false
 }
}

function addField() {
  fields.value.push({
    documentId: null,
    field_code: '',
    field_name: '',
    field_type: 'TEXT',
    is_required: 1, // 默认设为 1
    description: '',
    tempKey: `local-${tempIdCounter++}`
  })
}

async function handleDeleteField(row: any, index: number) {
  if (!row.documentId) {
    fields.value.splice(index, 1)
    return
  }
  try {
    loading.value = true
    await deleteFieldDef(row.documentId)
    ElMessage.success('字段已删除')
    fields.value.splice(index, 1)
  } catch (error) {
    ElMessage.error('删除失败')
  } finally {
    loading.value = false
  }
}

async function saveAllFields() {
  // --- 1. 前端必填校验 ---
  for (const [index, field] of fields.value.entries()) {
    if (!field.field_code || !field.field_name) {
      ElMessage.warning(`第 ${index + 1} 行配置不完整：字段编码和名称不能为空`)
      return // 拦截提交
    }
  }

  saving.value = true
  try {
    const orderedDocIds: string[] = []

    for (const field of fields.value) {
      const payload: AchievementFieldDef = {
        field_code: field.field_code,
        field_name: field.field_name,
        field_type: field.field_type,
        is_required: 1, // 默认强制为 1
        description: field.description,
        is_delete: 0
      }

      if (field.documentId) {
        await updateFieldDef(field.documentId, payload)
        orderedDocIds.push(field.documentId)
      } else {
        payload.achievement_type = props.initialData.documentId
        const res = await createFieldDef(payload)
        const newDocId = res?.data?.documentId || res?.data?.attributes?.documentId
        if (newDocId) {
          field.documentId = newDocId
          orderedDocIds.push(newDocId)
        }
      }
    }

    if (props.initialData.documentId && orderedDocIds.length > 0) {
      await updateResultTypeFieldOrder(props.initialData.documentId, orderedDocIds)
    }

    ElMessage.success('配置已保存')
    await fetchFields()
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.dialog-footer { text-align: right; padding-top: 20px; border-top: 1px solid var(--el-border-color-lighter); margin-top: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.dynamic-fields-config-container { padding-bottom: 0px; }
</style>