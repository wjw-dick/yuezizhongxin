<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>线索与跟进</h1>
        <p class="sub">来源归因 → 跟进 → 参观到店 → 一键转订单；跟进超时的线索会在工作台提醒</p>
      </div>
      <div class="actions">
        <el-button @click="openLeadForm()">
          <Icon name="plus" :size="15" /><span style="margin-left: 6px">新增线索</span>
        </el-button>
        <el-button type="primary" @click="openVisitForm()">
          <Icon name="calendar" :size="15" /><span style="margin-left: 6px">预约参观</span>
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

    <el-tabs v-model="tab" class="biz-tabs">
      <el-tab-pane label="线索看板" name="board">
        <div class="toolbar board-toolbar">
          <el-input v-model="keyword" placeholder="客户姓名 / 手机号" clearable size="small" style="width: 200px" @keyup.enter="loadLeads" @clear="loadLeads" />
          <el-select v-model="sourceCode" placeholder="来源" clearable size="small" style="width: 150px" @change="loadLeads">
            <el-option v-for="s in sources" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <el-button size="small" @click="loadLeads">查询</el-button>
          <span class="spacer" />
          <el-radio-group v-model="view" size="small">
            <el-radio-button label="board">看板</el-radio-button>
            <el-radio-button label="table">表格</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="view === 'board'" v-loading="loading" class="kanban">
          <div v-for="col in stages" :key="col.value" class="kanban-col">
            <div class="kanban-head">
              <span class="dot" :style="{ background: col.color }" />
              <strong>{{ col.label }}</strong>
              <em>{{ grouped[col.value]?.length || 0 }}</em>
            </div>
            <div class="kanban-body">
              <article
                v-for="lead in grouped[col.value] || []"
                :key="lead.id"
                class="lead-card"
                :class="{ overdue: isOverdue(lead) }"
                @click="openLeadDetail(lead)"
              >
                <div class="lead-top">
                  <strong>{{ lead.customerName }}</strong>
                  <span class="phone mono">{{ maskPhone(lead.phone) }}</span>
                </div>
                <p class="lead-meta">
                  <span>{{ lead.intendRoomType || '未填意向房型' }}</span>
                  <span v-if="lead.dueDate">预产 {{ lead.dueDate }}</span>
                </p>
                <p class="lead-follow" :class="{ warn: isOverdue(lead) }">
                  <Icon name="clock" :size="12" />
                  {{ followText(lead) }}
                </p>
                <div class="lead-foot">
                  <span class="tag">{{ sourceLabel(lead.sourceCode) }}</span>
                  <el-button link type="primary" @click.stop="toOrder(lead)">转订单</el-button>
                </div>
              </article>
              <p v-if="!(grouped[col.value] || []).length" class="col-empty">暂无线索</p>
            </div>
          </div>
        </div>

        <div v-else class="card flush">
          <el-table v-loading="loading" :data="leads" size="small" stripe>
            <el-table-column prop="customerName" label="客户" width="110" />
            <el-table-column label="手机号" width="130">
              <template #default="{ row }"><span class="mono">{{ maskPhone(row.phone) }}</span></template>
            </el-table-column>
            <el-table-column prop="dueDate" label="预产期" width="110" />
            <el-table-column prop="intendRoomType" label="意向房型" min-width="120" />
            <el-table-column label="预算" width="140">
              <template #default="{ row }">
                <span class="money">{{ money(row.budgetMin) }} ~ {{ money(row.budgetMax) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="来源" width="110">
              <template #default="{ row }">{{ sourceLabel(row.sourceCode) }}</template>
            </el-table-column>
            <el-table-column label="阶段" width="100">
              <template #default="{ row }"><span class="badge" :class="stageClass(row.stage)">{{ row.stage }}</span></template>
            </el-table-column>
            <el-table-column label="下次跟进" min-width="150">
              <template #default="{ row }">
                <span :class="{ 'text-warn': isOverdue(row) }">{{ row.nextFollowTime || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openLeadDetail(row)">跟进</el-button>
                <el-button link type="primary" @click="toOrder(row)">转订单</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>暂无线索</strong>官网、小程序、H5 的预约表单会自动进入线索池</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="参观预约" name="visit">
        <div class="card flush">
          <div class="toolbar visit-toolbar">
            <el-date-picker v-model="visitDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" size="small" style="width: 160px" @change="loadVisits" />
            <el-button size="small" text @click="visitDate = today(); loadVisits()">今天</el-button>
            <span class="spacer" />
            <span class="muted">共 {{ visits.length }} 条</span>
          </div>
          <el-table v-loading="loading" :data="visits" size="small" stripe>
            <el-table-column prop="visitDate" label="参观日期" width="120" />
            <el-table-column prop="timeSlot" label="时段" width="120" />
            <el-table-column label="客户" min-width="140">
              <template #default="{ row }">{{ leadName(row.leadId) }}</template>
            </el-table-column>
            <el-table-column prop="personCount" label="人数" width="80" />
            <el-table-column label="到店" width="100">
              <template #default="{ row }">
                <span class="badge" :class="row.checkedIn ? 'ok' : 'muted'">{{ row.checkedIn ? '已签到' : '未到店' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="feedback" label="接待反馈" min-width="180" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.checkedIn" link type="primary" @click="checkin(row)">签到</el-button>
                <span v-else class="muted">已完成</span>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>当日无参观预约</strong>可在右上角为客户创建参观预约</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane name="overdue">
        <template #label>
          跟进超时
          <span v-if="overdueList.length" class="tab-badge">{{ overdueList.length }}</span>
        </template>
        <div class="card flush">
          <el-table v-loading="loading" :data="overdueList" size="small" stripe>
            <el-table-column prop="customerName" label="客户" width="120" />
            <el-table-column label="手机号" width="130">
              <template #default="{ row }"><span class="mono">{{ maskPhone(row.phone) }}</span></template>
            </el-table-column>
            <el-table-column prop="stage" label="阶段" width="100" />
            <el-table-column prop="nextFollowTime" label="应跟进时间" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="200" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openLeadDetail(row)">去跟进</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <div class="empty"><strong>没有超时线索</strong>跟进节奏保持良好</div>
            </template>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 线索详情与跟进 -->
    <el-drawer v-model="detailDrawer" :title="current.customerName || '线索详情'" size="620px">
      <div v-if="current.id" class="detail">
        <div class="detail-grid">
          <div class="detail-item"><span>手机号</span><strong class="mono">{{ maskPhone(current.phone) }}</strong></div>
          <div class="detail-item"><span>预产期</span><strong>{{ current.dueDate || '-' }}</strong></div>
          <div class="detail-item"><span>意向房型</span><strong>{{ current.intendRoomType || '-' }}</strong></div>
          <div class="detail-item"><span>预算区间</span><strong>{{ money(current.budgetMin) }} ~ {{ money(current.budgetMax) }}</strong></div>
          <div class="detail-item"><span>来源</span><strong>{{ sourceLabel(current.sourceCode) }}</strong></div>
          <div class="detail-item"><span>阶段</span><strong>{{ current.stage }}</strong></div>
        </div>

        <div class="mini-form">
          <p class="section-label">新增跟进</p>
          <el-input v-model="followForm.content" type="textarea" :rows="3" placeholder="沟通内容，如：客户关注产后康复项目，希望参观套房" />
          <div class="mini-form-row">
            <el-input v-model="followForm.result" placeholder="跟进结果，如：意向较强" />
            <el-date-picker v-model="followForm.nextTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="下次跟进时间" style="width: 210px" />
          </div>
          <el-select v-model="followForm.stage" placeholder="推进阶段" clearable style="width: 100%">
            <el-option v-for="s in stages" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <div class="mini-form-actions">
            <el-button size="small" @click="markLost">标记流失</el-button>
            <span class="spacer" />
            <el-button size="small" type="primary" :loading="saving" @click="addFollow">保存跟进</el-button>
          </div>
        </div>

        <p class="section-label">跟进记录</p>
        <ul class="timeline">
          <li v-for="f in follows" :key="f.id">
            <span class="time mono">{{ f.followTime }}</span>
            <div class="bubble">
              <p>{{ f.content }}</p>
              <small>
                结果：{{ f.result || '-' }}
                <template v-if="f.nextTime">· 下次 {{ f.nextTime }}</template>
              </small>
            </div>
          </li>
          <li v-if="!follows.length" class="timeline-empty">还没有跟进记录</li>
        </ul>
      </div>
    </el-drawer>

    <!-- 新增线索 -->
    <el-dialog v-model="leadDialog" title="新增线索" width="560px">
      <el-form :model="leadForm" label-position="top" class="two-col">
        <el-form-item label="客户姓名" required><el-input v-model="leadForm.customerName" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="leadForm.phone" placeholder="11 位手机号，落库自动加密" /></el-form-item>
        <el-form-item label="预产期"><el-date-picker v-model="leadForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="意向房型">
          <el-select v-model="leadForm.intendRoomType" clearable style="width: 100%">
            <el-option v-for="t in roomTypes" :key="t.id" :label="t.name" :value="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算下限"><el-input-number v-model="leadForm.budgetMin" :min="0" :step="1000" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="预算上限"><el-input-number v-model="leadForm.budgetMax" :min="0" :step="1000" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="来源">
          <el-select v-model="leadForm.sourceCode" style="width: 100%">
            <el-option v-for="s in sources" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="销售顾问账号 ID"><el-input v-model="leadForm.advisorId" placeholder="选填" /></el-form-item>
        <el-form-item label="下次跟进时间"><el-date-picker v-model="leadForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="leadForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leadDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveLead">保存</el-button>
      </template>
    </el-dialog>

    <!-- 参观预约 -->
    <el-dialog v-model="visitDialog" title="预约参观" width="520px">
      <el-form :model="visitForm" label-position="top">
        <el-form-item label="客户线索" required>
          <el-select v-model="visitForm.leadId" filterable style="width: 100%">
            <el-option v-for="l in leads" :key="l.id" :label="`${l.customerName}（${maskPhone(l.phone)}）`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参观日期" required>
          <el-date-picker v-model="visitForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="时段" required>
          <el-select v-model="visitForm.timeSlot" style="width: 100%">
            <el-option v-for="slot in slots" :key="slot" :label="slot" :value="slot" />
          </el-select>
        </el-form-item>
        <el-form-item label="来访人数">
          <el-input-number v-model="visitForm.personCount" :min="1" :max="10" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visitDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVisit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 转订单 -->
    <el-dialog v-model="orderDialog" title="线索转订单" width="620px">
      <div class="convert-head">
        <span class="avatar">{{ (current.customerName || '?').slice(0, 1) }}</span>
        <div>
          <strong>{{ current.customerName }}</strong>
          <p class="muted">{{ maskPhone(current.phone) }} · 预产期 {{ current.dueDate || '未填' }}</p>
        </div>
      </div>
      <el-form :model="orderForm" label-position="top" class="two-col">
        <el-form-item label="套餐" required>
          <el-select v-model="orderForm.packageId" style="width: 100%" @change="onPackageChange">
            <el-option v-for="p in packages" :key="p.id" :label="`${p.name}（${p.days} 天 · ¥${money(p.price)}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间" required>
          <el-select v-model="orderForm.roomId" style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${r.roomNo}（${r.floor || '-'}）`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入住日期" required>
          <el-date-picker v-model="orderForm.checkinDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="syncCheckout" />
        </el-form-item>
        <el-form-item label="离馆日期" required>
          <el-date-picker v-model="orderForm.checkoutDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="护理模式">
          <el-select v-model="orderForm.careMode" style="width: 100%">
            <el-option label="一对一" value="一对一" />
            <el-option label="一对二" value="一对二" />
            <el-option label="集中护理" value="集中护理" />
          </el-select>
        </el-form-item>
        <el-form-item label="陪护人数"><el-input-number v-model="orderForm.companionCount" :min="0" :max="5" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="优惠金额"><el-input-number v-model="orderForm.discountAmount" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="定金（留空按套餐比例）"><el-input-number v-model="orderForm.bondAmount" :min="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="备注" class="full"><el-input v-model="orderForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitOrder">创建订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Icon from '../../components/Icon.vue'
import { tenantHttp } from '../../api/client'

const router = useRouter()
const tab = ref('board')
const view = ref<'board' | 'table'>('board')
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const sourceCode = ref('')
const leads = ref<any[]>([])
const overdueList = ref<any[]>([])
const visits = ref<any[]>([])
const roomTypes = ref<any[]>([])
const packages = ref<any[]>([])
const rooms = ref<any[]>([])
const visitDate = ref(today())

function today() {
  return new Date().toISOString().slice(0, 10)
}

const stages = [
  { label: '新线索', value: '新线索', color: '#7d93a8' },
  { label: '已联系', value: '已联系', color: '#4f8ec9' },
  { label: '已到店', value: '已到店', color: '#c99a4f' },
  { label: '已签约', value: '已签约', color: '#3aa173' },
  { label: '已流失', value: '已流失', color: '#b06b62' }
]

const sources = [
  { label: '官网', value: '官网' },
  { label: '小程序', value: '小程序' },
  { label: '公众号', value: '公众号' },
  { label: 'H5 分享', value: 'H5' },
  { label: '电话咨询', value: '电话' },
  { label: '到店自然客流', value: '到店' },
  { label: '转介绍', value: '转介绍' }
]

const slots = ['09:00-10:30', '10:30-12:00', '14:00-15:30', '15:30-17:00', '17:00-18:30']

const grouped = computed(() => {
  const map: Record<string, any[]> = {}
  stages.forEach((s) => (map[s.value] = []))
  leads.value.forEach((lead) => {
    const key = map[lead.stage] ? lead.stage : '新线索'
    map[key].push(lead)
  })
  return map
})

const stats = computed(() => {
  const total = leads.value.length
  const signed = leads.value.filter((l) => l.stage === '已签约').length
  const visited = visits.value.filter((v) => v.checkedIn).length
  return [
    { label: '线索总数', value: total, hint: '当前筛选条件' },
    { label: '跟进超时', value: overdueList.value.length, hint: '需今日处理' },
    { label: '参观到店', value: `${visited}/${visits.value.length}`, hint: visitDate.value },
    { label: '签约转化', value: total ? `${Math.round((signed / total) * 100)}%` : '0%', hint: `已签约 ${signed}` }
  ]
})

function maskPhone(phone: string) {
  if (!phone) return '-'
  return phone.length >= 7 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone
}

function money(v: any) {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function sourceLabel(code: string) {
  return sources.find((s) => s.value === code)?.label || code || '未知来源'
}

function stageClass(stage: string) {
  return { 已签约: 'ok', 已流失: 'muted', 已到店: 'warn', 已联系: 'info' }[stage] || 'info'
}

function isOverdue(lead: any) {
  if (!lead.nextFollowTime) return false
  if (lead.stage === '已签约' || lead.stage === '已流失') return false
  return lead.nextFollowTime < new Date().toISOString().slice(0, 19).replace('T', ' ')
}

function followText(lead: any) {
  if (lead.stage === '已签约') return '已签约，等待入住'
  if (!lead.nextFollowTime) return '尚未安排下次跟进'
  return isOverdue(lead) ? `已超时：${lead.nextFollowTime}` : `下次跟进 ${lead.nextFollowTime}`
}

async function loadLeads() {
  loading.value = true
  try {
    const res: any = await tenantHttp.get('/api/tenant/crm/leads', {
      params: { keyword: keyword.value || undefined, sourceCode: sourceCode.value || undefined }
    })
    leads.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadOverdue() {
  const res: any = await tenantHttp.get('/api/tenant/crm/leads/overdue')
  overdueList.value = res.data || []
}

async function loadVisits() {
  const res: any = await tenantHttp.get('/api/tenant/crm/visits', { params: { date: visitDate.value } })
  visits.value = res.data || []
}

async function loadRefs() {
  const [types, pkgs, rms] = await Promise.all([
    tenantHttp.get('/api/tenant/rooms/types'),
    tenantHttp.get('/api/tenant/catalog/packages'),
    tenantHttp.get('/api/tenant/rooms')
  ])
  roomTypes.value = (types as any).data || []
  packages.value = (pkgs as any).data?.records || (pkgs as any).data || []
  rooms.value = (rms as any).data?.records || (rms as any).data || []
}

function leadName(id: number) {
  return leads.value.find((l) => l.id === id)?.customerName || `线索 #${id}`
}

/* ---------------- 线索表单 ---------------- */
const leadDialog = ref(false)
const leadForm = reactive<any>({})

function openLeadForm() {
  Object.keys(leadForm).forEach((k) => delete leadForm[k])
  Object.assign(leadForm, { sourceCode: '电话', stage: '新线索' })
  leadDialog.value = true
}

async function saveLead() {
  if (!leadForm.customerName || !leadForm.phone) {
    ElMessage.warning('请填写客户姓名与手机号')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/crm/leads', leadForm)
    ElMessage.success('线索已创建')
    leadDialog.value = false
    await loadLeads()
  } finally {
    saving.value = false
  }
}

/* ---------------- 线索详情 ---------------- */
const detailDrawer = ref(false)
const current = ref<any>({})
const follows = ref<any[]>([])
const followForm = reactive<any>({ content: '', result: '', nextTime: null, stage: null })

async function openLeadDetail(lead: any) {
  current.value = { ...lead }
  detailDrawer.value = true
  followForm.content = ''
  followForm.result = ''
  followForm.nextTime = null
  followForm.stage = null
  const res: any = await tenantHttp.get(`/api/tenant/crm/leads/${lead.id}/follows`)
  follows.value = res.data || []
}

async function addFollow() {
  if (!followForm.content.trim()) {
    ElMessage.warning('请填写跟进内容')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post(`/api/tenant/crm/leads/${current.value.id}/follows`, {
      content: followForm.content,
      result: followForm.result,
      nextTime: followForm.nextTime
    })
    if (followForm.stage) {
      await tenantHttp.put(`/api/tenant/crm/leads/${current.value.id}`, {
        ...current.value,
        stage: followForm.stage
      })
    }
    ElMessage.success('跟进已保存')
    followForm.content = ''
    followForm.result = ''
    await Promise.all([openLeadDetail(current.value), loadLeads(), loadOverdue()])
  } finally {
    saving.value = false
  }
}

async function markLost() {
  const { value } = await ElMessageBox.prompt('请输入流失原因（必填），用于后续复盘', '标记流失', {
    inputPlaceholder: '如：已选择其他月子中心 / 预算超支',
    inputValidator: (v: string) => (v && v.trim() ? true : '流失原因不能为空')
  })
  await tenantHttp.put(`/api/tenant/crm/leads/${current.value.id}`, {
    ...current.value,
    stage: '已流失',
    lostReason: value
  })
  ElMessage.success('已标记流失')
  detailDrawer.value = false
  await Promise.all([loadLeads(), loadOverdue()])
}

/* ---------------- 参观预约 ---------------- */
const visitDialog = ref(false)
const visitForm = reactive<any>({ personCount: 2 })

function openVisitForm() {
  Object.keys(visitForm).forEach((k) => delete visitForm[k])
  Object.assign(visitForm, { personCount: 2, visitDate: today(), timeSlot: slots[0] })
  visitDialog.value = true
}

async function saveVisit() {
  if (!visitForm.leadId || !visitForm.visitDate) {
    ElMessage.warning('请选择线索与日期')
    return
  }
  saving.value = true
  try {
    await tenantHttp.post('/api/tenant/crm/visits', visitForm)
    ElMessage.success('参观预约已创建')
    visitDialog.value = false
    visitDate.value = visitForm.visitDate
    await loadVisits()
  } finally {
    saving.value = false
  }
}

async function checkin(row: any) {
  await tenantHttp.post(`/api/tenant/crm/visits/${row.id}/checkin`)
  ElMessage.success('已签到，线索阶段同步为已到店')
  await Promise.all([loadVisits(), loadLeads()])
}

/* ---------------- 转订单 ---------------- */
const orderDialog = ref(false)
const orderForm = reactive<any>({})

function toOrder(lead: any) {
  current.value = { ...lead }
  Object.keys(orderForm).forEach((k) => delete orderForm[k])
  Object.assign(orderForm, {
    customerName: lead.customerName,
    phone: lead.phone,
    careMode: '一对一',
    companionCount: 1,
    discountAmount: 0,
    source: lead.sourceCode || '门店'
  })
  orderDialog.value = true
}

function onPackageChange(id: number) {
  const pkg = packages.value.find((p) => p.id === id)
  if (pkg?.roomTypeId) {
    orderForm.roomTypeId = pkg.roomTypeId
  }
  syncCheckout()
}

function syncCheckout() {
  const pkg = packages.value.find((p) => p.id === orderForm.packageId)
  if (!pkg || !orderForm.checkinDate) return
  const start = new Date(orderForm.checkinDate)
  start.setDate(start.getDate() + Number(pkg.days || 28))
  orderForm.checkoutDate = start.toISOString().slice(0, 10)
}

async function submitOrder() {
  if (!orderForm.packageId || !orderForm.roomId || !orderForm.checkinDate || !orderForm.checkoutDate) {
    ElMessage.warning('请完善套餐、房间与入住日期')
    return
  }
  saving.value = true
  try {
    const res: any = await tenantHttp.post(`/api/tenant/crm/leads/${current.value.id}/convert`, {
      ...orderForm,
      leadId: current.value.id
    })
    ElMessage.success(`订单已创建：${res.data?.orderNo || ''}`)
    orderDialog.value = false
    detailDrawer.value = false
    await Promise.all([loadLeads(), loadOverdue()])
    router.push('/orders')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadLeads(), loadOverdue(), loadVisits(), loadRefs()])
})
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: var(--space-3);
}
.stat {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  box-shadow: var(--shadow-xs);
}
.stat-label { font-size: 11.5px; color: var(--ink-400); letter-spacing: 0.04em; }
.stat-value { font-size: 22px; font-weight: 650; color: var(--ink-900); font-variant-numeric: tabular-nums; }
.stat-hint { font-style: normal; font-size: 11.5px; color: var(--ink-400); }

.biz-tabs :deep(.el-tabs__header) { margin-bottom: 0; padding: 0 4px; border-bottom: 1px solid var(--line); }
.biz-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.biz-tabs :deep(.el-tabs__item) { height: 42px; font-size: 13.5px; color: var(--ink-500); }
.biz-tabs :deep(.el-tabs__item.is-active) { color: var(--brand-700); font-weight: 600; }
.biz-tabs :deep(.el-tabs__active-bar) { background: var(--brand-600); }

.tab-badge {
  display: inline-block; margin-left: 6px; padding: 0 5px; height: 16px; line-height: 16px;
  border-radius: 8px; background: #fbeeed; color: #a83c37; font-size: 10.5px;
}

.board-toolbar, .visit-toolbar { padding: 14px var(--space-5); border-bottom: 1px solid var(--line); background: var(--surface-sunken); }

.kanban { display: grid; grid-template-columns: repeat(5, minmax(220px, 1fr)); gap: var(--space-3); overflow-x: auto; padding-bottom: 6px; }
.kanban-col { background: var(--surface-sunken); border: 1px solid var(--line); border-radius: var(--radius-lg); display: flex; flex-direction: column; min-height: 320px; }
.kanban-head { display: flex; align-items: center; gap: 7px; padding: 11px 12px; border-bottom: 1px solid var(--line); }
.kanban-head .dot { width: 7px; height: 7px; border-radius: 50%; }
.kanban-head strong { font-size: 13px; color: var(--ink-900); }
.kanban-head em { margin-left: auto; font-style: normal; font-size: 11.5px; color: var(--ink-400); }
.kanban-body { padding: 10px; display: flex; flex-direction: column; gap: 8px; }

.lead-card {
  background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius-md);
  padding: 10px 11px; cursor: pointer; transition: box-shadow 0.15s ease, transform 0.15s ease;
}
.lead-card:hover { box-shadow: var(--shadow-sm); transform: translateY(-1px); }
.lead-card.overdue { border-left: 3px solid #d08a52; }
.lead-top { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.lead-top strong { font-size: 13.5px; color: var(--ink-900); }
.phone { font-size: 11.5px; color: var(--ink-400); }
.lead-meta { margin: 5px 0 0; display: flex; gap: 10px; font-size: 11.5px; color: var(--ink-500); }
.lead-follow { margin: 6px 0 0; display: flex; align-items: center; gap: 4px; font-size: 11.5px; color: var(--ink-400); }
.lead-follow.warn { color: #a8672c; }
.lead-foot { margin-top: 8px; padding-top: 8px; border-top: 1px dashed var(--line); display: flex; align-items: center; justify-content: space-between; }
.tag { font-size: 11px; color: var(--ink-500); background: var(--surface-sunken); border-radius: 4px; padding: 1px 6px; }
.col-empty { margin: 12px 0; text-align: center; font-size: 12px; color: var(--ink-400); }

.badge { display: inline-flex; align-items: center; height: 22px; padding: 0 8px; border-radius: 999px; font-size: 12px; background: #eef2f7; color: #4a5f7a; }
.badge.ok { background: #eef7f1; color: #2f7d55; }
.badge.warn { background: #fdf5e8; color: #9a6a18; }
.badge.muted { background: #f4f3ef; color: var(--ink-500); }
.money { font-variant-numeric: tabular-nums; }
.text-warn { color: #a8672c; }

.detail { display: flex; flex-direction: column; gap: var(--space-4); }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 16px; background: var(--surface-sunken); border-radius: var(--radius-md); padding: 14px; }
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-item span { font-size: 11.5px; color: var(--ink-400); }
.detail-item strong { font-size: 13px; color: var(--ink-900); font-weight: 550; }
.mini-form { display: flex; flex-direction: column; gap: 8px; }
.mini-form-row { display: flex; gap: 8px; }
.mini-form-row :deep(.el-input) { flex: 1; }
.mini-form-actions { display: flex; align-items: center; }
.mini-form-actions .spacer { flex: 1; }
.section-label { font-size: 11.5px; letter-spacing: 0.08em; color: var(--ink-400); font-weight: 600; margin: 0; }
.timeline { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.timeline li { display: flex; gap: 10px; }
.timeline .time { flex: 0 0 122px; font-size: 11.5px; color: var(--ink-400); padding-top: 2px; }
.bubble { flex: 1; background: var(--surface-sunken); border: 1px solid var(--line); border-radius: var(--radius-md); padding: 9px 11px; }
.bubble p { margin: 0; font-size: 13px; color: var(--ink-800); }
.bubble small { display: block; margin-top: 4px; font-size: 11.5px; color: var(--ink-400); }
.timeline-empty { color: var(--ink-400); font-size: 12px; }

.convert-head { display: flex; align-items: center; gap: 10px; margin-bottom: var(--space-3); }
.convert-head .avatar { width: 38px; height: 38px; border-radius: 50%; background: var(--brand-50); color: var(--brand-700); display: grid; place-items: center; font-weight: 600; }
.convert-head p { margin: 2px 0 0; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.two-col :deep(.full) { grid-column: 1 / -1; }
.empty { padding: 30px 0; color: var(--ink-400); font-size: 12px; }
.empty strong { display: block; color: var(--ink-700); font-size: 13px; margin-bottom: 4px; }
</style>
