<template>
  <div class="result-types">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>成果类型配置</span>
          <el-button type="primary" @click="addType">新增类型</el-button>
        </div>
      </template>

      <el-table :data="types" v-loading="loading">
        <el-table-column prop="name" label="类型名称" width="150" />
        <el-table-column prop="code" label="类型代码" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="editType(row)">
              编辑
            </el-button>
            <el-button type="primary" link size="small" @click="configFields(row)">
              配置字段
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="typeDialog" :title="dialogTitle" width="600px">
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="类型名称" required>
          <el-input v-model="typeForm.name" placeholder="如：论文" />
        </el-form-item>
        <el-form-item label="类型代码" required>
          <el-input v-model="typeForm.code" placeholder="如：paper" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="typeForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getResultTypes } from '@/api/result'

const loading = ref(false)
const types = ref([])
const typeDialog = ref(false)
const isEdit = ref(false)

const typeForm = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  enabled: true
})

const dialogTitle = computed(() => (isEdit.value ? '编辑类型' : '新增类型'))

onMounted(async () => {
  await loadTypes()
})

async function loadTypes() {
  loading.value = true
  try {
    const res = await getResultTypes()
    types.value = res?.data || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function addType() {
  isEdit.value = false
  Object.assign(typeForm, {
    id: '',
    name: '',
    code: '',
    description: '',
    enabled: true
  })
  typeDialog.value = true
}

function editType(row) {
  isEdit.value = true
  Object.assign(typeForm, row)
  typeDialog.value = true
}

function configFields() {
  ElMessage.info('字段配置功能开发中')
}

function saveType() {
  ElMessage.success('保存成功')
  typeDialog.value = false
  loadTypes()
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
