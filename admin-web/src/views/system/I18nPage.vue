<template>
  <div class="page">
    <div class="page-title">
      <h2>多语言</h2>
      <span class="hint">语言列表、默认语言与语言包；缺失词条自动回落默认语言</span>
    </div>

    <div class="card">
      <div class="toolbar" style="justify-content: space-between">
        <h3 class="card-title" style="margin: 0">语言列表</h3>
        <el-button size="small" type="primary" @click="openLanguage()">新增语言</el-button>
      </div>
      <el-table :data="languages" size="small" stripe style="margin-top: 12px">
        <el-table-column prop="name" label="语言" min-width="120" />
        <el-table-column prop="code" label="编码" min-width="100">
          <template #default="{ row }"><span class="mono muted">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="默认" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" size="small" type="success">默认</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLanguage(row)">编辑</el-button>
            <el-button link type="primary" :disabled="row.isDefault === 1" @click="setDefault(row)">设为默认</el-button>
            <el-button link type="danger" :disabled="row.isDefault === 1" @click="removeLanguage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="card">
      <div class="toolbar" style="justify-content: space-between">
        <h3 class="card-title" style="margin: 0">语言包词条</h3>
        <div class="toolbar">
          <el-select v-model="activeLang" style="width: 160px" @change="loadMessages">
            <el-option v-for="l in languages" :key="l.code" :label="`${l.name}（${l.code}）`" :value="l.code" />
          </el-select>
          <el-button size="small" @click="addRow">新增词条</el-button>
          <el-button size="small" type="primary" :loading="saving" @click="saveMessages">保存</el-button>
        </div>
      </div>

      <el-table :data="messages" size="small" style="margin-top: 12px">
        <el-table-column label="文案键" width="260">
          <template #default="{ row }"><el-input v-model="row.key" size="small" placeholder="common.confirm" /></template>
        </el-table-column>
        <el-table-column label="文案内容">
          <template #default="{ row }"><el-input v-model="row.value" size="small" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }"><el-button link type="danger" @click="messages.splice($index, 1)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑语言' : '新增语言'" width="420px">
      <el-form label-width="100px">
        <el-form-item label="语言编码" required>
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 ja-JP" />
        </el-form-item>
        <el-form-item label="中文名" required><el-input v-model="form.name" placeholder="如 日语" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefault" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitLanguage">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteLanguage, listLanguages, listMessages, saveLanguage, saveMessages as saveMsgsApi, setDefaultLanguage
} from '../../api/modules'

const languages = ref<any[]>([])
const messages = ref<{ key: string; value: string }[]>([])
const activeLang = ref('zh-CN')
const dialog = ref(false)
const saving = ref(false)
const form = reactive<any>({ id: null, code: '', name: '', sort: 0, enabled: true, isDefault: false })

const loadLanguages = async () => {
  const res: any = await listLanguages()
  languages.value = res.data
  if (!languages.value.find((l) => l.code === activeLang.value) && languages.value.length) {
    activeLang.value = languages.value.find((l) => l.isDefault === 1)?.code || languages.value[0].code
  }
  await loadMessages()
}

const loadMessages = async () => {
  if (!activeLang.value) return
  const res: any = await listMessages(activeLang.value)
  messages.value = res.data.map((m: any) => ({ key: m.msgKey, value: m.msgValue }))
}

const openLanguage = (row?: any) => {
  Object.assign(form, row
    ? { id: row.id, code: row.code, name: row.name, sort: row.sort, enabled: row.enabled === 1, isDefault: row.isDefault === 1 }
    : { id: null, code: '', name: '', sort: languages.value.length, enabled: true, isDefault: false })
  dialog.value = true
}

const submitLanguage = async () => {
  if (!form.code || !form.name) {
    ElMessage.warning('语言编码与中文名必填')
    return
  }
  await saveLanguage(form.id, {
    code: form.code, name: form.name, sort: form.sort, enabled: form.enabled, isDefault: form.isDefault
  })
  dialog.value = false
  await loadLanguages()
  ElMessage.success('已保存')
}

const setDefault = async (row: any) => {
  await setDefaultLanguage(row.id)
  await loadLanguages()
  ElMessage.success('默认语言已切换')
}

const removeLanguage = async (row: any) => {
  await ElMessageBox.confirm(`确认删除语言「${row.name}」及其语言包？`, '二次确认', { type: 'warning' })
  await deleteLanguage(row.id)
  await loadLanguages()
}

const addRow = () => messages.value.unshift({ key: '', value: '' })

const saveMessages = async () => {
  const payload: Record<string, string> = {}
  messages.value.forEach((m) => {
    if (m.key.trim()) payload[m.key.trim()] = m.value
  })
  saving.value = true
  try {
    await saveMsgsApi({ langCode: activeLang.value, messages: payload })
    ElMessage.success('语言包已保存并即时生效')
    await loadMessages()
  } finally {
    saving.value = false
  }
}

onMounted(loadLanguages)
</script>
