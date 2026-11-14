<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>护理管理</h1>
        <p class="sub">计划下发 → 执行打卡（服务端时间戳）→ 24 小时内可改留痕，超时补录需主管审批；交接与异常全程可追溯</p>
      </div>
      <div class="actions">
        <el-button @click="loadAll">
          <Icon name="refresh" :size="15" /><span style="margin-left: 6px">刷新</span>
        </el-button>
        <el-button type="primary" @click="openAbnormal()">
          <Icon name="alert" :size="15" /><span style="margin-left: 6px">上报异常</span>
        </el-button>
      </div>
    </div>

    <el-tabs v-model="tab" class="biz-tabs" @tab-change="onTab">
      <el-tab-pane label="护理看板" name="board">
        <div class="board-head">
          <div class="date-nav">
            <el-button size="small" text @click="shiftDate(-1)"><Icon name="chevron" :size="14" style="transform: rotate(90deg)" /></el-button>
            <el-date-picker v-model="boardDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 140px" @change="loadBoard" />
            <el-button size="small" text @click="shiftDate(1)"><Icon name="chevron" :size="14" style="transform: rotate(-90deg)" /></el-button>
            <el-button size="small" text @click="boardDate = today(); loadBoard()">今天</el-button>
          </div>
          <div class="board-kpi">
            <span>完成率 <b>{{ board.rate || 0 }}%</b></span>
            <span>已完成 <b>{{ board.done || 0 }}</b>/{{ board.total || 0 }}</span>
            <span class="warn">未闭环异常 <b>{{ board.openAbnormal || 0 }}</b></span>
          </div>
        </div>

        <div v-loading="loading" class="care-grid">
          <article v-for="card in board.cards || []" :key="card.planId" class="care-card" @click="openPlan(card.planId)">
            <div class="care-head">
              <span class="avatar">{{ (card.customerName || '?').slice(0, 1) }}</span>
              <div>
                <strong>{{ card.customerName }}</strong>
                <p class="muted">{{ roomName(card.roomId) }} · {{ card.careMode }}</p>
              </div>
              <span v-if="card.abnormal" class="flag">有跳过项</span>
            </div>
            <div class="ring-row">
              <div class="ring" :style="ringStyle(card.rate)">
                <span>{{ card.rate }}%</span>
              </div>
              <div class="ring-meta">
                <p><b>{{ card.done }}</b> / {{ card.total }} 项已完成</p>
                <el-progress :percentage="card.rate" :stroke-width="6" :show-text="false" :color="card.rate >= 80 ? '#3aa173' : card.rate >= 50 ? '#c99a4f' : '#b06b62'" />
              </div>
            </div>
          </article>
          <div v-if="!(board.cards || []).length" class="empty card">
            <strong>当日没有护理计划</strong>在「护理计划」为在住客户生成计划后进入看板
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="护理计划" name="plan">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-select v-model="planStatus" placeholder="全部状态" clearable size="small" style="width: 140px" @change="loadPlans">
              <el-option label="进行中" value="进行中" />
              <el-option label="已结束" value="已结束" />
            </el-select>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openCreatePlan">生成护理计划</el-button>
          </div>
          <el-table v-loading="loading" :data="plans" size="small" stripe @row-click="(row: any) => openPlan(row.id)">
            <el-table-column prop="customerName" label="客户" width="120" />
            <el-table-column label="房间" width="110">
              <template #default="{ row }">{{ roomName(row.roomId) }}</template>
            </el-table-column>
            <el-table-column prop="careMode" label="护理模式" min-width="130" />
            <el-table-column prop="templateCode" label="模板" width="130" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '进行中' ? 'ok' : 'muted'">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column prop="createdTime" label="创建时间" min-width="150" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openPlan(row.id)">查看计划</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>还没有护理计划</strong>入馆登记会自动生成，也可手动创建</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="护理记录" name="record">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-select v-model="recordTarget" placeholder="全部对象" clearable size="small" style="width: 130px" @change="loadRecords">
              <el-option label="产妇" value="产妇" />
              <el-option label="新生儿" value="新生儿" />
            </el-select>
            <span class="spacer" />
            <span class="muted">共 {{ records.length }} 条</span>
          </div>
          <el-table v-loading="loading" :data="records" size="small" stripe>
            <el-table-column prop="submitTime" label="记录时间" width="160" />
            <el-table-column prop="targetType" label="对象" width="90" />
            <el-table-column prop="itemName" label="护理项" min-width="140" />
            <el-table-column prop="temperature" label="体温" width="80" />
            <el-table-column prop="weight" label="体重" width="80" />
            <el-table-column prop="jaundiceValue" label="黄疸" width="80" />
            <el-table-column prop="bloodPressure" label="血压" width="100" />
            <el-table-column prop="content" label="记录内容" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="badge" :class="row.status === '待审批' ? 'warn' : 'ok'">{{ row.status || '已提交' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="editRecord(row)">修改</el-button>
                <el-button v-if="row.status === '待审批'" link type="primary" @click="approveRecord(row)">审批</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>暂无护理记录</strong>护理师端打卡后实时同步到这里</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="交接班" name="handover">
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-date-picker v-model="handoverDate" type="date" value-format="YYYY-MM-DD" size="small" placeholder="交接日期" style="width: 150px" @change="loadHandovers" />
            <span class="muted">及时率 {{ handoverStat.rate ?? 100 }}%（{{ handoverStat.confirmed || 0 }}/{{ handoverStat.total || 0 }}）</span>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openHandover">新建交接单</el-button>
          </div>
          <el-table v-loading="loading" :data="handovers" size="small" stripe>
            <el-table-column prop="shiftDate" label="日期" width="115" />
            <el-table-column prop="shiftType" label="班次" width="100" />
            <el-table-column prop="watchPoints" label="重点关注" min-width="180" />
            <el-table-column prop="unfinished" label="未完成事项" min-width="180" />
            <el-table-column prop="abnormalNotes" label="异常提示" min-width="150" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '已确认' ? 'ok' : 'warn'">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status !== '已确认'" link type="primary" @click="confirmHandover(row)">接班确认</el-button>
                <span v-else class="muted">双方已签</span>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>当日无交接单</strong>建议每班次结束后 10 分钟内完成交接</div></template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane name="abnormal">
        <template #label>
          异常上报
          <span v-if="openAbnormalCount" class="tab-badge">{{ openAbnormalCount }}</span>
        </template>
        <div class="card flush">
          <div class="toolbar filter-bar">
            <el-select v-model="abnormalStatus" placeholder="全部状态" clearable size="small" style="width: 140px" @change="loadAbnormals">
              <el-option label="待处理" value="待处理" />
              <el-option label="处理中" value="处理中" />
              <el-option label="已闭环" value="已闭环" />
            </el-select>
            <span class="spacer" />
            <el-button size="small" type="primary" @click="openAbnormal()">上报异常</el-button>
          </div>
          <el-table v-loading="loading" :data="abnormals" size="small" stripe>
            <el-table-column prop="createdTime" label="上报时间" width="160" />
            <el-table-column prop="abnormalType" label="类型" width="120" />
            <el-table-column label="紧急程度" width="100">
              <template #default="{ row }">
                <span class="badge" :class="row.urgency === '紧急' ? 'danger' : row.urgency === '较急' ? 'warn' : 'info'">{{ row.urgency }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="220" />
            <el-table-column label="告知家属" width="100">
              <template #default="{ row }">{{ row.familyNotified ? '已告知' : '未告知' }}</template>
            </el-table-column>
            <el-table-column prop="handleResult" label="处置记录" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><span class="badge" :class="row.status === '已闭环' ? 'ok' : row.status === '处理中' ? 'info' : 'warn'">{{ row.status }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleAbnormal(row)">{{ row.status === '已闭环' ? '查看' : '处置' }}</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty"><strong>没有异常记录</strong>护理师端上报的异常会实时进入此处</div></template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 计划详情 -->
    <el-drawer v-model="planDrawer" :title="`${planDetail.plan?.customerName || ''} · 护理计划`" size="720px">
      <div v-if="planDetail.plan" class="detail">
        <div class="plan-summary">
          <div><span>完成率</span><strong>{{ planDetail.rate }}%</strong></div>
          <div><span>已完成</span><strong>{{ planDetail.done }}/{{ planDetail.total }}</strong></div>
          <div><span>护理模式</span><strong>{{ planDetail.plan.careMode }}</strong></div>
          <div><span>状态</span><strong>{{ planDetail.plan.status }}</strong></div>
        </div>

        <div class="add-item">
          <el-input v-model="newItem.itemName" size="small" placeholder="护理项名称" style="width: 160px" />
          <el-select v-model="newItem.targetType" size="small" style="width: 110px">
            <el-option label="产妇" value="产妇" />
            <el-option label="新生儿" value="新生儿" />
          </el-select>
          <el-date-picker v-model="newItem.planDate" type="date" value-format="YYYY-MM-DD" size="small" placeholder="日期" style="width: 140px" />
          <el-time-select v-model="newItem.timeSlot" start="06:00" step="00:30" end="22:00" size="small" placeholder="时间" style="width: 110px" />
          <el-button size="small" type="primary" @click="addItem">添加计划项</el-button>
        </div>

        <div v-for="(items, date) in planDetail.items" :key="date" class="day-group">
          <p class="day-head">{{ date }}<span>{{ items.length }} 项</span></p>
          <ul class="item-list">
            <li v-for="item in items" :key="item.id" :class="{ done: item.status === '已完成', skip: item.status === '已跳过' }">
              <span class="time mono">{{ item.timeSlot }}</span>
              <span class="target">{{ item.targetType }}</span>
              <span class="name">{{ item.itemName }}</span>
              <span class="badge" :class="item.status === '已完成' ? 'ok' : item.status === '已跳过' ? 'danger' : 'warn'">{{ item.status }}</span>
              <span class="ops">
                <el-button v-if="item.status !== '已完成'" link type="primary" @click="executeItem(item)">执行</el-button>
                <el-button v-if="item.status !== '已完成'" link type="danger" @click="removeItem(item)">删除</el-button>
              </span>
            </li>
          </ul>
        </div>
      </div>
    </el-drawer>

    <!-- 执行护理项 -->
    <el-dialog v-model="execDialog" title="执行护理项并记录" width="560px">
      <el-form :model="execForm" label-position="top" class="two-col">
        <el-form-item label="体温（℃）"><el-input-number v-model="execForm.temperature" :min="34" :max="42" :precision="1" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="体重（kg）"><el-input-number v-model="execForm.weight" :min="0" :max="150" :precision="2" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="血压"><el-input v-model="execForm.bloodPressure" placeholder="如 120/80" /></el-form-item>
        <el-form-item label="脉搏"><el-input-number v-model="execForm.pulse" :min="0" :max="220" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="黄疸值"><el-input-number v-model="execForm.jaundiceValue" :min="0" :max="30" :precision="1" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="喂养量（ml）"><el-input-number v-model="execForm.feedingAmount" :min="0" :max="500" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="记录内容" class="full"><el-input v-model="execForm.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="补录原因（超 24 小时必填）" class="full"><el-input v-model="execForm.backfillReason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="execDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitExec">提交记录</el-button>
      </template>
    </el-dialog>

    <!-- 修改记录 -->
    <el-dialog v-model="recordDialog" title="修改护理记录" width="520px">
      <el-alert type="info" :closable="false" show-icon title="提交 24 小时内可直接修改并留痕；超过 24 小时须主管审批" style="margin-bottom: 14px" />
      <el-form :model="recordForm" label-position="top" class="two-col">
        <el-form-item label="体温"><el-input-number v-model="recordForm.temperature" :min="34" :max="42" :precision="1" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="体重"><el-input-number v-model="recordForm.weight" :min="0" :max="150" :precision="2" :step="0.1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="记录内容" class="full"><el-input v-model="recordForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRecord">保存</el-button>
      </template>
    </el-dialog>

    <!-- 生成计划 -->
    <el-dialog v-model="createPlanDialog" title="生成护理计划" width="520px">
      <el-form :model="createPlanForm" label-position="top" class="two-col">
        <el-form-item label="订单" required>
          <el-select v-model="createPlanForm.orderId" filterable style="width: 100%" @change="onOrderPick">
            <el-option v-for="o in livingOrders" :key="o.id" :label="`${o.customerName}（${o.orderNo}）`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="护理模式">
          <el-select v-model="createPlanForm.careMode" style="width: 100%">
            <el-option label="一对一专护" value="一对一专护" />
            <el-option label="一对二" value="一对二" />
            <el-option label="集中护理" value="集中护理" />
          </el-select>
        </el-form-item>
        <el-form-item label="分娩方式">
          <el-select v-model="createPlanForm.deliveryType" style="width: 100%">
            <el-option label="自然分娩" value="自然分娩" />
            <el-option label="剖宫产" value="剖宫产" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="createPlanForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="天数"><el-input-number v-model="createPlanForm.days" :min="1" :max="120" controls-position="right" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createPlanDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPlan">生成</el-button>
      </template>
    </el-dialog>

    <!-- 交接单 -->
    <el-dialog v-model="handoverDialog" title="新建交接单" width="560px">
      <el-form :model="handoverForm" label-position="top" class="two-col">
        <el-form-item label="交接日期"><el-date-picker v-model="handoverForm.shiftDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="班次">
          <el-select v-model="handoverForm.shiftType" style="width: 100%">
            <el-option label="早班" value="早班" />
            <el-option label="中班" value="中班" />
            <el-option label="夜班" value="夜班" />
          </el-select>
        </el-form-item>
        <el-form-item label="重点关注" class="full"><el-input v-model="handoverForm.watchPoints" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="未完成事项" class="full"><el-input v-model="handoverForm.unfinished" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="异常提示" class="full"><el-input v-model="handoverForm.abnormalNotes" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handoverDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitHandover">创建</el-button>
      </template>
    </el-dialog>

    <!-- 异常上报 -->
    <el-dialog v-model="abnormalDialog" title="上报异常" width="560px">
      <el-form :model="abnormalForm" label-position="top" class="two-col">
        <el-form-item label="异常类型" required>
          <el-select v-model="abnormalForm.abnormalType" style="width: 100%">
            <el-option v-for="t in abnormalTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度" required>
          <el-select v-model="abnormalForm.urgency" style="width: 100%">
            <el-option label="一般" value="一般" />
            <el-option label="较急" value="较急" />
            <el-option label="紧急" value="紧急" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联订单">
          <el-select v-model="abnormalForm.orderId" filterable clearable style="width: 100%">
            <el-option v-for="o in livingOrders" :key="o.id" :label="`${o.customerName}（${o.orderNo}）`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="已告知家属"><el-switch v-model="abnormalForm.familyNotified" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="情况描述" class="full" required>
          <el-input v-model="abnormalForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" :title="'建议就医提示：' + medicalTip" style="margin-top: 6px" />
      <template #footer>
        <el-button @click="abnormalDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAbnormal">提交</el-button>
      </template>
    </el-dialog>

    <!-- 异常处置 -->
    <el-dialog v-model="handleDialog" title="异常处置" width="520px">
      <el-form :model="handleForm" label-position="top">
        <el-form-item label="处置结果">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="3" :disabled="handleForm.status === '已闭环'" />
        </el-form-item>
        <el-form-item label="告知家属">
          <el-switch v-model="handleForm.familyNotified" :active-value="1" :inactive-value="0" :disabled="handleForm.status === '已闭环'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="handleForm.status" :disabled="handleForm.status === '已闭环'">
            <el-radio-button label="处理中" />
            <el-radio-button label="已闭环" />
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialog = false">关闭</el-button>
        <el-button v-if="handleForm.status !== '已闭环'" type="primary" :loading="saving" @click="submitHandle">保存处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tenantHttp } from '../../api/client'

const route = useRoute()
const tab = ref('board')
const loading = ref(false)
const saving = ref(false)
const rooms = ref<any[]>([])
const livingOrders = ref<any[]>([])

const today = () => new Date().toISOString().slice(0, 10)
const boardDate = ref(today())
const board = ref<any>({ cards: [] })

const abnormalTypes = ['发热', '黄疸偏高', '伤口异常', '喂养困难', '情绪异常', '跌倒', '其他']

const medicalTip = computed(() => '如出现持续高热、伤口红肿渗液、新生儿精神反应差，请立即建议家属前往医院就诊')

const openAbnormalCount = computed(() => abnormals.value.filter((a) => a.status !== '已闭环').length)

function roomName(id: number) {
  const room = rooms.value.find((r) => r.id === id)
  return room ? room.roomNo : id ? `房间 #${id}` : '-'
}

function ringStyle(rate: number) {
  const color = rate >= 80 ? '#3aa173' : rate >= 50 ? '#c99a4f' : '#b06b62'
  return { background: `conic-gradient(${color} ${rate * 3.6}deg, var(--line) 0deg)` }
}

function shiftDate(days: number) {
  const d = new Date(boardDate.value)
  d.setDate(d.getDate() + days)
  boardDate.value = d.toISOString().slice(0, 10)
  loadBoard()
}

async function loadBoard() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/care/board', { params: { date: boardDate.value } })
    board.value = res.data || { cards: [] }
  } finally {
    loading.value = false
  }
}

/* ---------------- 计划 ---------------- */
const plans = ref<any[]>([])
const planStatus = ref('')
const planDrawer = ref(false)
const planDetail = ref<any>({ items: {} })
const currentPlanId = ref<number>(0)
const newItem = reactive<any>({ itemName: '', targetType: '产妇', planDate: today(), timeSlot: '09:00' })

async function loadPlans() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/care/plans', {
      params: { status: planStatus.value || undefined }
    })
    plans.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function openPlan(id: number) {
  currentPlanId.value = id
  const res: any = await tenantHttp.get(`/api/tenant/care/plans/${id}`)
  planDetail.value = res.data || { items: {} }
  planDrawer.value = true
}

async function addItem() {
  if (!newItem.itemName) {
    ElMessage.warning('请填写护理项名称')
    return
  }
  await tenantHttp.post(`/api/tenant/care/plans/${currentPlanId.value}/items`, { ...newItem })
  ElMessage.success('计划项已添加')
  newItem.itemName = ''
  await openPlan(currentPlanId.value)
}

async function removeItem(item: any) {
  await ElMessageBox.confirm(`确认删除「${item.itemName}」？`, '删除计划项', { type: 'warning' })
  await tenantHttp.delete(`/api/tenant/care/items/${item.id}`)
  ElMessage.success('已删除')
  await openPlan(currentPlanId.value)
}

/* 执行 */
const execDialog = ref(false)
const execForm = reactive<any>({})
let execItemId = 0

function executeItem(item: any) {
  execItemId = item.id
  Object.keys(execForm).forEach((k) => delete execForm[k])
  execDialog.value = true
}

async function submitExec() {
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/care/items/${execItemId}/execute`, { ...execForm })
    ElMessage.success('护理记录已提交（服务端时间戳）')
    execDialog.value = false
    await openPlan(currentPlanId.value)
    loadBoard()
    loadRecords()
  } finally {
    saving.value = false
  }
}

/* 生成计划 */
const createPlanDialog = ref(false)
const createPlanForm = reactive<any>({})

function openCreatePlan() {
  Object.keys(createPlanForm).forEach((k) => delete createPlanForm[k])
  Object.assign(createPlanForm, {
    careMode: '一对一专护',
    deliveryType: '自然分娩',
    startDate: today(),
    days: 28
  })
  createPlanDialog.value = true
}

function onOrderPick(id: number) {
  const order = livingOrders.value.find((o) => o.id === id)
  if (order) {
    createPlanForm.customerName = order.customerName
    createPlanForm.roomId = order.roomId
    createPlanForm.careMode = order.careMode || '一对一专护'
    const nights = Math.round(
      (new Date(order.checkoutDate).getTime() - new Date(order.checkinDate).getTime()) / 86400000
    )
    createPlanForm.days = nights > 0 ? nights : 28
  }
}

async function submitPlan() {
  if (!createPlanForm.orderId) {
    ElMessage.warning('请选择订单')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/care/plans', { ...createPlanForm })
    ElMessage.success('护理计划已生成并下发到护理师端')
    createPlanDialog.value = false
    await Promise.all([loadPlans(), loadBoard()])
  } finally {
    saving.value = false
  }
}

/* ---------------- 记录 ---------------- */
const records = ref<any[]>([])
const recordTarget = ref('')
const recordDialog = ref(false)
const recordForm = reactive<any>({})

async function loadRecords() {
  const res: any = await tenantHttp.get('/api/tenant/care/records', {
    params: { targetType: recordTarget.value || undefined }
  })
  records.value = res.data || []
}

function editRecord(row: any) {
  Object.keys(recordForm).forEach((k) => delete recordForm[k])
  Object.assign(recordForm, { ...row })
  recordDialog.value = true
}

async function submitRecord() {
  saving.value = true
  try {
    await tenantHttp.put(`/api/tenant/care/records/${recordForm.id}`, recordForm)
    ElMessage.success('记录已修改并留痕')
    recordDialog.value = false
    loadRecords()
  } finally {
    saving.value = false
  }
}

async function approveRecord(row: any) {
  await ElMessageBox.confirm('审批通过后该记录参与看板完成率统计，确认通过？', '补录审批', { type: 'info' })
  await tenantHttp.put(`/api/tenant/care/records/${row.id}/approve`)
  ElMessage.success('已审批')
  loadRecords()
}

/* ---------------- 交接班 ---------------- */
const handovers = ref<any[]>([])
const handoverDate = ref(today())
const handoverStat = ref<any>({})
const handoverDialog = ref(false)
const handoverForm = reactive<any>({})

async function loadHandovers() {
  const [list, stat] = await Promise.all([
    tenantHttp.get('/api/tenant/care/handovers', { params: { date: handoverDate.value } }),
    tenantHttp.get('/api/tenant/care/handovers/stats')
  ])
  handovers.value = (list as any).data || []
  handoverStat.value = (stat as any).data || {}
}

function openHandover() {
  Object.keys(handoverForm).forEach((k) => delete handoverForm[k])
  Object.assign(handoverForm, { shiftDate: handoverDate.value, shiftType: '早班' })
  handoverDialog.value = true
}

async function submitHandover() {
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/care/handovers', { ...handoverForm })
    ElMessage.success('交接单已创建，等待接班确认')
    handoverDialog.value = false
    loadHandovers()
  } finally {
    saving.value = false
  }
}

async function confirmHandover(row: any) {
  await ElMessageBox.confirm('确认接班并签名？签名将记录当前登录人。', '接班确认', { type: 'info' })
  await tenantHttp.post(`/api/tenant/care/handovers/${row.id}/confirm`, {})
  ElMessage.success('已确认交接')
  loadHandovers()
}

/* ---------------- 异常 ---------------- */
const abnormals = ref<any[]>([])
const abnormalStatus = ref('')
const abnormalDialog = ref(false)
const abnormalForm = reactive<any>({})
const handleDialog = ref(false)
const handleForm = reactive<any>({})

async function loadAbnormals() {
  const res: any = await tenantHttp.get('/api/tenant/care/abnormals', {
    params: { status: abnormalStatus.value || undefined }
  })
  abnormals.value = res.data || []
}

function openAbnormal() {
  Object.keys(abnormalForm).forEach((k) => delete abnormalForm[k])
  Object.assign(abnormalForm, { urgency: '一般', abnormalType: '发热', familyNotified: 0 })
  abnormalDialog.value = true
}

async function submitAbnormal() {
  if (!abnormalForm.description) {
    ElMessage.warning('请填写情况描述')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/care/abnormals', { ...abnormalForm })
    ElMessage.success('异常已上报，护理主管已收到提醒')
    abnormalDialog.value = false
    await Promise.all([loadAbnormals(), loadBoard()])
  } finally {
    saving.value = false
  }
}

function handleAbnormal(row: any) {
  Object.keys(handleForm).forEach((k) => delete handleForm[k])
  Object.assign(handleForm, { ...row })
  handleDialog.value = true
}

async function submitHandle() {
  saving.value = true
  try {
    await tenantHttp.put(`/api/tenant/care/abnormals/${handleForm.id}/handle`, handleForm)
    ElMessage.success('处置已保存')
    handleDialog.value = false
    await Promise.all([loadAbnormals(), loadBoard()])
  } finally {
    saving.value = false
  }
}

async function loadRefs() {
  const [rms, orders] = await Promise.all([
    tenantHttp.get('/api/tenant/rooms'),
    tenantHttp.get('/api/tenant/orders', { params: { status: '在住', pageNum: 1, pageSize: 100 } })
  ])
  rooms.value = (rms as any).data?.records || (rms as any).data || []
  livingOrders.value = (orders as any).data?.records || []
}

function loadAll() {
  loadBoard()
  loadPlans()
  loadRecords()
  loadHandovers()
  loadAbnormals()
}

function onTab(name: string) {
  if (name === 'board') loadBoard()
  if (name === 'plan') loadPlans()
  if (name === 'record') loadRecords()
  if (name === 'handover') loadHandovers()
  if (name === 'abnormal') loadAbnormals()
}

onMounted(async () => {
  await loadRefs()
  await Promise.all([loadBoard(), loadPlans(), loadRecords(), loadHandovers(), loadAbnormals()])
  if (route.query.tab) tab.value = String(route.query.tab)
})
</script>

<style scoped>
.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }
.tab-badge { display: inline-block; margin-left: 6px; padding: 0 5px; height: 16px; line-height: 16px; border-radius: 8px; background: #fbeeed; color: #a83c37; font-size: 10.5px; }

.board-head { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); }
.date-nav { display: flex; align-items: center; gap: 2px; }
.board-kpi { display: flex; gap: 16px; font-size: 12.5px; color: var(--ink-500); }
.board-kpi b { color: var(--ink-900); font-size: 15px; font-variant-numeric: tabular-nums; }
.board-kpi .warn b { color: #a83c37; }

.care-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: var(--space-3); }
.care-card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-lg); padding: 14px; cursor: pointer; box-shadow: var(--shadow-xs); transition: box-shadow 0.15s ease, transform 0.15s ease; }
.care-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.care-head { display: flex; align-items: center; gap: 10px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-weight: 600; }
.care-head strong { font-size: 14px; }
.care-head p { margin: 1px 0 0; font-size: 11.5px; }
.flag { margin-left: auto; font-size: 10.5px; color: #a83c37; background: #fbeeed; border-radius: 4px; padding: 1px 6px; }
.ring-row { margin-top: 12px; display: flex; align-items: center; gap: 14px; }
.ring { width: 62px; height: 62px; border-radius: 50%; display: grid; place-items: center; position: relative; flex: none; }
.ring::after { content: ''; position: absolute; inset: 7px; border-radius: 50%; background: var(--surface); }
.ring span { position: relative; z-index: 1; font-size: 13px; font-weight: 650; font-variant-numeric: tabular-nums; }
.ring-meta { flex: 1; }
.ring-meta p { margin: 0 0 6px; font-size: 12px; color: var(--ink-500); }
.ring-meta b { color: var(--ink-900); font-size: 14px; }

.filter-bar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }

.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; white-space: nowrap; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.danger { background: #fbeeed; color: #a83c37; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.badge.info { background: #eef2f7; color: #4a5f7a; }

.detail { display: flex; flex-direction: column; gap: var(--space-4); }
.plan-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.plan-summary div { background: var(--surface-sunken); border-radius: var(--radius-md); padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; }
.plan-summary span { font-size: 11.5px; color: var(--ink-400); }
.plan-summary strong { font-size: 16px; font-variant-numeric: tabular-nums; }
.add-item { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 10px 12px; background: var(--surface-sunken); border-radius: var(--radius-md); }
.day-group { border: 1px solid var(--line); border-radius: var(--radius-md); overflow: hidden; }
.day-head { margin: 0; padding: 9px 12px; background: var(--surface-sunken); font-size: 12.5px; font-weight: 600; display: flex; justify-content: space-between; }
.day-head span { font-weight: 400; color: var(--ink-400); }
.item-list { list-style: none; margin: 0; padding: 0; }
.item-list li { display: flex; align-items: center; gap: 10px; padding: 9px 12px; border-top: 1px solid var(--line); font-size: 12.5px; }
.item-list li.done { background: #fcfdfb; }
.item-list li.skip { background: #fdf9f8; }
.item-list .time { flex: 0 0 46px; color: var(--ink-400); }
.item-list .target { flex: 0 0 54px; color: var(--ink-500); font-size: 11.5px; }
.item-list .name { flex: 1; }
.item-list .ops { display: flex; gap: 4px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
.empty { padding: 30px 0; text-align: center; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
