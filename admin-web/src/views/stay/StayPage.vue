<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>入馆与出馆</h1>
        <p class="sub">入馆校验健康告知与知情同意、证件加密留档，房态自动转已入住并生成护理计划；出馆完成尾款押金结算与满意度评价</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll">
          <Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span>
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
      <el-tab-pane name="pending">
        <template #label>
          待入馆
          <span v-if="pendingOrders.length" class="tab-badge">{{ pendingOrders.length }}</span>
        </template>
        <div class="card flush">
          <el-table v-loading="loading" :data="pendingOrders" size="small" stripe>
            <el-table-column prop="orderNo" label="订单号" width="180">
              <template #default="{ row }"><span class="mono">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column prop="customerName" label="客户" width="110" />
            <el-table-column label="房间" width="110">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column prop="checkinDate" label="入住日期" width="120" />
            <el-table-column prop="checkoutDate" label="离馆日期" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge warn">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCheckin(row)">办理入馆</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>没有待入馆订单</strong>订单支付定金后状态会转为「待入住」</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="在住管理" name="living">
        <div v-loading="loading" class="living-grid">
          <article v-for="c in living" :key="c.id" class="living-card" @click="openStay(c)">
            <div class="living-head">
              <span class="avatar">{{ (c.motherName || '?').slice(0, 1) }}</span>
              <div class="living-meta">
                <strong>{{ c.motherName }}</strong>
                <span class="muted">{{ roomName(c.roomId) }} · {{ c.deliveryType }}</span>
              </div>
              <span class="badge ok">{{ c.status }}</span>
            </div>
            <div class="living-body">
              <div><span>入馆时间</span><b>{{ (c.checkinTime || '').replace('T', ' ').slice(0, 16) }}</b></div>
              <div><span>新生儿体重</span><b>{{ c.babyWeight || '-' }} kg</b></div>
              <div><span>喂养方式</span><b>{{ c.feedingType || '-' }}</b></div>
              <div><span>押金</span><b>¥{{ money(c.depositAmount) }}</b></div>
            </div>
            <div class="living-foot">
              <span class="mono">证件 {{ c.idCardNo || '未登记' }}</span>
              <el-button link type="primary" @click.stop="openStay(c)">在住详情</el-button>
            </div>
          </article>
          <div v-if="!living.length" class="empty card">
            <strong>当前没有在住客户</strong>从「待入馆」办理入馆后进入此处
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="已出馆" name="done">
        <div class="card flush">
          <el-table v-loading="loading" :data="doneList" size="small" stripe>
            <el-table-column prop="motherName" label="产妇" width="110" />
            <el-table-column label="房间" width="110">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column prop="checkinTime" label="入馆时间" min-width="150" />
            <el-table-column prop="deliveryType" label="分娩方式" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge muted">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openStay(row)">查看小结</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂无出馆记录</strong></div></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 入馆登记 -->
    <el-drawer v-model="checkinDrawer" title="入馆登记" size="680px">
      <div v-if="currentOrder" class="order-brief">
        <div>
          <strong>{{ currentOrder.customerName }}</strong>
          <p class="muted mono">{{ currentOrder.orderNo }} · {{ currentOrder.checkinDate }} → {{ currentOrder.checkoutDate }}</p>
        </div>
        <span class="badge warn">{{ currentOrder.status }}</span>
      </div>

      <el-form :model="checkinForm" label-position="top" class="form-grid">
        <p class="section-label full">产妇信息</p>
        <el-form-item label="产妇姓名" required><el-input v-model="checkinForm.motherName" /></el-form-item>
        <el-form-item label="年龄"><el-input-number v-model="checkinForm.motherAge" :min="15" :max="60" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="孕周"><el-input-number v-model="checkinForm.gestationalWeek" :min="20" :max="45" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="分娩方式" required>
          <el-select v-model="checkinForm.deliveryType" style="width: 100%">
            <el-option label="自然分娩" value="自然分娩" />
            <el-option label="剖宫产" value="剖宫产" />
            <el-option label="产钳助产" value="产钳助产" />
          </el-select>
        </el-form-item>
        <el-form-item label="分娩日期"><el-date-picker v-model="checkinForm.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="新生儿出生日期"><el-date-picker v-model="checkinForm.babyBirthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="新生儿体重（kg）"><el-input-number v-model="checkinForm.babyWeight" :min="0.5" :max="8" :precision="2" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="喂养方式">
          <el-select v-model="checkinForm.feedingType" clearable style="width: 100%">
            <el-option label="纯母乳" value="纯母乳" />
            <el-option label="混合喂养" value="混合喂养" />
            <el-option label="人工喂养" value="人工喂养" />
          </el-select>
        </el-form-item>

        <p class="section-label full">入住与告知</p>
        <el-form-item label="房间" required>
          <el-select v-model="checkinForm.roomId" filterable style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo}（${r.floor || '-'} · ${r.status}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="护理天数"><el-input-number v-model="checkinForm.careDays" :min="1" :max="120" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="押金（元）"><el-input-number v-model="checkinForm.depositAmount" :min="0" :step="500" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="证件号码"><el-input v-model="checkinForm.idCardNo" placeholder="落库 AES/GCM 加密，列表仅显示脱敏值" /></el-form-item>
        <el-form-item label="家属信息" class="full"><el-input v-model="checkinForm.familyInfo" placeholder="如：丈夫 张先生 138****8888" /></el-form-item>
        <el-form-item label="紧急联系人" class="full"><el-input v-model="checkinForm.emergencyContact" /></el-form-item>
        <el-form-item label="健康告知与知情同意" class="full" required>
          <el-checkbox v-model="healthSigned">已向产妇告知并签署《健康告知与知情同意书》</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="checkinDrawer = false">取消</el-button>
          <el-button type="primary" :loading="saving" :disabled="!healthSigned" @click="submitCheckin">确认入馆</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 在住详情 / 出馆结算 -->
    <el-drawer v-model="stayDrawer" :title="`${current?.motherName || ''} · 在住详情`" size="720px">
      <div v-if="stay" class="detail">
        <div v-if="stay.checkout" class="checkout-banner">
          <Icon name="check" :size="16" />
          <div>
            <strong>已于 {{ stay.checkout.checkoutTime }} 出馆</strong>
            <p class="muted">满意度 {{ stay.checkout.satisfaction || '-' }} 分 · 复查预约 {{ stay.checkout.reviewAppointment || '未安排' }}</p>
          </div>
        </div>

        <div class="detail-grid">
          <div class="detail-item"><span>房间</span><strong>{{ roomName(stay.checkin?.roomId) }}</strong></div>
          <div class="detail-item"><span>分娩方式</span><strong>{{ stay.checkin?.deliveryType }}</strong></div>
          <div class="detail-item"><span>入馆时间</span><strong>{{ stay.checkin?.checkinTime }}</strong></div>
          <div class="detail-item"><span>押金</span><strong>¥{{ money(stay.checkin?.depositAmount) }}</strong></div>
          <div class="detail-item"><span>证件（脱敏）</span><strong class="mono">{{ stay.checkin?.idCardNo || '-' }}</strong></div>
          <div class="detail-item"><span>紧急联系人</span><strong>{{ stay.checkin?.emergencyContact || '-' }}</strong></div>
        </div>

        <p class="section-label">护理计划</p>
        <el-table :data="stay.order?.items || []" size="small" stripe>
          <el-table-column prop="itemName" label="项目" min-width="160" />
          <el-table-column prop="itemType" label="类型" width="110" />
          <el-table-column prop="quantity" label="数量" width="80" align="right" />
          <el-table-column label="金额" width="110" align="right">
            <template #default="{ row }"><span class="money">¥{{ money(row.amount) }}</span></template>
          </el-table-column>
          <template #empty><div class="empty">暂无订单明细</div></template>
        </el-table>

        <template v-if="!stay.checkout">
          <p class="section-label">出馆结算</p>
          <el-form :model="checkoutForm" label-position="top" class="form-grid">
            <el-form-item label="尾款金额"><el-input-number v-model="checkoutForm.tailAmount" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item label="增值消费"><el-input-number v-model="checkoutForm.extraAmount" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item label="押金退还（不超过押金）"><el-input-number v-model="checkoutForm.depositRefund" :min="0" :precision="2" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item label="物品已归还">
              <el-switch v-model="checkoutForm.itemReturned" />
            </el-form-item>
            <el-form-item label="满意度（1~5）">
              <el-rate v-model="checkoutForm.satisfaction" :max="5" show-score />
            </el-form-item>
            <el-form-item label="42 天复查预约">
              <el-date-picker v-model="checkoutForm.reviewAppointment" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="出馆小结" class="full">
              <el-input v-model="checkoutForm.summary" type="textarea" :rows="3" placeholder="恢复情况、喂养建议、注意事项" />
            </el-form-item>
          </el-form>
          <div class="drawer-footer">
            <el-button @click="settleTail">仅收尾款</el-button>
            <el-button type="primary" :loading="saving" @click="submitCheckout">确认出馆</el-button>
          </div>
        </template>
        <div v-else class="summary-box">
          <p class="section-label">出馆小结</p>
          <p>{{ stay.checkout.summary || '未填写' }}</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const tab = ref('pending')
const loading = ref(false)
const saving = ref(false)
const pendingOrders = ref<any[]>([])
const checkins = ref<any[]>([])
const rooms = ref<any[]>([])

const living = computed(() => checkins.value.filter((c) => c.status === '在住'))
const doneList = computed(() => checkins.value.filter((c) => c.status !== '在住'))

const stats = computed(() => [
  { label: '待入馆', value: pendingOrders.value.length, hint: '已付定金待办理' },
  { label: '在住', value: living.value.length, hint: '当前在住产妇' },
  { label: '今日离馆', value: pendingOrders.value.filter((o) => o.checkoutDate === today()).length, hint: today() },
  { label: '累计出馆', value: doneList.value.length, hint: '含历史' }
])

function today() {
  return new Date().toISOString().slice(0, 10)
}

function money(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function roomName(id: number) {
  const room = rooms.value.find((r) => r.id === id)
  return room ? room.roomNo : id ? `房间 #${id}` : '-'
}

async function loadOrders() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/orders', {
      params: { status: '待入住', pageNum: 1, pageSize: 100 }
    })
    pendingOrders.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

async function loadCheckins() {
  const res: any = await tenantHttp.get('/api/tenant/stay/checkins')
  checkins.value = res.data || []
}

async function loadRooms() {
  const res: any = await tenantHttp.get('/api/tenant/rooms')
  rooms.value = res.data?.records || res.data || []
}

function loadAll() {
  loadOrders()
  loadCheckins()
}

function onTab(name: string) {
  if (name === 'pending') loadOrders()
  else loadCheckins()
}

/* ---------------- 入馆 ---------------- */
const checkinDrawer = ref(false)
const currentOrder = ref<any>(null)
const healthSigned = ref(false)
const checkinForm = reactive<any>({})

function openCheckin(order: any) {
  currentOrder.value = order
  Object.keys(checkinForm).forEach((k) => delete checkinForm[k])
  const nights = Math.max(
    1,
    Math.round((new Date(order.checkoutDate).getTime() - new Date(order.checkinDate).getTime()) / 86400000)
  )
  Object.assign(checkinForm, {
    orderId: order.id,
    roomId: order.roomId,
    motherName: order.customerName,
    deliveryType: '自然分娩',
    careDays: nights,
    depositAmount: Number(order.bondAmount || 0)
  })
  healthSigned.value = false
  checkinDrawer.value = true
}

async function submitCheckin() {
  if (!checkinForm.roomId) {
    ElMessage.warning('请选择房间')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/stay/checkins', {
      ...checkinForm,
      healthNoticeSigned: 1
    })
    ElMessage.success('入馆登记完成，房态已转已入住并生成护理计划')
    checkinDrawer.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

/* ---------------- 在住详情 ---------------- */
const stayDrawer = ref(false)
const current = ref<any>(null)
const stay = ref<any>({})
const checkoutForm = reactive<any>({})

async function openStay(row: any) {
  current.value = row
  const res: any = await tenantHttp.get(`/api/tenant/stay/orders/${row.orderId}`)
  stay.value = res.data || {}
  const order = stay.value.order?.order || {}
  Object.keys(checkoutForm).forEach((k) => delete checkoutForm[k])
  Object.assign(checkoutForm, {
    tailAmount: Number(order.tailAmount || 0),
    extraAmount: 0,
    depositRefund: Number(row.depositAmount || 0),
    itemReturned: true,
    satisfaction: 5,
    summary: ''
  })
  stayDrawer.value = true
}

async function settleTail() {
  const res: any = await tenantHttp.post(`/api/tenant/stay/orders/${current.value.orderId}/settle`, {
    method: '现金',
    amount: checkoutForm.tailAmount
  })
  ElMessage.success(`尾款已收：¥${money(res.data?.amount || checkoutForm.tailAmount)}`)
}

async function submitCheckout() {
  await ElMessageBox.confirm('确认办理出馆？房态将转为待清洁并生成清洁任务。', '出馆确认', { type: 'warning' })
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/stay/checkouts', {
      orderId: current.value.orderId,
      checkinId: current.value.id,
      tailAmount: checkoutForm.tailAmount,
      extraAmount: checkoutForm.extraAmount,
      depositRefund: checkoutForm.depositRefund,
      itemReturned: checkoutForm.itemReturned ? 1 : 0,
      summary: checkoutForm.summary,
      satisfaction: checkoutForm.satisfaction,
      reviewAppointment: checkoutForm.reviewAppointment || null
    })
    ElMessage.success('出馆完成，已生成清洁任务')
    stayDrawer.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRooms()
  loadAll()
})
</script>

<style scoped>
.stat-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: var(--space-3); }
.stat { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px 16px; display: flex; flex-direction: column; gap: 2px; box-shadow: var(--shadow-xs); }
.stat-label { font-size: 11.5px; color: var(--ink-400); letter-spacing: 0.04em; }
.stat-value { font-size: 22px; font-weight: 650; color: var(--ink-900); font-variant-numeric: tabular-nums; }
.stat-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }

.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.tab-badge { display: inline-block; margin-left: 6px; padding: 0 5px; height: 16px; line-height: 16px; border-radius: 8px; background: #fbeeed; color: #a83c37; font-size: 10.5px; }

.living-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: var(--space-3); }
.living-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px; cursor: pointer; transition: box-shadow 0.15s ease, transform 0.15s ease; box-shadow: var(--shadow-xs); }
.living-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.living-head { display: flex; align-items: center; gap: 10px; }
.avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-weight: 600; }
.living-meta { display: flex; flex-direction: column; line-height: 1.3; }
.living-meta strong { font-size: 14px; }
.living-meta span { font-size: 11.5px; }
.living-head .badge { margin-left: auto; }
.living-body { margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px 12px; }
.living-body div { display: flex; flex-direction: column; gap: 1px; }
.living-body span { font-size: 11px; color: var(--ink-400); }
.living-body b { font-size: 12.5px; font-weight: 550; }
.living-foot { margin-top: 10px; padding-top: 9px; border-top: 1px dashed var(--line); display: flex; align-items: center; justify-content: space-between; font-size: 11.5px; color: var(--ink-400); }

.order-brief { display: flex; align-items: center; justify-content: space-between; background: var(--surface-sunken); border-radius: var(--radius-md); padding: 12px 14px; margin-bottom: var(--space-4); }
.order-brief p { margin: 2px 0 0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.form-grid :deep(.full) { grid-column: 1 / -1; }
.section-label { margin: 6px 0 8px; font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; }
.drawer-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: var(--space-4); }

.detail { display: flex; flex-direction: column; gap: var(--space-4); }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; background: var(--surface-sunken); border-radius: var(--radius-md); padding: 14px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item span { font-size: 11.5px; color: var(--ink-400); }
.detail-item strong { font-size: 13px; font-weight: 550; }
.checkout-banner { display: flex; align-items: center; gap: 10px; background: #eef7f1; color: #2f7d55; border-radius: var(--radius-md); padding: 12px 14px; }
.checkout-banner p { margin: 2px 0 0; }
.summary-box { background: var(--surface-sunken); border-radius: var(--radius-md); padding: 14px; }
.summary-box p { margin: 4px 0 0; font-size: 13px; line-height: 1.6; }
.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.money { font-variant-numeric: tabular-nums; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
