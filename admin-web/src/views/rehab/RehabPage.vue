<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>产后康复</h1>
        <p class="sub">入馆评估 → 疗程卡 → 预约排程（治疗师与康复室冲突校验）→ 执行扣次；出馆后仍可延续预约</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
        <el-button type="primary" @click="openAppoint()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新建预约</span>
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
      <el-tab-pane label="项目库" name="item">
        <CrudPage
          title="康复项目"
          subtitle="项目、适用阶段、单次时长、疗程次数与价格；人员资质要求会在排程时提示"
          base="/api/tenant/rehab/items"
          entity="项目"
          create-text="新增项目"
          :columns="itemColumns"
          :fields="itemFields"
          :deletable="false"
        />
      </el-tab-pane>

      <el-tab-pane label="康复评估" name="assess">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <span class="muted">评估结果与疗程卡绑定，可在小程序「康复」中查看</span>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openAssess">新增评估</el-button>
          </div>
          <el-table v-loading="loading" :data="assesses" size="small" stripe>
            <el-table-column prop="assessTime" label="评估时间" width="160" />
            <el-table-column label="订单" width="170">
              <template #default="{ row }"><span class="mono">{{ orderNo(row.orderId) }}</span></template>
            </el-table-column>
            <el-table-column prop="pelvicScore" label="盆底肌评分" width="110" align="center" />
            <el-table-column prop="diastasisCm" label="腹直肌分离(cm)" width="130" align="center" />
            <el-table-column prop="moodScore" label="情绪评分" width="100" align="center" />
            <el-table-column prop="posture" label="体态问题" min-width="140" />
            <el-table-column prop="report" label="评估报告" min-width="220" />
            <template #empty><div class="empty"><strong>暂无评估</strong>入馆 48 小时内建议完成首次评估</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="疗程卡" name="course">
        <div class="card flush">
          <table class="plain-table">
            <thead>
              <tr>
                <th>订单</th>
                <th>项目</th>
                <th class="num">总次数</th>
                <th class="num">已用</th>
                <th class="num">剩余</th>
                <th>有效期至</th>
                <th>来源</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in courses" :key="c.id">
                <td class="mono">{{ orderNo(c.orderId) }}</td>
                <td>{{ c.itemName || c.itemId }}</td>
                <td class="num">{{ c.totalTimes }}</td>
                <td class="num">{{ c.usedTimes }}</td>
                <td class="num">
                  <span class="badge" :class="Number(c.remain) > 0 ? 'ok' : 'muted'">{{ c.remain }}</span>
                </td>
                <td>{{ c.expireDate || '不限' }}</td>
                <td>{{ c.source || '-' }}</td>
              </tr>
              <tr v-if="!courses.length"><td colspan="7" class="empty-cell">暂无疗程卡，可在入馆后为订单生成</td></tr>
            </tbody>
          </table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="预约排程" name="appoint">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-date-picker v-model="appointDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 150px" @change="loadAppoints" />
            <el-button size="small" text @click="appointDate = today(); loadAppoints()">今天</el-button>
            <span class="spacer" />
            <span class="muted">共 {{ appoints.length }} 个预约</span>
          </div>
          <el-table v-loading="loading" :data="appoints" size="small" stripe>
            <el-table-column prop="startTime" label="时间" width="130">
              <template #default="{ row }"><span class="mono">{{ row.startTime }} - {{ row.endTime }}</span></template>
            </el-table-column>
            <el-table-column label="客户订单" width="170">
              <template #default="{ row }"><span class="mono">{{ orderNo(row.orderId) }}</span></template>
            </el-table-column>
            <el-table-column label="项目" min-width="140">
              <template #default="{ row }">{{ courseName(row.courseId) }}</template>
            </el-table-column>
            <el-table-column label="治疗师" width="110">
              <template #default="{ row }">{{ staffName(row.therapistId) }}</template>
            </el-table-column>
            <el-table-column label="康复室" width="100">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="appointClass(row.status)">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === '待确认'" link type="primary" @click="checkin(row)">签到</el-button>
                <el-button v-if="['待确认', '已签到'].includes(row.status)" link type="primary" @click="execute(row)">执行扣次</el-button>
                <el-button v-if="!['已完成', '已取消'].includes(row.status)" link type="danger" @click="cancel(row)">取消</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>当日无作业安排</strong>同一治疗师与康复室时段冲突会被拦截</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="执行记录" name="record">
        <div class="card flush">
          <el-table v-loading="loading" :data="records" size="small" stripe>
            <el-table-column prop="createdTime" label="执行时间" width="160" />
            <el-table-column prop="itemName" label="项目" min-width="150" />
            <el-table-column prop="durationMin" label="时长(分)" width="100" align="center" />
            <el-table-column label="治疗师" width="110">
              <template #default="{ row }">{{ staffName(row.therapistId) }}</template>
            </el-table-column>
            <el-table-column prop="feedback" label="反馈" min-width="220" />
            <el-table-column prop="nextAppoint" label="下次预约" width="120" />
            <template #empty><div class="empty"><strong>暂无执行记录</strong></div></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 评估 -->
    <el-dialog v-model="assessDialog" title="康复评估" width="560px">
      <el-form :model="assessForm" label-position="top" class="two-col">
        <el-form-item label="订单" required>
          <el-select v-model="assessForm.orderId" filterable style="width: 100%">
            <el-option v-for="o in orders" :key="o.id" :label="`${o.customerName}（${o.orderNo}）`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盆底肌评分（0~100）"><el-input-number v-model="assessForm.pelvicScore" :min="0" :max="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="腹直肌分离（cm）"><el-input-number v-model="assessForm.diastasisCm" :min="0" :max="10" :precision="1" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="情绪评分（0~10）"><el-input-number v-model="assessForm.moodScore" :min="0" :max="10" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="体态问题" class="full"><el-input v-model="assessForm.posture" placeholder="如：骨盆前倾 / 圆肩" /></el-form-item>
        <el-form-item label="评估报告" class="full"><el-input v-model="assessForm.report" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAssess">保存评估</el-button>
      </template>
    </el-dialog>

    <!-- 预约 -->
    <el-dialog v-model="appointDialog" title="新建康复预约" width="560px">
      <el-form :model="appointForm" label-position="top" class="two-col">
        <el-form-item label="疗程卡" required>
          <el-select v-model="appointForm.courseId" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="`${orderNo(c.orderId)} · ${c.itemName || c.itemId}（剩 ${c.remain} 次）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" required>
          <el-date-picker v-model="appointForm.appointDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-select v-model="appointForm.startTime" start="08:00" step="00:30" end="20:00" style="width: 100%" @change="syncEnd" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-input v-model="appointForm.endTime" placeholder="按项目时长自动计算" />
        </el-form-item>
        <el-form-item label="治疗师">
          <el-select v-model="appointForm.therapistId" clearable style="width: 100%">
            <el-option v-for="s in therapists" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="康复室">
          <el-select v-model="appointForm.roomId" clearable style="width: 100%">
            <el-option v-for="r in rehabRooms" :key="r.id" :label="r.roomNo" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appointDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAppoint">提交预约</el-button>
      </template>
    </el-dialog>

    <!-- 执行 -->
    <el-dialog v-model="execDialog" title="执行并扣次" width="520px">
      <el-form :model="execForm" label-position="top">
        <el-form-item label="执行时长（分钟）"><el-input-number v-model="execForm.durationMin" :min="1" :max="180" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="反馈"><el-input v-model="execForm.feedback" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="下次预约日期"><el-date-picker v-model="execForm.nextAppoint" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="execDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitExec">确认执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const tab = ref('item')
const loading = ref(false)
const saving = ref(false)
const items = ref<any[]>([])
const courses = ref<any[]>([])
const assesses = ref<any[]>([])
const appoints = ref<any[]>([])
const records = ref<any[]>([])
const orders = ref<any[]>([])
const staffs = ref<any[]>([])
const rooms = ref<any[]>([])
const appointDate = ref(today())

function today() {
  return new Date().toISOString().slice(0, 10)
}

const therapists = computed(() => staffs.value.filter((s) => (s.jobType || '').includes('康复') || (s.jobType || '').includes('治疗')))
const rehabRooms = computed(() => rooms.value.filter((r) => (r.roomNo || '').startsWith('康复') || r.roomTypeId))

const stats = computed(() => {
  const remain = courses.value.reduce((sum, c) => sum + Number(c.remain || 0), 0)
  return [
    { label: '康复项目', value: items.value.length, hint: '在售项目数' },
    { label: '疗程余额', value: remain, hint: '全租户剩余次数' },
    { label: '今日预约', value: appoints.value.length, hint: appointDate.value },
    { label: '已完成', value: appoints.value.filter((a) => a.status === '已完成').length, hint: '今日已执行' }
  ]
})

const itemColumns = [
  { prop: 'name', label: '项目名称', min: 150 },
  { prop: 'stage', label: '适用阶段', width: 120 },
  { prop: 'durationMin', label: '单次时长', width: 100 },
  { prop: 'courseTimes', label: '疗程次数', width: 100 },
  { prop: 'price', label: '价格', width: 120, type: 'money' as const },
  { prop: 'qualification', label: '资质要求', min: 140 },
  { prop: 'status', label: '状态', width: 100, type: 'status' as const }
]

const itemFields = [
  { key: 'name', label: '项目名称', required: true },
  {
    key: 'stage',
    label: '适用阶段',
    type: 'select',
    options: [
      { label: '产后 0-7 天', value: '产后初期' },
      { label: '产后 8-28 天', value: '产褥期' },
      { label: '产后 29-42 天', value: '恢复期' },
      { label: '产后 42 天以上', value: '巩固期' }
    ],
    default: '产褥期'
  },
  { key: 'durationMin', label: '单次时长（分钟）', type: 'number', default: 45 },
  { key: 'courseTimes', label: '疗程次数', type: 'number', default: 10 },
  { key: 'price', label: '价格（元）', type: 'money' },
  { key: 'qualification', label: '人员资质要求', placeholder: '如：康复治疗师资格证' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' }
    ],
    default: 'ENABLED'
  }
]

function orderNo(id: number) {
  return orders.value.find((o) => o.id === id)?.orderNo || `#${id}`
}

function staffName(id: number) {
  return staffs.value.find((s) => s.id === id)?.name || (id ? `#${id}` : '-')
}

function roomName(id: number) {
  return rooms.value.find((r) => r.id === id)?.roomNo || (id ? `#${id}` : '-')
}

function courseName(id: number) {
  const course = courses.value.find((c) => c.id === id)
  if (!course) return id ? `疗程 #${id}` : '-'
  return course.itemName || `项目 #${course.itemId}`
}

function appointClass(status: string) {
  return { 已完成: 'ok', 已签到: 'info', 待确认: 'warn', 已取消: 'muted' }[status] || 'info'
}

async function loadItems() {
  const res: any = await tenantHttp.get('/api/tenant/rehab/items')
  items.value = res.data || []
}

async function loadCourses() {
  const res: any = await tenantHttp.get('/api/tenant/rehab/courses')
  courses.value = res.data || []
}

async function loadAssesses() {
  const res: any = await tenantHttp.get('/api/tenant/rehab/assesses')
  assesses.value = res.data || []
}

async function loadAppoints() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/rehab/appointments', { params: { date: appointDate.value } })
    appoints.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadRecords() {
  const first = orders.value[0]
  if (!first) {
    records.value = []
    return
  }
  const res: any = await tenantHttp.get('/api/tenant/rehab/records', { params: { orderId: first.id } })
  records.value = res.data || []
}

async function loadRefs() {
  const [orderRes, staffRes, roomRes] = await Promise.all([
    tenantHttp.get('/api/tenant/orders', { params: { pageNum: 1, pageSize: 100 } }),
    tenantHttp.get('/api/tenant/staff'),
    tenantHttp.get('/api/tenant/rooms')
  ])
  orders.value = (orderRes as any).data?.records || []
  staffs.value = (staffRes as any).data || []
  rooms.value = (roomRes as any).data?.records || (roomRes as any).data || []
}

/* 评估 */
const assessDialog = ref(false)
const assessForm = reactive<any>({})

function openAssess() {
  Object.keys(assessForm).forEach((k) => delete assessForm[k])
  Object.assign(assessForm, { pelvicScore: 60, diastasisCm: 2, moodScore: 8 })
  assessDialog.value = true
}

async function saveAssess() {
  if (!assessForm.orderId) {
    ElMessage.warning('请选择订单')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/rehab/assesses', { ...assessForm })
    ElMessage.success('评估已保存')
    assessDialog.value = false
    loadAssesses()
  } finally {
    saving.value = false
  }
}

/* 预约 */
const appointDialog = ref(false)
const appointForm = reactive<any>({})

function openAppoint() {
  Object.keys(appointForm).forEach((k) => delete appointForm[k])
  Object.assign(appointForm, { appointDate: appointDate.value, startTime: '09:00', endTime: '09:45' })
  appointDialog.value = true
}

function syncEnd() {
  const course = courses.value.find((c) => c.id === appointForm.courseId)
  const minutes = Number(course?.durationMin || 45)
  const [h, m] = String(appointForm.startTime || '09:00').split(':').map(Number)
  const total = h * 60 + m + minutes
  appointForm.endTime = `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

async function saveAppoint() {
  if (!appointForm.courseId || !appointForm.appointDate) {
    ElMessage.warning('请选择疗程卡与日期')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/rehab/appointments', { ...appointForm })
    ElMessage.success('预约已创建')
    appointDialog.value = false
    await Promise.all([loadAppoints(), loadCourses()])
  } finally {
    saving.value = false
  }
}

async function checkin(row: any) {
  await tenantHttp.post(`/api/tenant/rehab/appointments/${row.id}/checkin`)
  ElMessage.success('已签到')
  loadAppoints()
}

async function cancel(row: any) {
  await ElMessageBox.confirm('确认取消该康复预约？', '取消预约', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/rehab/appointments/${row.id}/cancel`)
  ElMessage.success('已取消')
  loadAppoints()
}

/* 执行 */
const execDialog = ref(false)
const execForm = reactive<any>({})
let execId = 0

function execute(row: any) {
  execId = row.id
  Object.keys(execForm).forEach((k) => delete execForm[k])
  Object.assign(execForm, { durationMin: 45 })
  execDialog.value = true
}

async function submitExec() {
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/rehab/appointments/${execId}/execute`, { ...execForm })
    ElMessage.success('执行完成，疗程卡已扣次')
    execDialog.value = false
    await Promise.all([loadAppoints(), loadCourses(), loadRecords()])
  } finally {
    saving.value = false
  }
}

function loadAll() {
  loadItems()
  loadCourses()
  loadAssesses()
  loadAppoints()
  loadRecords()
}

function onTab(name: string) {
  if (name === 'assess') loadAssesses()
  if (name === 'course') loadCourses()
  if (name === 'appoint') loadAppoints()
  if (name === 'record') loadRecords()
}

onMounted(async () => {
  await loadRefs()
  loadAll()
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
.plain-table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.plain-table th { text-align: left; font-weight: 600; font-size: 11px; letter-spacing: 0.04em; text-transform: uppercase; color: var(--ink-500); background: var(--surface-sunken); padding: 10px 14px; }
.plain-table td { padding: 11px 14px; border-top: 1px solid var(--line); }
.plain-table .num { text-align: right; font-variant-numeric: tabular-nums; }
.empty-cell { text-align: center; color: var(--ink-400); padding: 30px 0; }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.badge.info { background: #eef2f7; color: #4a5f7a; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
</style>
