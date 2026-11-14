<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>端入口管理</h1>
        <p class="sub">小程序码、公众号二维码、H5 链接、App 下载与带参码统一管理；带参码用于来源归因，扫码计数自动累加</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="openForm()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新增入口</span>
        </el-button>
      </div>
    </div>

    <div class="type-grid">
      <div v-for="t in types" :key="t.value" class="type-card" :class="{ active: entryType === t.value }" @click="entryType = entryType === t.value ? '' : t.value; load()">
        <Icon :name="t.icon" :size="20" />
        <div>
          <strong>{{ t.label }}</strong>
          <em>{{ countOf(t.value) }} 个</em>
        </div>
      </div>
    </div>

    <div class="card flush">
      <el-table v-loading="loading" :data="list" size="small" stripe>
        <el-table-column label="入口" min-width="180">
          <template #default="{ row }">
            <div class="entry-cell">
              <div v-if="row.imageKey" class="qr"><img :src="row.imageKey" alt="" /></div>
              <div v-else class="qr placeholder"><Icon name="grid" :size="16" /></div>
              <div>
                <strong>{{ row.name }}</strong>
                <em>{{ row.entryType }}</em>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="channelCode" label="渠道码" width="130">
          <template #default="{ row }"><span class="mono">{{ row.channelCode || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="url" label="地址" min-width="260">
          <template #default="{ row }">
            <span class="mono link">{{ row.url || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="scanCount" label="扫码/访问" width="110" align="right" />
        <el-table-column prop="createdTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copy(row)">复制参数</el-button>
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty"><strong>还没有入口</strong>为每个渠道创建带参码，官网与物料统一使用</div>
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑入口' : '新增入口'" width="520px">
      <el-form :model="form" label-position="top" class="two-col">
        <el-form-item label="入口类型" required>
          <el-select v-model="form.entryType" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" placeholder="如 官网首页小程序码" /></el-form-item>
        <el-form-item label="渠道码"><el-input v-model="form.channelCode" placeholder="如 web-home、mp-poster01" /></el-form-item>
        <el-form-item label="门店 ID"><el-input v-model="form.storeId" placeholder="多门店时填写" /></el-form-item>
        <el-form-item label="链接地址" class="full">
          <el-input v-model="form.url" placeholder="https://... 或 miniprogram://pages/index/index?ch=xxx" />
        </el-form-item>
        <el-form-item label="二维码 / 截图" class="full">
          <ImageUploader v-model="form.imageKey" dir="entry" :limit="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import ImageUploader from '../../components/ImageUploader.vue'
import { tenantHttp } from '../../api/client'

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const entryType = ref('')

const types = [
  { label: '小程序码', value: '小程序码', icon: 'grid' },
  { label: '公众号二维码', value: '公众号二维码', icon: 'wechat' },
  { label: 'H5 链接', value: 'H5链接', icon: 'globe' },
  { label: 'App 下载', value: 'App下载', icon: 'package' },
  { label: '带参码', value: '带参码', icon: 'layers' }
]

function countOf(value: string) {
  return list.value.filter((e) => e.entryType === value).length
}

async function load() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/entries', {
      params: { entryType: entryType.value || undefined }
    })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const dialog = ref(false)
const form = reactive<any>({})

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { entryType: entryType.value || '小程序码' })
  if (row) Object.assign(form, row)
  dialog.value = true
}

async function save() {
  if (!form.name) {
    ElMessage.warning('请填写名称')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/entries', { ...form })
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function copy(row: any) {
  const text = `${row.url || ''}${row.channelCode ? `${row.url?.includes('?') ? '&' : '?'}ch=${row.channelCode}` : ''}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('带参链接已复制')
  } catch {
    ElMessage.info(text)
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('删除后该入口的扫码归因将失效，确认？', '删除入口', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/entries/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.type-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: var(--space-3); }
.type-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg);
  padding: 14px 16px; cursor: pointer; box-shadow: var(--shadow-xs);
  transition: all 0.15s ease;
}
.type-card:hover { border-color: var(--line-strong); }
.type-card.active { border-color: var(--brand-300); background: var(--brand-50); }
.type-card div { display: flex; flex-direction: column; line-height: 1.3; }
.type-card strong { font-size: 13.5px; }
.type-card em { font-style: normal; font-size: 11.5px; color: var(--ink-400); }
.entry-cell { display: flex; align-items: center; gap: 10px; }
.qr { width: 40px; height: 40px; border-radius: 6px; overflow: hidden; background: var(--surface-sunken); flex: none; }
.qr img { width: 100%; height: 100%; object-fit: cover; display: block; }
.qr.placeholder { display: grid; place-items: center; color: var(--ink-300); }
.entry-cell div:last-child { display: flex; flex-direction: column; line-height: 1.3; }
.entry-cell strong { font-size: 13px; }
.entry-cell em { font-style: normal; font-size: 11px; color: var(--ink-400); }
.link { font-size: 11.5px; color: var(--ink-500); word-break: break-all; }
.empty { padding: 34px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
</style>
