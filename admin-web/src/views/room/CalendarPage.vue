<template>
  <div class="calendar-page">
    <div class="page-head">
      <div>
        <h1>档期日历</h1>
        <p class="sub">房间 × 日期矩阵，颜色区分五态；点击格子可锁定或释放档期</p>
      </div>
      <div class="actions">
        <el-button size="small" @click="shift(-14)">前 14 天</el-button>
        <el-button size="small" @click="resetRange">今天</el-button>
        <el-button size="small" @click="shift(14)">后 14 天</el-button>
      </div>
    </div>

    <div class="card flush">
      <div class="filters">
        <el-select v-model="floor" placeholder="全部楼层" clearable size="small" style="width: 130px" @change="load">
          <el-option v-for="f in floors" :key="f" :label="f" :value="f" />
        </el-select>
        <el-select v-model="roomTypeId" placeholder="全部房型" clearable size="small" style="width: 150px" @change="load">
          <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
        <span class="range">{{ from }} 至 {{ to }}</span>
        <span class="spacer" />
        <div class="legend">
          <span v-for="item in legends" :key="item.state" class="legend-item">
            <i :style="{ background: item.color }" />{{ item.label }}
          </span>
        </div>
      </div>

      <div v-loading="loading" class="matrix-wrap">
        <table class="matrix">
          <thead>
            <tr>
              <th class="room-col sticky">房间</th>
              <th v-for="day in days" :key="day.date" :class="{ weekend: day.weekend === 'true' }">
                <span class="d-day">{{ day.day }}</span>
                <span class="d-week">周{{ day.week }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="room in rooms" :key="room.roomId">
              <td class="room-col sticky">
                <strong>{{ room.roomNo }}</strong>
                <span>{{ room.floor || '-' }} · {{ typeName(room.roomTypeId) }}</span>
              </td>
              <td
                v-for="day in days"
                :key="day.date"
                class="cell"
                :class="[stateClass(room, day.date), { weekend: day.weekend === 'true' }]"
                :title="cellTitle(room, day.date)"
                @click="openCell(room, day)"
              >
                <span class="cell-label">{{ stateLabel(room, day.date) }}</span>
              </td>
            </tr>
            <tr v-if="!rooms.length">
              <td :colspan="days.length + 1" class="empty-row">该筛选条件下暂无房间</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <el-dialog v-model="dialog" :title="`${current.roomNo} · ${current.date}`" width="440px">
      <div class="cell-detail">
        <p>
          当前档期状态：<strong>{{ current.state ? stateText(current.state) : '未占用' }}</strong>
        </p>
        <el-form label-width="100px">
          <el-form-item label="订单 ID">
            <el-input v-model="form.orderId" placeholder="锁定时可关联订单，释放时必填" />
          </el-form-item>
          <el-form-item v-if="!current.state" label="保留时长">
            <el-input-number v-model="form.holdMinutes" :min="10" :max="1440" :step="10" />
            <span class="unit">分钟</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button v-if="current.state" type="warning" plain @click="release">释放该订单档期</el-button>
        <el-button v-else type="primary" @click="lock">锁定档期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tenantHttp } from '../../api/client'

const loading = ref(false)
const days = ref<any[]>([])
const rooms = ref<any[]>([])
const types = ref<any[]>([])
const floors = ref<string[]>([])
const floor = ref('')
const roomTypeId = ref<number | null>(null)

function fmt(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate()
  ).padStart(2, '0')}`
}

const today = new Date()
const from = ref(fmt(today))
const to = ref(fmt(new Date(today.getTime() + 13 * 86400000)))

const legends = [
  { state: 'AVAILABLE', label: '可预订', color: '#ffffff' },
  { state: 'LOCKED', label: '已锁定', color: '#f3d9a4' },
  { state: 'OCCUPIED', label: '已入住', color: '#8fc9b2' },
  { state: 'CLEANING', label: '待清洁', color: '#a9c4de' },
  { state: 'MAINTENANCE', label: '维修中', color: '#e0a9a5' }
]

const dialog = ref(false)
const current = reactive<any>({ roomId: null, roomNo: '', date: '', state: '' })
const form = reactive<any>({ orderId: '', holdMinutes: 60 })

const stateText = (state: string) =>
  ({ LOCKED: '已锁定', OCCUPIED: '已入住', CLEANING: '待清洁', MAINTENANCE: '维修中' }[state] || state)

function typeName(id: number) {
  return types.value.find((t) => t.id === id)?.name || '未分房型'
}

function cellState(room: any, date: string) {
  return room.dayStates?.[date] || ''
}

function stateClass(room: any, date: string) {
  const state = cellState(room, date)
  return state ? state.toLowerCase() : 'free'
}

function stateLabel(room: any, date: string) {
  const state = cellState(room, date)
  return state ? stateText(state) : ''
}

function cellTitle(room: any, date: string) {
  const state = cellState(room, date)
  return `${room.roomNo} ${date} ${state ? stateText(state) : '可预订'}`
}

function shift(daysDelta: number) {
  const start = new Date(from.value)
  start.setDate(start.getDate() + daysDelta)
  const end = new Date(start)
  end.setDate(end.getDate() + 13)
  from.value = fmt(start)
  to.value = fmt(end)
  load()
}

function resetRange() {
  from.value = fmt(new Date())
  to.value = fmt(new Date(Date.now() + 13 * 86400000))
  load()
}

async function load() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/rooms/calendar', {
      params: { from: from.value, to: to.value, floor: floor.value || undefined, roomTypeId: roomTypeId.value || undefined }
    })
    days.value = res.data?.days || []
    rooms.value = res.data?.rooms || []
    floors.value = Array.from(new Set(rooms.value.map((r) => r.floor).filter(Boolean))) as string[]
  } finally {
    loading.value = false
  }
}

function openCell(room: any, day: any) {
  Object.assign(current, {
    roomId: room.roomId,
    roomNo: room.roomNo,
    date: day.date,
    state: cellState(room, day.date)
  })
  form.orderId = ''
  form.holdMinutes = 60
  dialog.value = true
}

async function lock() {
  await tenantHttp.post('/api/tenant/rooms/schedules/lock', {
    roomId: current.roomId,
    orderId: form.orderId ? Number(form.orderId) : null,
    dates: [current.date],
    holdMinutes: form.holdMinutes
  })
  ElMessage.success('档期已锁定')
  dialog.value = false
  load()
}

async function release() {
  if (!form.orderId) {
    ElMessage.warning('释放档期需要填写关联的订单 ID')
    return
  }
  await ElMessageBox.confirm('释放后该日期将回到可预订，确认继续？', '释放档期', { type: 'warning' })
  await tenantHttp.post(`/api/tenant/rooms/schedules/${form.orderId}/release`)
  ElMessage.success('档期已释放')
  dialog.value = false
  load()
}

onMounted(async () => {
  const res: any = await tenantHttp.get('/api/tenant/rooms/types')
  types.value = res.data || []
  await load()
})
</script>

<style scoped>
.calendar-page {
  padding: var(--space-5) var(--space-6) var(--space-7);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px var(--space-5);
  border-bottom: 1px solid var(--line);
  background: var(--surface-sunken);
}
.range {
  font-size: 12px;
  color: var(--ink-500);
  font-variant-numeric: tabular-nums;
}
.legend {
  display: flex;
  gap: 12px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--ink-500);
}
.legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  border: 1px solid var(--line-strong);
}
.matrix-wrap {
  overflow: auto;
  max-height: calc(100vh - 290px);
}
.matrix {
  border-collapse: separate;
  border-spacing: 0;
  width: 100%;
  min-width: 1200px;
}
.matrix th {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  padding: 8px 4px;
  font-weight: 500;
  color: var(--ink-500);
  font-size: 11px;
  min-width: 58px;
}
.matrix th.weekend {
  background: #fbf7f0;
}
.d-day {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
}
.d-week {
  font-size: 10px;
  color: var(--ink-400);
}
.room-col {
  position: sticky;
  left: 0;
  z-index: 3;
  background: var(--surface);
  border-right: 1px solid var(--line);
  min-width: 150px;
  padding: 8px 12px;
  text-align: left;
}
.matrix th.room-col { z-index: 4; }
.room-col strong {
  display: block;
  font-size: 13px;
  color: var(--ink-900);
}
.room-col span {
  font-size: 11px;
  color: var(--ink-400);
}
.cell {
  height: 40px;
  border-bottom: 1px solid var(--line);
  border-right: 1px solid #f2eee6;
  cursor: pointer;
  text-align: center;
  transition: filter 0.12s ease;
}
.cell:hover {
  filter: brightness(0.96);
}
.cell.free { background: #fff; }
.cell.locked { background: #f3d9a4; }
.cell.occupied { background: #8fc9b2; }
.cell.cleaning { background: #a9c4de; }
.cell.maintenance { background: #e0a9a5; }
.cell.weekend.free { background: #fdfbf7; }
.cell-label {
  font-size: 10.5px;
  color: rgba(22, 33, 29, 0.72);
}
.empty-row {
  text-align: center;
  padding: 50px;
  color: var(--ink-400);
}
.cell-detail p {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--ink-700);
}
.unit {
  margin-left: 8px;
  color: var(--ink-400);
  font-size: 12px;
}
</style>
