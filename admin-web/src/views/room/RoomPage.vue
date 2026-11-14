<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>房型与房间</h1>
        <p class="sub">房态五态联动档期：可预订 / 已锁定 / 已入住 / 待清洁 / 维修中</p>
      </div>
      <div class="actions">
        <el-button @click="router.push('/calendar')">
          <Icon name="calendar" :size="15" /><span style="margin-left: 6px">档期日历</span>
        </el-button>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs">
      <el-tab-pane label="房型" name="type">
        <CrudPage
          ref="typeRef"
          title="房型"
          subtitle="房型图集与富文本介绍直接用于官网与小程序展示"
          base="/api/tenant/rooms/types"
          entity="房型"
          create-text="新增房型"
          :columns="typeColumns"
          :fields="typeFields"
          :deletable="true"
        />
      </el-tab-pane>

      <el-tab-pane label="房间台账" name="room">
        <CrudPage
          ref="roomRef"
          title="房间台账"
          subtitle="房间编号租户内唯一，删除为逻辑删除，历史档期保留"
          base="/api/tenant/rooms"
          entity="房间"
          create-text="新增房间"
          :columns="roomColumns"
          :fields="roomFields"
          :filters="roomFilters"
        >
          <template #col-roomTypeId="{ row }">{{ typeName(row.roomTypeId) }}</template>
        </CrudPage>
      </el-tab-pane>

      <el-tab-pane label="清洁与维修" name="task">
        <CrudPage
          ref="taskRef"
          title="房间任务"
          subtitle="出馆自动生成清洁任务；维修任务创建后房态转为维修中"
          base="/api/tenant/rooms/tasks"
          entity="任务"
          create-text="新建任务"
          :columns="taskColumns"
          :fields="taskFields"
          :filters="taskFilters"
          :deletable="false"
        >
          <template #col-roomId="{ row }">{{ roomName(row.roomId) }}</template>
          <template #row-actions="{ row, reload }">
            <el-button v-if="row.status !== '已完成'" link type="primary" @click.stop="finish(row, reload)">
              完成
            </el-button>
            <span v-else class="muted">已完成</span>
          </template>
        </CrudPage>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage from '../../components/CrudPage.vue'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const router = useRouter()
const tab = ref('type')
const types = ref<any[]>([])
const rooms = ref<any[]>([])
const typeRef = ref()
const roomRef = ref()
const taskRef = ref()

const levelOptions = [
  { label: '温馨单间', value: '温馨单间' },
  { label: '豪华套间', value: '豪华套间' },
  { label: '尊享套房', value: '尊享套房' }
]
const roomStatusOptions = [
  { label: '可预订', value: 'AVAILABLE' },
  { label: '已锁定', value: 'LOCKED' },
  { label: '已入住', value: 'OCCUPIED' },
  { label: '待清洁', value: 'CLEANING' },
  { label: '维修中', value: 'MAINTENANCE' }
]

const typeColumns = [
  { prop: 'name', label: '房型名称', min: 150 },
  { prop: 'level', label: '档位', min: 120, type: 'status' as const },
  { prop: 'area', label: '面积（㎡）', width: 110 },
  { prop: 'maxGuest', label: '可住人数', width: 100 },
  { prop: 'bedType', label: '床型', min: 120 },
  { prop: 'status', label: '状态', width: 100, type: 'status' as const },
  { prop: 'sort', label: '排序', width: 80 }
]

const typeFields = [
  { key: 'name', label: '房型名称', required: true },
  { key: 'level', label: '房型档位', type: 'select', options: levelOptions, required: true, default: '温馨单间' },
  { key: 'area', label: '面积（㎡）', type: 'number' },
  { key: 'maxGuest', label: '可住人数', type: 'number', required: true, default: 2 },
  { key: 'bedType', label: '床型' },
  { key: 'facilities', label: '房间配置', placeholder: '如：独立卫浴 / 家属陪护床 / 冰箱' },
  { key: 'images', label: '房型图集', type: 'image', multiple: true, limit: 9, dir: 'room', full: true },
  { key: 'description', label: '房型介绍', type: 'richtext', full: true },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' }
    ],
    default: 'ENABLED'
  },
  { key: 'sort', label: '排序', type: 'number', default: 0 }
]

const roomColumns = [
  { prop: 'roomNo', label: '房间编号', width: 130 },
  { prop: 'roomTypeId', label: '房型', min: 140, slot: true },
  { prop: 'floor', label: '楼层', width: 90 },
  { prop: 'status', label: '房态', width: 110, type: 'status' as const },
  { prop: 'remark', label: '备注', min: 160 }
]

const roomFilters = [
  { key: 'floor', label: '楼层' },
  {
    key: 'status',
    label: '房态',
    type: 'select',
    options: roomStatusOptions
  }
]

const roomFields = [
  { key: 'roomNo', label: '房间编号', required: true },
  { key: 'roomTypeId', label: '房型', type: 'select', required: true, options: [] as any[] },
  { key: 'floor', label: '楼层', placeholder: '如 3F' },
  {
    key: 'status',
    label: '房态',
    type: 'select',
    options: roomStatusOptions,
    default: 'AVAILABLE'
  },
  { key: 'remark', label: '备注', type: 'textarea', full: true }
]

const taskColumns = [
  { prop: 'roomId', label: '房间', min: 120, slot: true },
  { prop: 'taskType', label: '类型', width: 90 },
  { prop: 'status', label: '状态', width: 100, type: 'status' as const },
  { prop: 'dueTime', label: '要求完成', min: 150 },
  { prop: 'remark', label: '任务说明', min: 200 }
]

const taskFilters = [
  { key: 'taskType', label: '类型', type: 'select', options: [{ label: '清洁', value: '清洁' }, { label: '维修', value: '维修' }] },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { label: '待处理', value: '待处理' },
      { label: '进行中', value: '进行中' },
      { label: '已完成', value: '已完成' }
    ]
  }
]

const taskFields = [
  { key: 'roomId', label: '房间', type: 'select', required: true, options: [] as any[] },
  { key: 'taskType', label: '任务类型', type: 'select', options: [{ label: '清洁', value: '清洁' }, { label: '维修', value: '维修' }], default: '清洁' },
  { key: 'assigneeId', label: '执行人账号 ID', type: 'number' },
  { key: 'dueTime', label: '要求完成时间', type: 'datetime' },
  { key: 'remark', label: '任务说明', type: 'textarea', full: true }
]

function typeName(id: number) {
  return types.value.find((t) => t.id === id)?.name || '-'
}

function roomName(id: number) {
  const room = rooms.value.find((r) => r.id === id)
  return room ? `${room.roomNo}${room.floor ? `（${room.floor}）` : ''}` : '-'
}

async function finish(row: any, reload: () => void) {
  await ElMessageBox.confirm(`确认房间任务已完成？房态将自动回到可预订。`, '完成任务', {
    type: 'info'
  })
  await tenantHttp.put(`/api/tenant/rooms/tasks/${row.id}/finish`)
  ElMessage.success('任务已完成')
  reload()
}

async function loadRefs() {
  const [typeRes, roomRes]: any = await Promise.all([
    tenantHttp.get('/api/tenant/rooms/types'),
    tenantHttp.get('/api/tenant/rooms')
  ])
  types.value = typeRes.data || []
  rooms.value = roomRes.data?.records || roomRes.data || []
  const typeField = roomFields.find((f) => f.key === 'roomTypeId')
  if (typeField) {
    typeField.options = types.value.map((t) => ({ label: t.name, value: t.id }))
  }
  const roomField = taskFields.find((f) => f.key === 'roomId')
  if (roomField) {
    roomField.options = rooms.value.map((r) => ({ label: r.roomNo, value: r.id }))
  }
}

onMounted(loadRefs)
</script>

<style scoped>
.biz-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
  padding: 0 4px;
  border-bottom: 1px solid var(--line);
}
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) {
  height: 42px;
  font-size: 13.5px;
  color: var(--ink-500);
}
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.biz-tabs :deep(.el-tabs__content) { overflow: visible; }
.biz-tabs :deep(.crud-page) { padding-left: 0; padding-right: 0; max-width: none; }
.muted { color: var(--ink-400); font-size: 12px; }
</style>
