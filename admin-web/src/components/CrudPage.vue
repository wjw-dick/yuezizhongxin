<template>
  <div class="crud-page">
    <div class="page-head">
      <div>
        <h1>{{ title }}</h1>
        <p v-if="subtitle" class="sub">{{ subtitle }}</p>
      </div>
      <div class="actions">
        <slot name="toolbar" />
        <el-button v-if="creatable" type="primary" @click="openForm()">
          <Icon name="plus" :size="15" />
          <span style="margin-left: 6px">{{ createText }}</span>
        </el-button>
      </div>
    </div>

    <div class="card flush">
      <div v-if="filters.length" class="filter-bar">
        <template v-for="filter in filters" :key="filter.key">
          <el-select
            v-if="filter.type === 'select'"
            v-model="query[filter.key]"
            :placeholder="filter.label"
            clearable
            size="small"
            style="width: 150px"
            @change="reload(1)"
          >
            <el-option v-for="opt in filter.options || []" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input
            v-else
            v-model="query[filter.key]"
            :placeholder="filter.label"
            clearable
            size="small"
            style="width: 180px"
            @keyup.enter="reload(1)"
            @clear="reload(1)"
          />
        </template>
        <el-button size="small" @click="reload(1)">查询</el-button>
        <el-button size="small" text @click="resetQuery">重置</el-button>
        <span class="spacer" />
        <span class="total-hint">共 {{ total }} 条</span>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        size="small"
        stripe
        :row-key="rowKey"
        @row-click="onRowClick"
      >
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.min || 120"
          :align="col.align || 'left'"
          :fixed="col.fixed"
        >
          <template #default="{ row }">
            <slot v-if="col.slot" :name="`col-${col.prop}`" :row="row">
              {{ display(row[col.prop], col) }}
            </slot>
            <template v-else-if="col.type === 'money'">
              <span class="money">¥{{ money(row[col.prop]) }}</span>
            </template>
            <template v-else-if="col.type === 'status'">
              <span class="badge" :class="statusClass(row[col.prop], col)">{{ row[col.prop] || '-' }}</span>
            </template>
            <template v-else-if="col.type === 'bool'">
              <span class="badge" :class="row[col.prop] ? 'ok' : 'muted'">{{ row[col.prop] ? '是' : '否' }}</span>
            </template>
            <template v-else>{{ display(row[col.prop], col) }}</template>
          </template>
        </el-table-column>

        <el-table-column v-if="showActions" label="操作" :width="actionWidth" fixed="right">
          <template #default="{ row }">
            <slot name="row-actions" :row="row" :reload="reload">
              <el-button link type="primary" @click.stop="openForm(row)">编辑</el-button>
              <el-button v-if="deletable" link type="danger" @click.stop="remove(row)">删除</el-button>
            </slot>
          </template>
        </el-table-column>

        <template #empty>
          <div class="empty-state">
            <Icon name="inbox" :size="28" />
            <p>暂无数据</p>
            <span>调整筛选条件，或先新增一条记录</span>
          </div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, sizes, total"
          :page-sizes="[10, 20, 50, 100]"
          background
          small
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-drawer v-model="drawer" :title="form.id ? `编辑${entity}` : `新增${entity}`" size="560px">
      <el-form :model="form" label-position="top" class="crud-form">
        <el-form-item
          v-for="field in visibleFields"
          :key="field.key"
          :label="field.label"
          :required="field.required"
          :class="{ full: field.full || field.type === 'textarea' || field.type === 'richtext' || field.type === 'image' }"
        >
          <el-input
            v-if="!field.type || field.type === 'text'"
            v-model="form[field.key]"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
          <el-input
            v-else-if="field.type === 'textarea'"
            v-model="form[field.key]"
            type="textarea"
            :rows="field.rows || 3"
            :placeholder="field.placeholder || `请输入${field.label}`"
          />
          <el-input
            v-else-if="field.type === 'password'"
            v-model="form[field.key]"
            type="password"
            show-password
            :placeholder="field.placeholder || '留空表示不修改'"
          />
          <el-input-number
            v-else-if="field.type === 'number' || field.type === 'money'"
            v-model="form[field.key]"
            :min="field.min ?? 0"
            :max="field.max"
            :precision="field.type === 'money' ? 2 : 0"
            :step="field.type === 'money' ? 100 : 1"
            controls-position="right"
            style="width: 100%"
          />
          <el-select
            v-else-if="field.type === 'select'"
            v-model="form[field.key]"
            :placeholder="`请选择${field.label}`"
            clearable
            style="width: 100%"
          >
            <el-option v-for="opt in field.options || []" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="`请选择${field.label}`"
            style="width: 100%"
          />
          <el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form[field.key]"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            :placeholder="`请选择${field.label}`"
            style="width: 100%"
          />
          <el-switch v-else-if="field.type === 'switch'" v-model="form[field.key]" />
          <ImageUploader
            v-else-if="field.type === 'image'"
            v-model="form[field.key]"
            :multiple="field.multiple"
            :limit="field.limit || 9"
            :dir="field.dir || 'common'"
          />
          <RichEditor
            v-else-if="field.type === 'richtext'"
            v-model="form[field.key]"
            :min-height="260"
          />
          <div v-else class="fallback-field">不支持的字段类型：{{ field.type }}</div>
          <p v-if="field.hint" class="field-hint">{{ field.hint }}</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawer = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from './Icon.vue'
import ImageUploader from './ImageUploader.vue'
import RichEditor from './RichEditor.vue'
import { createOne, fetchList, removeOne, updateOne } from '../api/resource'

interface Column {
  prop: string
  label: string
  min?: number
  width?: number
  align?: 'left' | 'center' | 'right'
  type?: 'money' | 'status' | 'bool'
  slot?: boolean
  fixed?: 'left' | 'right'
  options?: { label: string; value: string }[]
  format?: (row: any) => string
}

interface Field {
  key: string
  label: string
  type?: string
  required?: boolean
  full?: boolean
  hint?: string
  placeholder?: string
  rows?: number
  options?: { label: string; value: any }[]
  default?: any
  min?: number
  max?: number
  multiple?: boolean
  limit?: number
  dir?: string
}

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    base: string
    entity?: string
    columns: Column[]
    fields?: Field[]
    filters?: { key: string; label: string; type?: string; options?: { label: string; value: any }[] }[]
    rowKey?: string
    pageSize?: number
    creatable?: boolean
    deletable?: boolean
    createText?: string
    showActions?: boolean
    actionWidth?: number
    deletePath?: string
    updateMethod?: 'post' | 'put'
    beforeSubmit?: (payload: any, isEdit: boolean) => any
    extraParams?: Record<string, any>
  }>(),
  {
    entity: '记录',
    fields: () => [],
    filters: () => [],
    rowKey: 'id',
    pageSize: 10,
    creatable: true,
    deletable: true,
    createText: '新增',
    showActions: true,
    actionWidth: 150,
    updateMethod: 'post',
    extraParams: () => ({})
  }
)

const emit = defineEmits<{ (e: 'row-click', row: any): void }>()

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(props.pageSize)
const query = reactive<Record<string, any>>({})
const drawer = ref(false)
const saving = ref(false)
const form = reactive<Record<string, any>>({})

const visibleFields = computed(() => props.fields)
const statusColors: Record<string, string> = {
  待支付: 'warn',
  待入住: 'info',
  在住: 'ok',
  待结算: 'warn',
  已完成: 'ok',
  已取消: 'muted',
  已退订: 'danger',
  待执行: 'warn',
  待处理: 'warn',
  处理中: 'info',
  已闭环: 'ok',
  待确认: 'warn',
  已确认: 'ok',
  已发布: 'ok',
  草稿: 'muted',
  已下线: 'muted',
  已退款: 'muted',
  待审核: 'warn',
  已通过: 'ok',
  已驳回: 'danger',
  在职: 'ok',
  离职: 'muted',
  ENABLED: 'ok',
  DISABLED: 'muted',
  ON_SHELF: 'ok',
  OFF_SHELF: 'muted',
  已同步: 'ok',
  同步失败: 'danger',
  待同步: 'warn',
  已支付: 'ok',
  支付异常: 'danger',
  已签约: 'ok',
  已流失: 'muted',
  已关注: 'ok',
  已取消关注: 'muted',
  在住客户: 'ok',
  待清洁: 'warn',
  维修中: 'danger'
}

/**
 * 状态枚举的中文兜底：不少实体直接存英文枚举（ENABLED / ON_SHELF …），
 * 列未显式配置 options 时也应显示中文，避免列表里出现裸英文。
 */
const STATUS_TEXT: Record<string, string> = {
  ENABLED: '启用',
  DISABLED: '停用',
  ON_SHELF: '上架',
  OFF_SHELF: '下架',
  AVAILABLE: '可预订',
  LOCKED: '已锁定',
  OCCUPIED: '已入住',
  CLEANING: '待清洁',
  MAINTENANCE: '维修中',
  PENDING: '待处理',
  PROCESSING: '处理中',
  FINISHED: '已完成',
  CANCELED: '已取消'
}

function display(value: any, col: Column) {
  if (col.format) {
    return col.format({ [col.prop]: value })
  }
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (col.type === 'status' && col.options?.length) {
    return col.options.find((o) => o.value === value)?.label || value
  }
  if (col.type === 'status' && typeof value === 'string' && STATUS_TEXT[value]) {
    return STATUS_TEXT[value]
  }
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value)) {
    return value.slice(0, 16).replace('T', ' ')
  }
  return value
}

function statusClass(value: string, col: Column) {
  if (col.options?.length) {
    const found = col.options.find((o) => o.value === value)
    if (found) {
      return statusColors[value] || 'info'
    }
  }
  return statusColors[value] || 'info'
}

function money(value: any) {
  const num = Number(value || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  try {
    const { records, total: count } = await fetchList(props.base, {
      ...props.extraParams,
      ...query,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = records
    total.value = count
  } finally {
    loading.value = false
  }
}

function reload(page?: number) {
  if (page) {
    pageNum.value = page
  }
  load()
}

function resetQuery() {
  Object.keys(query).forEach((key) => delete query[key])
  reload(1)
}

function onSizeChange(size: number) {
  pageSize.value = size
  reload(1)
}

function onRowClick(row: any) {
  emit('row-click', row)
}

function openForm(row?: any) {
  Object.keys(form).forEach((key) => delete form[key])
  props.fields.forEach((field) => {
    form[field.key] = field.default ?? null
  })
  if (row) {
    Object.assign(form, JSON.parse(JSON.stringify(row)))
  }
  drawer.value = true
}

async function submit() {
  for (const field of props.fields) {
    if (field.required && (form[field.key] === null || form[field.key] === undefined || form[field.key] === '')) {
      ElMessage.warning(`请填写${field.label}`)
      return
    }
  }
  saving.value = true
  try {
    const isEdit = Boolean(form.id)
    let payload: any = { ...form }
    if (props.beforeSubmit) {
      payload = props.beforeSubmit(payload, isEdit)
    }
    if (!isEdit) {
      await createOne(props.base, payload)
    } else if (props.updateMethod === 'put') {
      await updateOne(props.base, form.id, payload)
    } else {
      await createOne(props.base, payload)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm(
    `确认删除该${props.entity}？删除后不可恢复。`,
    '危险操作确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await removeOne(props.deletePath || props.base, row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)

defineExpose({ reload, openForm, rows, query })
</script>

<style scoped>
.crud-page {
  padding: var(--space-5) var(--space-6) var(--space-7);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  max-width: var(--content-max);
  margin: 0 auto;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 14px var(--space-5);
  border-bottom: 1px solid var(--line);
  background: var(--surface-sunken);
  flex-wrap: wrap;
}
.total-hint {
  font-size: 12px;
  color: var(--ink-400);
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 12px var(--space-5);
  border-top: 1px solid var(--line);
}
.money {
  font-variant-numeric: tabular-nums;
  color: var(--ink-900);
  font-weight: 550;
}
.badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  background: var(--brand-50);
  color: var(--brand-700);
  border: 1px solid var(--brand-100);
}
.badge.ok {
  background: #eef7f1;
  color: #2f7d55;
  border-color: #d6ebdd;
}
.badge.warn {
  background: #fdf5e8;
  color: #9a6a18;
  border-color: #f3e3c6;
}
.badge.danger {
  background: #fbeeed;
  color: #a83c37;
  border-color: #f1d7d5;
}
.badge.muted {
  background: #f4f3ef;
  color: var(--ink-500);
  border-color: var(--line);
}
.badge.info {
  background: #eef2f7;
  color: #4a5f7a;
  border-color: #dde5ef;
}
.empty-state {
  padding: 40px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--ink-400);
}
.empty-state p {
  margin: 0;
  color: var(--ink-700);
  font-weight: 550;
}
.empty-state span {
  font-size: 12px;
}
.crud-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.crud-form :deep(.full),
.crud-form :deep(.el-form-item.full) {
  grid-column: 1 / -1;
}
.field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-400);
}
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.fallback-field {
  color: var(--ink-400);
}
</style>
