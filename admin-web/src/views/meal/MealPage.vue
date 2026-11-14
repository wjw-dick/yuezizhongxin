<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>月子餐</h1>
        <p class="sub">按阶段模板生成每日六餐，忌口与过敏原强校验，按房间汇总出餐份数与特殊标记</p>
      </div>
      <div class="actions">
        <el-date-picker v-model="mealDate" type="date" value-format="YYYY-MM-DD" size="default" style="width: 150px" @change="loadDay" />
        <el-button type="primary" @click="generate">
          <Icon name="sparkle" :size="15" /><span style="margin-left: 6px">生成当日餐单</span>
        </el-button>
      </div>
    </div>

    <div class="stat-row">
      <div v-for="item in stats" :key="item.label" class="stat">
        <span class="stat-label">{{ item.label }}</span>
        <strong class="stat-value">{{ item.value }}</strong>
        <em class="stat-hint">{{ item.hint }}</em>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs" @tab-change="onTab">
      <el-tab-pane label="按房间出餐" name="summary">
        <div v-loading="loading" class="room-grid">
          <article v-for="room in summary" :key="room.roomId" class="room-card">
            <div class="room-head">
              <strong>{{ room.roomNo }}</strong>
              <span class="count">{{ room.count }} 份</span>
            </div>
            <ul v-if="room.special?.length" class="special-list">
              <li v-for="(tag, i) in room.special" :key="i">{{ tag }}</li>
            </ul>
            <p v-else class="muted">无特殊标记</p>
          </article>
          <div v-if="!summary.length" class="empty card">
            <strong>当日还没有出餐记录</strong>点击右上角「生成当日餐单」按在住订单批量生成
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="出餐明细" name="orders">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-select v-model="checkedFilter" placeholder="全部" clearable size="small" style="width: 130px" @change="loadMeals">
              <el-option label="已核对" :value="1" />
              <el-option label="未核对" :value="0" />
            </el-select>
            <span class="spacer" />
            <span class="muted">共 {{ meals.length }} 条</span>
          </div>
          <el-table v-loading="loading" :data="meals" size="small" stripe>
            <el-table-column label="房间" width="100">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column label="订单" width="170">
              <template #default="{ row }"><span class="mono">{{ orderNo(row.orderId) }}</span></template>
            </el-table-column>
            <el-table-column prop="mealType" label="餐次" width="100" />
            <el-table-column prop="dishes" label="餐单" min-width="240" />
            <el-table-column label="特殊标记" width="140">
              <template #default="{ row }">
                <span v-if="row.specialTag" class="tag-warn">{{ row.specialTag }}</span>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="加餐费" width="100" align="right">
              <template #default="{ row }">¥{{ money(row.extraCharge) }}</template>
            </el-table-column>
            <el-table-column label="核对" width="100">
              <template #default="{ row }">
                <span class="badge" :class="row.checked ? 'ok' : 'warn'">{{ row.checked ? '已核对' : '待核对' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.checked" link type="primary" @click="check(row)">核对</el-button>
                <el-button v-if="row.abnormal" link type="danger" @click="showAbnormal(row)">异常</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>当日无出餐记录</strong></div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="阶段餐单模板" name="template">
        <CrudPage
          title="阶段餐单模板"
          subtitle="按产后阶段与天数维护六餐标准；忌口标签用于生成时的强校验"
          base="/api/tenant/meals/templates"
          entity="餐单"
          create-text="新增餐单"
          :columns="templateColumns"
          :fields="templateFields"
          :filters="templateFilters"
          :deletable="false"
        />
      </el-tab-pane>

      <el-tab-pane label="忌口与过敏原" name="restrict">
        <div class="card">
          <div class="toolbar">
            <el-select v-model="restrictOrderId" filterable placeholder="选择在住订单" style="width: 280px" @change="loadRestricts">
              <el-option v-for="o in livingOrders" :key="o.id" :label="`${o.customerName}（${o.orderNo}）`" :value="o.id" />
            </el-select>
            <el-input v-model="restrictForm.restrictType" placeholder="类型，如 过敏 / 忌口" style="width: 160px" />
            <el-input v-model="restrictForm.content" placeholder="内容，如 海鲜 / 花生" style="width: 200px" />
            <el-button type="primary" @click="addRestrict">添加</el-button>
          </div>

          <el-table :data="restricts" size="small" stripe style="margin-top: 14px">
            <el-table-column prop="restrictType" label="类型" width="140" />
            <el-table-column prop="content" label="内容" min-width="200" />
            <el-table-column prop="createdTime" label="登记时间" width="180" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeRestrict(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>该订单暂无忌口登记</strong>生成当日餐单时若菜品命中忌口会直接拦截</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="加餐申请" name="addon">
        <div class="card">
          <el-form :model="addonForm" label-position="top" class="addon-form">
            <el-form-item label="订单" required>
              <el-select v-model="addonForm.orderId" filterable style="width: 100%">
                <el-option v-for="o in livingOrders" :key="o.id" :label="`${o.customerName}（${o.orderNo}）`" :value="o.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期" required>
              <el-date-picker v-model="addonForm.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="餐次">
              <el-select v-model="addonForm.mealType" style="width: 100%">
                <el-option v-for="t in mealTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="加餐内容" class="full">
              <el-input v-model="addonForm.dishes" type="textarea" :rows="2" placeholder="如：红枣桂圆汤 + 小米粥" />
            </el-form-item>
            <el-form-item label="收费金额（元）">
              <el-input-number v-model="addonForm.charge" :min="0" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-form>
          <div class="addon-foot">
            <el-button type="primary" :loading="saving" @click="submitAddon">提交加餐</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const tab = ref('summary')
const loading = ref(false)
const saving = ref(false)
const mealDate = ref(today())
const summary = ref<any[]>([])
const meals = ref<any[]>([])
const restricts = ref<any[]>([])
const livingOrders = ref<any[]>([])
const rooms = ref<any[]>([])
const checkedFilter = ref<number | undefined>(undefined)

const mealTypes = ['早餐', '早加餐', '午餐', '午加餐', '晚餐', '晚加餐']

function today() {
  return new Date().toISOString().slice(0, 10)
}

const stats = computed(() => {
  const total = meals.value.length
  const done = meals.value.filter((m) => m.checked).length
  const special = meals.value.filter((m) => m.specialTag).length
  const roomsCount = new Set(meals.value.map((m) => m.roomId)).size
  return [
    { label: '出餐份数', value: total, hint: mealDate.value },
    { label: '已核对', value: `${done}/${total}`, hint: total ? `完成 ${Math.round((done / total) * 100)}%` : '暂无' },
    { label: '特殊标记', value: special, hint: '忌口与过敏原' },
    { label: '覆盖房间', value: roomsCount, hint: '按房间汇总' }
  ]
})

const templateColumns = [
  { prop: 'stage', label: '阶段', width: 130 },
  { prop: 'dayIndex', label: '第几天', width: 90 },
  { prop: 'mealType', label: '餐次', width: 100 },
  { prop: 'dishes', label: '餐单', min: 260 },
  { prop: 'avoidTags', label: '避用标签', min: 140 }
]

const templateFields = [
  {
    key: 'stage',
    label: '阶段',
    type: 'select',
    options: [
      { label: '产后 0-7 天（排恶露）', value: '第一阶段' },
      { label: '产后 8-14 天（修复）', value: '第二阶段' },
      { label: '产后 15-28 天（滋补）', value: '第三阶段' },
      { label: '产后 29 天以后（调养）', value: '第四阶段' }
    ],
    default: '第一阶段'
  },
  { key: 'dayIndex', label: '第几天', type: 'number', default: 1 },
  { key: 'mealType', label: '餐次', type: 'select', options: mealTypes.map((t) => ({ label: t, value: t })), default: '早餐' },
  { key: 'dishes', label: '餐单', type: 'textarea', required: true, full: true },
  { key: 'avoidTags', label: '避用标签', placeholder: '如：海鲜,花生,辛辣', full: true }
]

const templateFilters = [
  {
    key: 'stage',
    label: '阶段',
    type: 'select',
    options: [
      { label: '第一阶段', value: '第一阶段' },
      { label: '第二阶段', value: '第二阶段' },
      { label: '第三阶段', value: '第三阶段' },
      { label: '第四阶段', value: '第四阶段' }
    ]
  }
]

function money(v: any) {
  if (!v) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function roomName(id: number) {
  return rooms.value.find((r) => r.id === id)?.roomNo || (id ? `#${id}` : '未分配')
}

function orderNo(id: number) {
  return livingOrders.value.find((o) => o.id === id)?.orderNo || `#${id}`
}

async function loadDay() {
  loading.value = true
  try {
    const [sum, list] = await Promise.all([
      tenantHttp.get('/api/tenant/meals/summary', { params: { date: mealDate.value } }),
      tenantHttp.get('/api/tenant/meals/orders', {
        params: { date: mealDate.value, checked: checkedFilter.value }
      })
    ])
    summary.value = (sum as any).data || []
    meals.value = (list as any).data || []
  } finally {
    loading.value = false
  }
}

function loadMeals() {
  loadDay()
}

async function generate() {
  await ElMessageBox.confirm(
    `将按 ${mealDate.value} 在住订单与阶段模板生成六餐，命中忌口会中断生成。确认继续？`,
    '生成餐单',
    { type: 'info' }
  )
  const res: any = await tenantHttp.post('/api/tenant/meals/generate', null, { params: { date: mealDate.value } })
  ElMessage.success(`已生成 ${res.data || 0} 条出餐记录`)
  loadDay()
}

async function check(row: any) {
  await tenantHttp.put(`/api/tenant/meals/orders/${row.id}/check`, { abnormal: '' })
  ElMessage.success('已核对')
  loadDay()
}

function showAbnormal(row: any) {
  ElMessageBox.alert(row.abnormal, '出餐异常', { type: 'warning' })
}

/* 忌口 */
const restrictOrderId = ref<number>()
const restrictForm = reactive<any>({ restrictType: '忌口', content: '' })

async function loadRestricts() {
  if (!restrictOrderId.value) {
    restricts.value = []
    return
  }
  const res: any = await tenantHttp.get('/api/tenant/meals/restricts', { params: { orderId: restrictOrderId.value } })
  restricts.value = res.data || []
}

async function addRestrict() {
  if (!restrictOrderId.value || !restrictForm.content) {
    ElMessage.warning('请选择订单并填写忌口内容')
    return
  }
  await tenantHttp.post('/api/tenant/meals/restricts', {
    orderId: restrictOrderId.value,
    restrictType: restrictForm.restrictType,
    content: restrictForm.content
  })
  ElMessage.success('已添加')
  restrictForm.content = ''
  loadRestricts()
}

async function removeRestrict(row: any) {
  await ElMessageBox.confirm('确认删除该忌口记录？', '删除忌口', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/meals/restricts/${row.id}`)
  ElMessage.success('已删除')
  loadRestricts()
}

/* 加餐 */
const addonForm = reactive<any>({})

async function submitAddon() {
  if (!addonForm.orderId) {
    ElMessage.warning('请选择订单')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/meals/add-on', { ...addonForm })
    ElMessage.success('加餐已提交并计入订单增值消费')
    addonForm.dishes = ''
    addonForm.charge = 0
    loadDay()
  } finally {
    saving.value = false
  }
}

async function loadRefs() {
  const [orders, roomRes] = await Promise.all([
    tenantHttp.get('/api/tenant/orders', { params: { status: '在住', pageNum: 1, pageSize: 100 } }),
    tenantHttp.get('/api/tenant/rooms')
  ])
  livingOrders.value = (orders as any).data?.records || []
  rooms.value = (roomRes as any).data?.records || (roomRes as any).data || []
}

function onTab(name: string) {
  if (name === 'summary' || name === 'orders') loadDay()
  if (name === 'restrict') loadRestricts()
}

onMounted(async () => {
  await loadRefs()
  addonForm.date = today()
  addonForm.mealType = '午加餐'
  addonForm.charge = 0
  loadDay()
})
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-3); }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; box-shadow: var(--shadow-xs); }
.stat-label { font-size: 11.5px; color: var(--ink-400); }
.stat-value { font-size: 22px; font-weight: 650; font-variant-numeric: tabular-nums; }
.stat-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }
.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.biz-tabs :deep(.crud-page) { padding-left: 0; padding-right: 0; max-width: none; }
.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }

.room-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: var(--space-3); }
.room-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 13px 14px; box-shadow: var(--shadow-xs); }
.room-head { display: flex; align-items: center; justify-content: space-between; }
.room-head strong { font-size: 15px; }
.count { font-size: 12px; color: var(--brand-700); background: var(--brand-50); border-radius: 999px; padding: 2px 9px; }
.special-list { list-style: none; margin: 10px 0 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
.special-list li { font-size: 11.5px; color: #a8672c; background: #fdf5e8; border-radius: 4px; padding: 2px 7px; }
.room-card p { margin: 10px 0 0; font-size: 11.5px; }

.tag-warn { font-size: 11.5px; color: #a8672c; background: #fdf5e8; border-radius: 4px; padding: 2px 7px; }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.addon-form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; max-width: 720px; }
.addon-form :deep(.full) { grid-column: 1 / -1; }
.addon-foot { max-width: 720px; display: flex; justify-content: flex-end; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
