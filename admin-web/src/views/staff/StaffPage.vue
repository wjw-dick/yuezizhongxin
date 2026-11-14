<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>人力与排班</h1>
        <p class="sub">人员档案与资质证件（到期提醒）→ 班次 → 一对一配比排班 → 打卡考勤 → 工作量统计</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll"><Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span></el-button>
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
      <el-tab-pane label="人员档案" name="staff">
        <CrudPage
          title="人员档案"
          subtitle="员工账号与档案绑定后可登录护理师端；离职为逻辑删除，历史记录保留"
          base="/api/tenant/staff"
          entity="人员"
          create-text="新增人员"
          :columns="staffColumns"
          :fields="staffFields"
          :filters="staffFilters"
          :deletable="false"
        />
      </el-tab-pane>

      <el-tab-pane name="cert">
        <template #label>
          资质证件
          <span v-if="expiring.length" class="tab-badge">{{ expiring.length }}</span>
        </template>
        <div class="card">
          <div class="toolbar">
            <el-select v-model="certStaffId" filterable placeholder="选择人员" style="width: 200px" @change="loadCerts">
              <el-option v-for="s in staffs" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-input v-model="certForm.certType" placeholder="证件类型，如 护士执业证" style="width: 200px" />
            <el-input v-model="certForm.certNo" placeholder="证件编号" style="width: 180px" />
            <el-date-picker v-model="certForm.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="到期日" style="width: 150px" />
            <el-button type="primary" @click="addCert">添加证件</el-button>
          </div>

          <el-table :data="certs" size="small" stripe style="margin-top: 14px">
            <el-table-column prop="certType" label="证件类型" width="180" />
            <el-table-column prop="certNo" label="证件编号" min-width="180" />
            <el-table-column prop="expireDate" label="到期日" width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <span class="badge" :class="certState(row).cls">{{ certState(row).text }}</span>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂未登记证件</strong>过期证件会被排班校验拦截</div></template>
          </el-table>

          <p class="section-label" style="margin-top: 20px">60 天内到期提醒</p>
          <el-table :data="expiring" size="small" stripe>
            <el-table-column prop="staffName" label="人员" width="130" />
            <el-table-column prop="jobType" label="岗位" width="130" />
            <el-table-column prop="certType" label="证件" min-width="160" />
            <el-table-column prop="expireDate" label="到期日" width="130" />
            <el-table-column prop="days" label="剩余天数" width="110" align="center" />
            <template #empty><div class="empty">没有即将到期的证件</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="班次定义" name="shift">
        <CrudPage
          title="班次定义"
          subtitle="跨天班次（如夜班 22:00-06:00）需标记跨天，交接及时率按班次结束时间计算"
          base="/api/tenant/staff/shifts"
          entity="班次"
          create-text="新增班次"
          :columns="shiftColumns"
          :fields="shiftFields"
          :creatable="true"
          :deletable="false"
          :show-actions="false"
        />
      </el-tab-pane>

      <el-tab-pane label="排班" name="schedule">
        <div class="card">
          <div class="toolbar">
            <el-button size="small" @click="shiftWeek(-7)">上一周</el-button>
            <span class="week-label mono">{{ weekStart }} ~ {{ weekEnd }}</span>
            <el-button size="small" @click="shiftWeek(7)">下一周</el-button>
            <span class="spacer" />
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="一对一配比：每条护理排班必须绑定在住订单；证件过期人员不可排班"
              style="max-width: 460px"
            />
            <el-button size="small" type="primary" :loading="saving" @click="saveSchedules">保存本周排班</el-button>
          </div>

          <div v-loading="loading" class="schedule-grid">
            <table class="sched-table">
              <thead>
                <tr>
                  <th class="sticky-col">人员</th>
                  <th v-for="day in weekDays" :key="day.date">
                    {{ day.label }}
                    <em>{{ day.date.slice(5) }}</em>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in staffs" :key="s.id">
                  <td class="sticky-col">
                    <strong>{{ s.name }}</strong>
                    <span class="muted">{{ s.jobType }}</span>
                  </td>
                  <td v-for="day in weekDays" :key="day.date">
                    <el-select
                      :model-value="cellValue(s.id, day.date)"
                      size="small"
                      placeholder="-"
                      clearable
                      @update:model-value="(v: string) => setCell(s.id, day.date, v)"
                    >
                      <el-option v-for="sh in shifts" :key="sh.id" :label="sh.shiftName" :value="sh.shiftName" />
                      <el-option label="休息" value="休息" />
                    </el-select>
                  </td>
                </tr>
                <tr v-if="!staffs.length"><td :colspan="8" class="empty-cell">还没有人员档案</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="考勤" name="attendance">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-date-picker v-model="attRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadAttendance" />
            <el-select v-model="attStaffId" filterable clearable placeholder="全部人员" size="small" style="width: 170px" @change="loadAttendance">
              <el-option v-for="s in staffs" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <span class="spacer" />
            <el-button size="small" @click="clockIn">代打卡</el-button>
          </div>
          <el-table v-loading="loading" :data="attendance" size="small" stripe>
            <el-table-column label="人员" width="130">
              <template #default="{ row }">{{ staffName(row.staffId) }}</template>
            </el-table-column>
            <el-table-column prop="attendanceDate" label="日期" width="120" />
            <el-table-column prop="checkInTime" label="上班打卡" min-width="150" />
            <el-table-column prop="checkOutTime" label="下班打卡" min-width="150" />
            <el-table-column prop="location" label="定位" min-width="160" />
            <el-table-column prop="leaveType" label="请假类型" width="110" />
            <template #empty><div class="empty"><strong>暂无考勤记录</strong>护理师端打卡后自动写入</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="工作量" name="workload">
        <div class="card">
          <div class="toolbar">
            <el-date-picker v-model="workRange" type="daterange" size="small" value-format="YYYY-MM-DD" start-placeholder="起" end-placeholder="止" style="width: 240px" @change="loadWorkload" />
            <span class="spacer" />
            <span class="muted">按护理记录与排班统计，用于绩效参考</span>
          </div>
          <div v-loading="loading" class="workload-grid">
            <div v-for="row in workload" :key="row.staffId" class="workload-card">
              <div class="wl-head">
                <strong>{{ row.staffName || staffName(row.staffId) }}</strong>
                <span class="muted">{{ row.jobType }}</span>
              </div>
              <div class="wl-bar">
                <span :style="{ width: `${Math.min(100, (Number(row.recordCount || 0) / maxWorkload) * 100)}%` }" />
              </div>
              <div class="wl-meta">
                <span>护理记录 <b>{{ row.recordCount || 0 }}</b></span>
                <span>排班 <b>{{ row.shiftCount || 0 }}</b> 班</span>
              </div>
            </div>
            <div v-if="!workload.length" class="empty card"><strong>暂无工作量数据</strong></div>
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

const tab = ref('staff')
const loading = ref(false)
const saving = ref(false)
const staffs = ref<any[]>([])
const shifts = ref<any[]>([])
const certs = ref<any[]>([])
const expiring = ref<any[]>([])
const attendance = ref<any[]>([])
const workload = ref<any[]>([])
const schedules = ref<any[]>([])
const weekStart = ref(startOfWeek(new Date()))
const weekEnd = ref(addDays(weekStart.value, 6))
const attRange = ref<string[] | null>(null)
const workRange = ref<string[] | null>(null)
const attStaffId = ref<number>()
const certStaffId = ref<number>()

function fmt(d: Date) {
  return d.toISOString().slice(0, 10)
}

function startOfWeek(d: Date) {
  const copy = new Date(d)
  const day = copy.getDay() || 7
  copy.setDate(copy.getDate() - day + 1)
  return fmt(copy)
}

function addDays(date: string, days: number) {
  const d = new Date(date)
  d.setDate(d.getDate() + days)
  return fmt(d)
}

const weekDays = computed(() => {
  const labels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return labels.map((label, i) => ({ label, date: addDays(weekStart.value, i) }))
})

const maxWorkload = computed(() => Math.max(1, ...workload.value.map((w) => Number(w.recordCount || 0))))

const stats = computed(() => [
  { label: '在职人员', value: staffs.value.filter((s) => s.status === '在职').length, hint: '含护理师与销售' },
  { label: '证件预警', value: expiring.value.length, hint: '60 天内到期' },
  { label: '本周排班', value: schedules.value.length, hint: '班次数' },
  { label: '本月记录', value: workload.value.reduce((sum, w) => sum + Number(w.recordCount || 0), 0), hint: '护理记录条数' }
])

const staffColumns = [
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'phone', label: '手机号', width: 140 },
  { prop: 'jobType', label: '岗位', width: 140 },
  { prop: 'userId', label: '绑定账号 ID', width: 120 },
  { prop: 'status', label: '状态', width: 100, type: 'status' as const },
  { prop: 'createdTime', label: '入职时间', min: 160 }
]

const staffFilters = [
  {
    key: 'jobType',
    label: '岗位',
    type: 'select',
    options: [
      { label: '护理师', value: '护理师' },
      { label: '护理主管', value: '护理主管' },
      { label: '康复治疗师', value: '康复治疗师' },
      { label: '销售顾问', value: '销售顾问' },
      { label: '营养师', value: '营养师' }
    ]
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '在职', value: '在职' },
      { label: '离职', value: '离职' }
    ]
  }
]

const staffFields = [
  { key: 'name', label: '姓名', required: true },
  { key: 'phone', label: '手机号', required: true },
  {
    key: 'jobType',
    label: '岗位',
    type: 'select',
    options: [
      { label: '护理师', value: '护理师' },
      { label: '护理主管', value: '护理主管' },
      { label: '康复治疗师', value: '康复治疗师' },
      { label: '销售顾问', value: '销售顾问' },
      { label: '营养师', value: '营养师' }
    ],
    default: '护理师'
  },
  { key: 'userId', label: '绑定登录账号 ID', type: 'number', hint: '绑定后可用该账号登录护理师端' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '在职', value: '在职' },
      { label: '离职', value: '离职' }
    ],
    default: '在职'
  }
]

const shiftColumns = [
  { prop: 'shiftName', label: '班次名称', width: 140 },
  { prop: 'startTime', label: '开始时间', width: 120 },
  { prop: 'endTime', label: '结束时间', width: 120 },
  { prop: 'crossDay', label: '跨天', width: 90, type: 'bool' as const }
]

const shiftFields = [
  { key: 'shiftName', label: '班次名称', required: true },
  { key: 'startTime', label: '开始时间', placeholder: '08:00' },
  { key: 'endTime', label: '结束时间', placeholder: '16:00' },
  { key: 'crossDay', label: '是否跨天', type: 'switch' }
]

function staffName(id: number) {
  return staffs.value.find((s) => s.id === id)?.name || (id ? `#${id}` : '-')
}

function certState(row: any) {
  if (!row.expireDate) return { text: '长期有效', cls: 'muted' }
  const days = Math.round((new Date(row.expireDate).getTime() - Date.now()) / 86400000)
  if (days < 0) return { text: '已过期', cls: 'danger' }
  if (days <= 60) return { text: `${days} 天后到期`, cls: 'warn' }
  return { text: '有效', cls: 'ok' }
}

async function loadStaffs() {
  const res: any = await tenantHttp.get('/api/tenant/staff')
  staffs.value = res.data || []
}

async function loadShifts() {
  const res: any = await tenantHttp.get('/api/tenant/staff/shifts')
  shifts.value = res.data || []
}

async function loadCerts() {
  if (!certStaffId.value) {
    certs.value = []
    return
  }
  const res: any = await tenantHttp.get(`/api/tenant/staff/${certStaffId.value}/certs`)
  certs.value = res.data || []
}

async function loadExpiring() {
  const res: any = await tenantHttp.get('/api/tenant/staff/certs/expiring', { params: { days: 60 } })
  expiring.value = res.data || []
}

const certForm = reactive<any>({ certType: '', certNo: '', expireDate: null })

async function addCert() {
  if (!certStaffId.value || !certForm.certType) {
    ElMessage.warning('请选择人员并填写证件类型')
    return
  }
  await tenantHttp.post('/api/tenant/staff/certs', { staffId: certStaffId.value, ...certForm })
  ElMessage.success('证件已登记')
  certForm.certType = ''
  certForm.certNo = ''
  certForm.expireDate = null
  await Promise.all([loadCerts(), loadExpiring()])
}

/* 排班 */
async function loadSchedules() {
  const res: any = await tenantHttp.get('/api/tenant/staff/schedules', {
    params: { from: weekStart.value, to: weekEnd.value }
  })
  schedules.value = res.data || []
}

function cellValue(staffId: number, date: string) {
  return schedules.value.find((s) => s.staffId === staffId && s.scheduleDate === date)?.shiftType || ''
}

function setCell(staffId: number, date: string, shiftType: string) {
  const index = schedules.value.findIndex((s) => s.staffId === staffId && s.scheduleDate === date)
  if (!shiftType) {
    if (index >= 0) schedules.value.splice(index, 1)
    return
  }
  if (index >= 0) {
    schedules.value[index].shiftType = shiftType
  } else {
    schedules.value.push({ staffId, scheduleDate: date, shiftType })
  }
}

function shiftWeek(days: number) {
  weekStart.value = addDays(weekStart.value, days)
  weekEnd.value = addDays(weekStart.value, 6)
  loadSchedules()
}

async function saveSchedules() {
  if (!schedules.value.length) {
    ElMessage.warning('本周还没有排班内容')
    return
  }
  saving.value = true
  try {
    const res: any = await tenantHttp.post('/api/tenant/staff/schedules', schedules.value)
    ElMessage.success(`已保存 ${res.data || 0} 条排班（配比与证件校验通过）`)
    loadSchedules()
  } finally {
    saving.value = false
  }
}

/* 考勤 */
async function loadAttendance() {
  const res: any = await tenantHttp.get('/api/tenant/staff/attendance', {
    params: {
      from: attRange.value?.[0],
      to: attRange.value?.[1],
      staffId: attStaffId.value
    }
  })
  attendance.value = res.data || []
}

async function clockIn() {
  if (!staffs.value.length) {
    ElMessage.warning('请先创建人员档案')
    return
  }
  const options = staffs.value.map((s) => `${s.id}:${s.name}`)
  const { value } = await ElMessageBox.prompt(`输入「人员ID:姓名」代打卡，可选：${options.slice(0, 5).join('、')}`, '代打卡', {
    inputPlaceholder: options[0]
  })
  const [staffId, type] = String(value).split(':')
  await tenantHttp.post('/api/tenant/staff/attendance', { staffId, type: type ? 'out' : 'in', location: '后台代打卡' })
  ElMessage.success('打卡已记录')
  loadAttendance()
}

/* 工作量 */
async function loadWorkload() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/staff/workload', {
      params: { from: workRange.value?.[0], to: workRange.value?.[1] }
    })
    workload.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadStaffs(), loadShifts()])
  await Promise.all([loadExpiring(), loadSchedules(), loadAttendance(), loadWorkload()])
}

function onTab(name: string) {
  if (name === 'cert') loadExpiring()
  if (name === 'schedule') loadSchedules()
  if (name === 'attendance') loadAttendance()
  if (name === 'workload') loadWorkload()
}

onMounted(async () => {
  await loadAll()
  const first = staffs.value[0]
  if (first) {
    certStaffId.value = first.id
    loadCerts()
  }
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
.tab-badge { display: inline-block; margin-left: 6px; padding: 0 5px; height: 16px; line-height: 16px; border-radius: 8px; background: #fdf5e8; color: #9a6a18; font-size: 10.5px; }
.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }
.section-label { font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.week-label { font-size: 12.5px; color: var(--ink-600); }

.schedule-grid { margin-top: 14px; overflow-x: auto; }
.sched-table { width: 100%; border-collapse: separate; border-spacing: 0; font-size: 12.5px; }
.sched-table th { background: var(--surface-sunken); padding: 10px 8px; font-size: 11.5px; font-weight: 600; color: var(--ink-500); border-bottom: 1px solid var(--line); text-align: left; }
.sched-table th em { display: block; font-style: normal; font-size: 10.5px; color: var(--ink-400); font-weight: 400; }
.sched-table td { padding: 7px 8px; border-bottom: 1px solid var(--line); }
.sticky-col { position: sticky; left: 0; background: var(--surface); z-index: 1; min-width: 130px; }
.sched-table th.sticky-col { background: var(--surface-sunken); }
.sticky-col span { display: block; font-size: 11px; }
.empty-cell { text-align: center; color: var(--ink-400); padding: 30px 0; }

.workload-grid { margin-top: 14px; display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: var(--space-3); }
.workload-card { border: 1px solid var(--line); border-radius: var(--radius-md); padding: 12px 14px; }
.wl-head { display: flex; align-items: baseline; justify-content: space-between; }
.wl-head strong { font-size: 13.5px; }
.wl-head span { font-size: 11.5px; }
.wl-bar { margin: 10px 0 8px; height: 6px; border-radius: 999px; background: var(--line); overflow: hidden; }
.wl-bar span { display: block; height: 100%; background: linear-gradient(90deg, #4fae8d, #2f8b70); border-radius: 999px; }
.wl-meta { display: flex; justify-content: space-between; font-size: 11.5px; color: var(--ink-500); }
.wl-meta b { color: var(--ink-900); }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
